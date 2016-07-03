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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.ParentNotFoundException;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataStream;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.FailedResult;
import fr.petrus.lib.core.processes.results.SourceDestinationResult;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The {@code Process} which handles documents encryption.
 *
 * @author Pierre Sagne
 * @since 24.08.2015
 */
public class DocumentsEncryptionProcess extends AbstractProcess<DocumentsEncryptionProcess.Results> {

    private static Logger LOG = LoggerFactory.getLogger(DocumentsEncryptionProcess.class);

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     */
    public static class Results extends BaseProcessResults<SourceDestinationResult<File, EncryptedDocument>, String> {

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
                                    success.get(i).getSource().getAbsolutePath(),
                                    success.get(i).getDestination().logicalPath()
                            };
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            result = new String[]{
                                    success.get(i).getSource().getAbsolutePath(),
                                    success.get(i).getDestination().getDisplayName()
                            };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[]{
                                    success.get(i).getSource().getAbsolutePath(),
                                    success.get(i).getDestination().getDisplayName()
                            };
                        }
                        break;
                    case Skipped:
                        try {
                            result = new String[]{
                                    skipped.get(i).getSource().getAbsolutePath(),
                                    skipped.get(i).getDestination().logicalPath()
                            };
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            result = new String[]{
                                    skipped.get(i).getSource().getAbsolutePath(),
                                    skipped.get(i).getDestination().getDisplayName()
                            };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[]{
                                    skipped.get(i).getSource().getAbsolutePath(),
                                    skipped.get(i).getDestination().getDisplayName()
                            };
                        }
                        break;
                    case Errors:
                        result = new String[] {
                                errors.get(i).getElement(),
                                textI18n.getExceptionDescription(errors.get(i).getException())
                        };
                        break;
                    default:
                        result = new String[0];
                }
            }
            return result;
        }

        /**
         * Returns the successfully encrypted documents as a list.
         *
         * @return the list of successfully encrypted documents
         */
        public List<EncryptedDocument> getSuccessfulyEncryptedDocuments() {
            List<EncryptedDocument> results = new ArrayList<>();
            for (SourceDestinationResult<File, EncryptedDocument> result : success) {
                results.add(result.getDestination());
            }
            return results;
        }
    }

    private Crypto crypto;
    private KeyManager keyManager;
    private FileSystem fileSystem;
    private EncryptedDocuments encryptedDocuments;
    private LinkedHashMap<String, SourceDestinationResult<File, EncryptedDocument>> successfulEncryptions = new LinkedHashMap<>();
    private LinkedHashMap<String, SourceDestinationResult<File, EncryptedDocument>> existingDocuments = new LinkedHashMap<>();
    private LinkedHashMap<String, FailedResult<String>> failedEncryptions = new LinkedHashMap<>();
    private ProgressListener progressListener;

    /**
     * Creates a new {@code DocumentsEncryptionProcess}, providing its dependencies.
     *
     * @param crypto             a {@code Crypto} instance
     * @param keyManager         a {@code KeyManager} instance
     * @param textI18n           a {@code TextI18n} instance
     * @param fileSystem         a {@code FileSystem} instance
     * @param encryptedDocuments an {@code EncryptedDocuments} instance
     */
    public DocumentsEncryptionProcess(Crypto crypto, KeyManager keyManager, TextI18n textI18n,
                                      FileSystem fileSystem, EncryptedDocuments encryptedDocuments) {
        super(new Results(textI18n));
        this.crypto = crypto;
        this.keyManager = keyManager;
        this.fileSystem = fileSystem;
        this.encryptedDocuments = encryptedDocuments;
        progressListener = null;
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
     * Encrypts the given {@code srcDocuments} into the folder represented by the {@code EncryptedDocument}
     * with the given {@code dstFolderId}, using the {@code dstKeyAlias}.
     *
     * <p>If some of the {@code srcDocuments} are folders, {@code EncryptedDocument}s are created
     * for them, and all the files they contain are also encrypted and stored into the matching
     * {@code EncryptedDocument}
     *
     * @param srcDocuments the paths of the documents to encrypt
     * @param dstFolderId  the id of the {@code EncryptedDocument} representing the folder where the
     *                     documents will be encrypted
     * @param dstKeyAlias  the key used to encrypt the documents
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void encryptDocuments(List<String> srcDocuments, long dstFolderId, String dstKeyAlias)
            throws DatabaseConnectionClosedException {
        start();
        EncryptedDocument dstFolder = encryptedDocuments.encryptedDocumentWithId(dstFolderId);
        if (null != dstFolder && null != srcDocuments && srcDocuments.size() > 0) {
            List<String> folders = fileSystem.getFoldersList(srcDocuments);
            Collections.sort(folders);
            HashMap<String, EncryptedDocument> createdFolders = new HashMap<>();
            HashSet<String> skippedFolders = new HashSet<>();
            List<String> files = fileSystem.getFilesList(srcDocuments);

            int currentDocumentIndex = 0;
            if (null != progressListener) {
                progressListener.onSetMax(0, srcDocuments.size());
                progressListener.onProgress(0, currentDocumentIndex);
                progressListener.onSetMax(1, 1);
                progressListener.onProgress(1, 0);
            }

            for (String srcPath : folders) {
                pauseIfNeeded();
                if (isCanceled()) {
                    return;
                }

                if (null != progressListener) {
                    progressListener.onMessage(0, srcPath);
                    progressListener.onProgress(0, currentDocumentIndex);
                    progressListener.onProgress(1, 0);
                }

                File srcFile = new File(srcPath);

                long parentId;
                if (createdFolders.containsKey(srcFile.getParent())) {
                    parentId = createdFolders.get(srcFile.getParent()).getId();
                } else if (skippedFolders.contains(srcFile.getParent())) {
                    skippedFolders.add(srcPath);
                    continue;
                } else {
                    parentId = dstFolderId;
                }

                EncryptedDocument parent = encryptedDocuments.encryptedDocumentWithId(parentId);
                if (null==parent) {
                    failedEncryptions.put(srcPath, new FailedResult<>(srcPath,
                            new StorageCryptException("Failed to find parent with id " + parentId,
                                    StorageCryptException.Reason.ParentNotFound)));
                    continue;
                } else {
                    EncryptedDocument encryptedDocument = parent.child(srcFile.getName());
                    if (null != encryptedDocument) {
                        skippedFolders.add(srcPath);
                        existingDocuments.put(srcPath, new SourceDestinationResult<>(srcFile, encryptedDocument));
                        continue;
                    }
                }

                try {
                    EncryptedDocument folder = parent.createChild(srcFile.getName(),
                            Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE, dstKeyAlias);
                    createdFolders.put(srcPath, folder);
                    successfulEncryptions.put(srcPath, new SourceDestinationResult<>(srcFile, folder));
                } catch (StorageCryptException e) {
                    skippedFolders.add(srcPath);
                    LOG.error("Failed to create encrypted folder {}", srcPath, e);
                    failedEncryptions.put(srcPath, new FailedResult<>(srcPath, e));
                    continue;
                }

                if (null != progressListener) {
                    progressListener.onProgress(1, 1);
                }
                currentDocumentIndex++;
            }

            for (String srcPath : files) {
                pauseIfNeeded();
                if (isCanceled()) {
                    return;
                }
                if (null != progressListener) {
                    progressListener.onMessage(0, srcPath);
                    progressListener.onProgress(0, currentDocumentIndex);
                }

                File srcFile = new File(srcPath);

                if (null != progressListener) {
                    progressListener.onSetMax(1, (int) srcFile.length());
                    progressListener.onProgress(1, 0);
                }

                long parentId;
                if (createdFolders.containsKey(srcFile.getParent())) {
                    parentId = createdFolders.get(srcFile.getParent()).getId();
                } else if (skippedFolders.contains(srcFile.getParent())) {
                    continue;
                } else {
                    parentId = dstFolderId;
                }

                EncryptedDocument parent = encryptedDocuments.encryptedDocumentWithId(parentId);
                if (null==parent) {
                    failedEncryptions.put(srcPath, new FailedResult<>(srcPath,
                            new StorageCryptException("Failed to find parent with id " + parentId,
                                    StorageCryptException.Reason.ParentNotFound)));
                    continue;
                } else {
                    EncryptedDocument encryptedDocument = parent.child(srcFile.getName());
                    if (null != encryptedDocument) {
                        existingDocuments.put(srcPath, new SourceDestinationResult<>(srcFile, encryptedDocument));
                        continue;
                    }
                }

                EncryptedDocument dstEncryptedDocument;
                try {
                    dstEncryptedDocument = parent.createChild(srcFile.getName(),
                            fileSystem.getMimeType(srcFile), dstKeyAlias);
                } catch (StorageCryptException e) {
                    LOG.error("Failed to create encrypted file {}", srcPath, e);
                    failedEncryptions.put(srcPath, new FailedResult<>(srcPath, e));
                    continue;
                }

                InputStream srcFileInputStream = null;
                OutputStream dstFileOutputStream = null;
                File dstFile;

                try {
                    try {
                        srcFileInputStream = new FileInputStream(srcFile);
                    } catch (IOException e) {
                        LOG.error("Failed to open source file {}", srcPath, e);
                        failedEncryptions.put(srcPath, new FailedResult<>(srcPath,
                                new StorageCryptException("Failed to open source file",
                                        StorageCryptException.Reason.SourceFileOpenError, e)));
                        continue;
                    }

                    try {
                        dstFile = dstEncryptedDocument.file();
                        dstFileOutputStream = new FileOutputStream(dstFile);
                    } catch (IOException e) {
                        LOG.error("Failed to open destination file {}", dstEncryptedDocument.getFileName(), e);
                        failedEncryptions.put(srcPath, new FailedResult<>(srcPath,
                                new StorageCryptException("Failed to open destination file",
                                        StorageCryptException.Reason.DestinationFileOpenError, e)));
                        continue;
                    }

                    try {
                        EncryptedDataStream encryptedDataStream =
                                new EncryptedDataStream(crypto, keyManager.getKeys(dstKeyAlias));
                        encryptedDataStream.encrypt(srcFileInputStream, dstFileOutputStream, new ProcessProgressAdapter() {
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
                                return DocumentsEncryptionProcess.this.isCanceled();
                            }

                            @Override
                            public void pauseIfNeeded() {
                                DocumentsEncryptionProcess.this.pauseIfNeeded();
                            }
                        });
                        dstEncryptedDocument.updateFileSize();
                        dstEncryptedDocument.updateLocalModificationTime(System.currentTimeMillis());
                        if (!dstEncryptedDocument.isUnsynchronized()) {
                            dstEncryptedDocument.updateSyncState(SyncAction.Upload, State.Planned);
                        }
                    } catch (CryptoException e) {
                        dstEncryptedDocument.delete();
                        LOG.error("Failed to encrypt file {}", srcPath, e);
                        failedEncryptions.put(srcPath, new FailedResult<>(srcPath,
                                new StorageCryptException("Failed to encrypt file",
                                        StorageCryptException.Reason.EncryptionError, e)));
                        continue;
                    }
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
                successfulEncryptions.put(srcPath, new SourceDestinationResult<>(srcFile, dstEncryptedDocument));
                currentDocumentIndex++;
            }
        }
        getResults().addResults(
                successfulEncryptions.values(),
                existingDocuments.values(),
                failedEncryptions.values());
    }
}
