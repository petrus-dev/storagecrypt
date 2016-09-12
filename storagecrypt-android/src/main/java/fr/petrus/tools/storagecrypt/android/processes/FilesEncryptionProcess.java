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

package fr.petrus.tools.storagecrypt.android.processes;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataStream;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.AbstractProcess;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.ColumnType;
import fr.petrus.lib.core.processes.results.FailedResult;
import fr.petrus.lib.core.processes.results.SourceDestinationResult;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.android.utils.UriHelper;

/**
 * The {@code Process} which handles files encryption
 *
 * @author Pierre Sagne
 * @since 24.08.2015
 */
public class FilesEncryptionProcess extends AbstractProcess<FilesEncryptionProcess.Results> {

    public static final String TAG = "FilesEncryptionProcess";

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     */
    public static class Results extends BaseProcessResults<SourceDestinationResult<Uri, EncryptedDocument>, Uri> {

        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super(textI18n, true, true, true);
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
        public ColumnType[] getResultsColumnsTypes(ResultsType resultsType) {
            if (null!=resultsType) {
                switch (resultsType) {
                    case Success:
                        return new ColumnType[] { ColumnType.Source, ColumnType.Destination };
                    case Skipped:
                        return new ColumnType[] { ColumnType.Source, ColumnType.Destination };
                    case Errors:
                        return new ColumnType[] { ColumnType.Document, ColumnType.Error };
                }
            }
            return super.getResultsColumnsTypes(resultsType);
        }

