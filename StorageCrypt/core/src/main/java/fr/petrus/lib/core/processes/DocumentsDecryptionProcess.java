/*
 *  Copyright Pierre Sagne (12 december 2014)
 *
 * petrus.dev.fr@gmail.com
 *
 * This software is a computer program whose purpose is to encrypt and
 * synchronize files on the cloud.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *
 */

package fr.petrus.lib.core.processes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataStream;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.FailedResult;
import fr.petrus.lib.core.processes.results.SourceDestinationResult;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The {@code Process} which handles documents decryption.
 *
 * @author Pierre Sagne
 * @since 24.08.2015
 */
public class DocumentsDecryptionProcess extends AbstractProcess<DocumentsDecryptionProcess.Results> {

    private static Logger LOG = LoggerFactory.getLogger(DocumentsDecryptionProcess.class);

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     */
    public static class Results extends BaseProcessResults<SourceDestinationResult<EncryptedDocument, File>, EncryptedDocument> {

        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super (textI18n, true, true, true);
        }

        @Override
        public int getResultsColumnsCount(ResultsType resultsType) {
            if (null!=resultsType) {
                switch (resultsType) {
                    case Success:
                        return 2;
                    case Skipped:
                        return 2;
                    case Errors:
                        return 2;
                }
            }
            return 0;
        }