        @Override
        public String[] getResultColumns(ResultsType resultsType, int i) {
            String[] result;
            if (null==resultsType) {
                result = new String[0];
            } else {
                switch (resultsType) {
                    case Success:
                        result = new String[]{
                                success.get(i).getSource().toString(),
                                success.get(i).getDestination().failSafeLogicalPath()
                        };
                        break;
                    case Skipped:
                        result = new String[] {
                                skipped.get(i).getSource().toString(),
                                skipped.get(i).getDestination().failSafeLogicalPath()
                        };
                        break;
                    case Errors:
                        result = new String[]{
                                errors.get(i).getElement().toString(),
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
         * Returns the successfully encrypted files as a list of Uris.
         *
         * @return the list of Uris of the successfully encrypted files
         */
        public List<Uri> getSuccessfulyEncryptedUris() {
            List<Uri> results = new ArrayList<>();
            for (SourceDestinationResult<Uri, EncryptedDocument> result : success) {
                results.add(result.getSource());
            }
            return results;
        }

        /**
         * Returns the successfully encrypted files as a list.
         *
         * @return the list of successfully encrypted files
         */
        public List<EncryptedDocument> getSuccessfulyEncryptedDocuments() {
            List<EncryptedDocument> results = new ArrayList<>();
            for (SourceDestinationResult<Uri, EncryptedDocument> result : success) {
                results.add(result.getDestination());
            }
            return results;
        }
    }

    private Crypto crypto;
    private KeyManager keyManager;
    private EncryptedDocuments encryptedDocuments;
    private LinkedHashMap<Uri, SourceDestinationResult<Uri,EncryptedDocument>> successfulEncryptions = new LinkedHashMap<>();
    private LinkedHashMap<Uri, SourceDestinationResult<Uri,EncryptedDocument>> existingDocuments = new LinkedHashMap<>();
    private LinkedHashMap<Uri, FailedResult<Uri>> failedEncryptions = new LinkedHashMap<>();
    private ProgressListener progressListener;

    /**
     * Creates a new {@code FilesEncryptionProcess}, providing its dependencies.
     *
     * @param crypto             a {@code Crypto} instance
     * @param keyManager         a {@code KeyManager} instance
     * @param textI18n           a {@code TextI18n} instance
     * @param encryptedDocuments an {@code EncryptedDocuments} instance
     */
    public FilesEncryptionProcess(Crypto crypto, KeyManager keyManager, TextI18n textI18n,
                                  EncryptedDocuments encryptedDocuments) {
        super(new Results(textI18n));
        this.crypto = crypto;
        this.keyManager = keyManager;
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
     * Encrypts the documents targeted by the the given {@code srcFileUris} into the folder
     * represented by the {@code EncryptedDocument} with the given {@code dstFolderId},
     * using the {@code dstKeyAlias}.
     **
     * @param srcFileUris  the list of Uris of the documents to encrypt
     * @param dstFolderId  the id of the {@code EncryptedDocument} representing the folder where the
     *                     documents will be encrypted
     * @param dstKeyAlias  the key used to encrypt the documents
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void encryptFiles(Context context, List<Uri> srcFileUris, long dstFolderId, String dstKeyAlias)
            throws DatabaseConnectionClosedException {
        try {
            start();

            EncryptedDocument dstFolder = encryptedDocuments.encryptedDocumentWithId(dstFolderId);

            if (null != dstFolder && null != srcFileUris && srcFileUris.size() > 0) {
                if (null != progressListener) {
                    progressListener.onSetMax(0, srcFileUris.size());
                    progressListener.onProgress(0, 0);
                    progressListener.onSetMax(1, 1);
                    progressListener.onProgress(1, 0);
                }

                for (int i = 0; i < srcFileUris.size(); i++) {
                    pauseIfNeeded();
                    if (isCanceled()) {
                        return;
                    }

                    Uri srcFileUri = srcFileUris.get(i);
                    UriHelper srcFileUriHelper = new UriHelper(context, srcFileUri);
                    String displayName = srcFileUriHelper.getDisplayName();
                    String mimeType = srcFileUriHelper.getMimeType();
                    int size = srcFileUriHelper.getSize();

                    if (null != progressListener) {
                        progressListener.onMessage(0, displayName);
                        progressListener.onProgress(0, i);
                        progressListener.onSetMax(1, size);
                        progressListener.onProgress(1, 0);
                    }

                    EncryptedDocument encryptedDocument = dstFolder.child(displayName);
                    if (null != encryptedDocument) {
                        existingDocuments.put(srcFileUri, new SourceDestinationResult<>(srcFileUri, encryptedDocument));
                        continue;
                    }

                    EncryptedDocument dstEncryptedDocument;
                    try {
                        dstEncryptedDocument = dstFolder.createChild(displayName, mimeType, dstKeyAlias);
                    } catch (StorageCryptException e) {
                        Log.e(TAG, "Failed to create encrypted file " + displayName, e);
                        failedEncryptions.put(srcFileUri, new FailedResult<>(srcFileUri, e));
                        continue;
                    }

                    InputStream srcFileInputStream = null;
                    OutputStream dstFileOutputStream = null;
                    File dstFile;

                    try {
                        try {
                            srcFileInputStream = srcFileUriHelper.openInputStream(true);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to open source file " + displayName, e);
                            dstEncryptedDocument.delete();
                            failedEncryptions.put(srcFileUri, new FailedResult<>(srcFileUri,
                                    new StorageCryptException(
                                            "Error while opening source file",
                                            StorageCryptException.Reason.SourceFileOpenError, e)));
                            continue;
                        }

                        try {
                            dstFile = dstEncryptedDocument.file();
                            dstFileOutputStream = new FileOutputStream(dstFile);
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to open destination file " + dstEncryptedDocument.getFileName(), e);
                            dstEncryptedDocument.delete();
                            failedEncryptions.put(srcFileUri, new FailedResult<>(srcFileUri,
                                    new StorageCryptException(
                                            "Error while opening destination file",
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
                                    return FilesEncryptionProcess.this.isCanceled();
                                }

                                @Override
                                public void pauseIfNeeded() {
                                    FilesEncryptionProcess.this.pauseIfNeeded();
                                }
                            });
                            dstEncryptedDocument.updateFileSize();
                            dstEncryptedDocument.updateLocalModificationTime(System.currentTimeMillis());
                            if (!dstEncryptedDocument.isUnsynchronized()) {
                                dstEncryptedDocument.updateSyncState(SyncAction.Upload, State.Planned);
                            }
                        } catch (CryptoException e) {
                            dstEncryptedDocument.delete();
                            Log.e(TAG, "Failed to encrypt file " + displayName, e);
                            failedEncryptions.put(srcFileUri, new FailedResult<>(srcFileUri,
                                    new StorageCryptException(
                                            "Error while encrypting file",
                                            StorageCryptException.Reason.EncryptionError, e)));
                            continue;
                        }
                    } finally {
                        if (null != srcFileInputStream) {
                            try {
                                srcFileInputStream.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error when closing source input stream", e);
                            }
                        }
                        if (null != dstFileOutputStream) {
                            try {
                                dstFileOutputStream.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error when closing destination output stream", e);
                            }
                        }
                    }
                    successfulEncryptions.put(srcFileUri, new SourceDestinationResult<>(srcFileUri, dstEncryptedDocument));
                }
            }
        } finally {
            getResults().addResults(
                    successfulEncryptions.values(),
                    existingDocuments.values(),
                    failedEncryptions.values());
        }
    }
}