        @Override
        public String[] getResultColumns(ResultsType resultsType, int i) {
            String[] result;
            if (null==resultsType) {
                result = new String[0];
            } else {
                switch (resultsType) {
                    case Success:
                        try {
                            result = new String[]{
                                    success.get(i).getSource().logicalPath(),
                                    success.get(i).getDestination().getAbsolutePath()
                            };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[]{
                                    success.get(i).getSource().getDisplayName(),
                                    success.get(i).getDestination().getAbsolutePath()
                            };
                        }
                        break;
                    case Skipped:
                        try {
                            result = new String[]{
                                    skipped.get(i).getSource().logicalPath(),
                                    skipped.get(i).getDestination().getAbsolutePath()
                            };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[]{
                                    skipped.get(i).getSource().getDisplayName(),
                                    skipped.get(i).getDestination().getAbsolutePath()
                            };
                        }
                        break;
                    case Errors:
                        try {
                            result = new String[]{
                                    errors.get(i).getElement().logicalPath(),
                                    textI18n.getExceptionDescription(errors.get(i).getException())
                            };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[]{
                                    errors.get(i).getElement().getDisplayName(),
                                    textI18n.getExceptionDescription(errors.get(i).getException())
                            };
                        }
                        break;
                    default:
                        result = new String[0];
                }
            }
            return result;
        }
    }

    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private LinkedHashMap<Long, SourceDestinationResult<EncryptedDocument, File>> successfulDecryptions = new LinkedHashMap<>();
    private LinkedHashMap<Long, SourceDestinationResult<EncryptedDocument, File>> existingDocuments = new LinkedHashMap<>();
    private LinkedHashMap<Long, FailedResult<EncryptedDocument>> failedDecryptions = new LinkedHashMap<>();
    private ProgressListener progressListener;

    /**
     * Creates a new {@code DocumentsDecryptionProcess}, providing its dependencies.
     *
     * @param crypto             a {@code Crypto} instance
     * @param keyManager         a {@code KeyManager} instance
     * @param textI18n           a {@code TextI18n} instance
     */
    public DocumentsDecryptionProcess(Crypto crypto, KeyManager keyManager, TextI18n textI18n) {
        super(new Results(textI18n));
        progressListener = null;
        this.crypto = crypto;
        this.keyManager = keyManager;
    }

    /**
     * Sets the {@code ProgressListener} which this process will report its progress to.
     *
     * @param progressListener the {@code ProgressListener} which this process will report its progress to
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Decrypts the given {@code srcDocuments} and stores the decrypted files into the folder
     * with the given {@code dstFolderPath}.
     *
     * @param srcDocuments  the list of {@code EncryptedDocument}s to decrypt
     * @param dstFolderPath the path of the destination folder
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void decryptDocuments(List<EncryptedDocument> srcDocuments, String dstFolderPath)
            throws DatabaseConnectionClosedException {
        start();

        File dstFolder = new File(dstFolderPath);
        LOG.debug("Destination folder = {}", dstFolder.getAbsolutePath());

        if (null!=srcDocuments && srcDocuments.size()>0) {
            HashMap<Long, File> dstFolders = new HashMap<>();

            if (null!= progressListener) {
                progressListener.onSetMax(0, srcDocuments.size());
                progressListener.onProgress(0, 0);
                progressListener.onSetMax(1, 1);
                progressListener.onProgress(1, 0);
            }

            for (int i = 0; i<srcDocuments.size(); i++) {
                EncryptedDocument srcDocument = srcDocuments.get(i);

                pauseIfNeeded();
                if (isCanceled()) {
                    return;
                }

                if (null!= progressListener) {
                    progressListener.onProgress(0, i);
                    progressListener.onProgress(1, 0);
                }
                File dstFile;
                if (dstFolders.containsKey(srcDocument.getParentId())) {
                    dstFile = new File(dstFolders.get(srcDocument.getParentId()), srcDocument.getDisplayName());
                } else {
                    dstFile = new File(dstFolder, srcDocument.getDisplayName());
                }
                if (srcDocument.isFolder()) {
                    if (null != progressListener) {
                        progressListener.onMessage(0, srcDocument.getDisplayName());
                        progressListener.onSetMax(1, 1);
                    }
                    dstFolders.put(srcDocument.getId(), dstFile);
                    LOG.debug("Creating folder {}", dstFile.getAbsolutePath());
                    if (dstFile.exists()) {
                        LOG.debug("Folder {} exists", dstFile.getAbsolutePath());
                        existingDocuments.put(srcDocument.getId(),
                                new SourceDestinationResult<>(srcDocument, dstFile));
                    } else {
                        dstFile.mkdirs();
                        successfulDecryptions.put(srcDocument.getId(),
                                new SourceDestinationResult<>(srcDocument, dstFile));
                    }
                    if (null!=progressListener) {
                        progressListener.onProgress(1, 1);
                    }
                } else {
                    if (null != progressListener) {
                        progressListener.onMessage(0, srcDocument.logicalPath());
                        progressListener.onSetMax(1, (int)srcDocument.getSize());
                        progressListener.onProgress(1, 0);
                    }
                    LOG.debug("Decrypting file {}", dstFile.getAbsolutePath());
                    if (dstFile.exists()) {
                        LOG.debug("File {} exists", dstFile.getAbsolutePath());
                        existingDocuments.put(srcDocument.getId(),
                                new SourceDestinationResult<>(srcDocument, dstFile));
                    } else {
                        InputStream srcFileInputStream = null;
                        OutputStream dstFileOutputStream = null;
                        try {
                            try {
                                File srcFile = srcDocument.file();
                                srcFileInputStream = new FileInputStream(srcFile);
                            } catch (IOException e) {
                                LOG.error("Failed to open source file {}", srcDocument.getFileName(), e);
                                failedDecryptions.put(srcDocument.getId(), new FailedResult<>(srcDocument,
                                        new StorageCryptException("Failed to open source file",
                                                StorageCryptException.Reason.SourceFileOpenError, e)));
                                continue;
                            }

                            try {
                                dstFile.createNewFile();
                                dstFileOutputStream = new FileOutputStream(dstFile);
                            } catch (IOException e) {
                                LOG.error("Failed to open destination file {}", dstFile.getAbsolutePath(), e);
                                failedDecryptions.put(srcDocument.getId(), new FailedResult<>(srcDocument,
                                        new StorageCryptException("Failed to open destination file",
                                                StorageCryptException.Reason.DestinationFileOpenError, e)));
                                continue;
                            }

                            try {
                                EncryptedDataStream encryptedDataStream
                                        = new EncryptedDataStream(crypto, keyManager.getKeys(srcDocument.getKeyAlias()));
                                encryptedDataStream.decrypt(srcFileInputStream, dstFileOutputStream, new ProcessProgressAdapter() {
                                    @Override
                                    public void onProgress(int i, int progress) {
                                        if (null != progressListener) {
                                            if (0 == i) {
                                                progressListener.onProgress(1, progress);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onSetMax(int i, int max) {
                                        if (null != progressListener) {
                                            if (0 == i) {
                                                progressListener.onSetMax(1, max);
                                            }
                                        }
                                    }

                                    @Override
                                    public boolean isCanceled() {
                                        return DocumentsDecryptionProcess.this.isCanceled();
                                    }

                                    @Override
                                    public void pauseIfNeeded() {
                                        DocumentsDecryptionProcess.this.pauseIfNeeded();
                                    }
                                });
                            } catch (CryptoException e) {
                                dstFile.delete();
                                LOG.error("Failed to decrypt file {}", srcDocument.getDisplayName(), e);
                                failedDecryptions.put(srcDocument.getId(), new FailedResult<>(srcDocument,
                                        new StorageCryptException("Failed to decrypt file",
                                                StorageCryptException.Reason.DecryptionError, e)));
                                continue;
                            }
                            successfulDecryptions.put(srcDocument.getId(),
                                    new SourceDestinationResult<>(srcDocument, dstFile));
                        } finally {
                            if (null != srcFileInputStream) {
                                try {
                                    srcFileInputStream.close();
                                } catch (IOException e) {
                                    LOG.error("Error when closing source input stream", e);
                                }
                            }
                            if (null != dstFileOutputStream) {
                                try {
                                    dstFileOutputStream.close();
                                } catch (IOException e) {
                                    LOG.error("Error when closing destination output stream", e);
                                }
                            }
                        }
                    }
                }
            }
        }
        getResults().addResults(
                successfulDecryptions.values(),
                existingDocuments.values(),
                failedDecryptions.values());
    }
}
