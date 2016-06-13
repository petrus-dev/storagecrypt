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

package fr.petrus.tools.storagecrypt.android.services;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataStream;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.lib.core.Progress;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.AlertDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ProgressDialogFragment;
import fr.petrus.tools.storagecrypt.android.tasks.FileDecryptionTask;
import fr.petrus.tools.storagecrypt.android.utils.UriHelper;
import fr.petrus.lib.core.StorageCryptException;

/**
 * The {@code ThreadService} which handles a single file decryption.
 *
 * @see FileDecryptionTask
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class FileDecryptionService extends ThreadService<FileDecryptionService> {
    private static final String TAG = "FileDecryptionSvc";

    /**
     * The argument used to pass the ID of the document to decrypt.
     */
    public static final String SRC_DOCUMENT_ID = "srcDocumentId";

    /**
     * The argument used to pass the URI of the destination file where to save the decrypted file.
     */
    public static final String DST_FILE_URI = "dstFileUri";

    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private TextI18n textI18n = null;
    private EncryptedDocuments encryptedDocuments = null;

    /**
     * Creates a new {@code FileDecryptionService} instance.
     */
    public FileDecryptionService() {
        super(TAG, AndroidConstants.MAIN_ACTIVITY.FILE_DECRYPTION_PROGRESS_DIALOG);
    }

    @Override
    public void initDependencies() {
        super.initDependencies();
        crypto = appContext.getCrypto();
        keyManager = appContext.getKeyManager();
        textI18n = appContext.getTextI18n();
        encryptedDocuments = appContext.getEncryptedDocuments();
    }

    @Override
    protected void runIntent(int command, Bundle parameters) {
        if (null!=parameters) {
            long srcDocumentId = parameters.getLong(SRC_DOCUMENT_ID, -1);
            Uri dstFileUri = parameters.getParcelable(DST_FILE_URI);
            try {
                decryptFile(srcDocumentId, dstFileUri);
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
            } catch (StorageCryptException e) {
                Log.e(TAG, "Error while decrypting file", e);
                new ShowDialogEvent(new AlertDialogFragment.Parameters()
                        .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                        .setMessage(textI18n.getExceptionDescription(e))).postSticky();
            }
            new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.FILE_DECRYPTION_PROGRESS_DIALOG)
                    .postSticky();
        }
    }

    private void decryptFile(long srcDocumentId, Uri dstFileUri)
            throws DatabaseConnectionClosedException, StorageCryptException {
        EncryptedDocument srcEncryptedDocument = encryptedDocuments.encryptedDocumentWithId(srcDocumentId);

        if (null!= srcEncryptedDocument) {
            int size = (int) srcEncryptedDocument.getSize();
            Progress progress;
            if (size > 0) {
                progress = new Progress(0, size);
            } else {
                progress = new Progress(true);
            }

            new ShowDialogEvent(new ProgressDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.FILE_DECRYPTION_PROGRESS_DIALOG)
                    .setTitle(getString(R.string.progress_text_decrypting_documents))
                    .setMessage(srcEncryptedDocument.getDisplayName())
                    .setProgresses(progress)).postSticky();

            InputStream srcFileInputStream = null;
            OutputStream dstFileOutputStream = null;

            try {
                File srcFile;
                final TaskProgressEvent taskProgressEvent = new TaskProgressEvent(
                        AndroidConstants.MAIN_ACTIVITY.FILE_DECRYPTION_PROGRESS_DIALOG, 1);

                try {
                    srcFile = srcEncryptedDocument.file();
                    srcFileInputStream = new FileInputStream(srcFile);
                } catch (IOException e) {
                    throw new StorageCryptException("Error while opening source file : " + srcEncryptedDocument.getDisplayName(),
                            StorageCryptException.Reason.SourceFileOpenError, e);
                }

                UriHelper dstUriHelper = new UriHelper(getBaseContext(), dstFileUri);
                try {
                    dstFileOutputStream = dstUriHelper.openOutputStream(true);
                } catch (IOException e) {
                    throw new StorageCryptException("Error while opening destination file : " + dstUriHelper.getDisplayName(),
                            StorageCryptException.Reason.DestinationFileOpenError, e);
                }

                try {
                    EncryptedDataStream encryptedDataStream = new EncryptedDataStream(crypto,
                            keyManager.getKeys(srcEncryptedDocument.getKeyAlias()));

                    encryptedDataStream.decrypt(srcFileInputStream, dstFileOutputStream, new ProcessProgressAdapter() {
                        @Override
                        public void onProgress(int i, int progress) {
                            taskProgressEvent.setProgress(i, progress).postSticky();
                        }
                    });
                } catch (CryptoException e) {
                    dstUriHelper.delete();
                    throw new StorageCryptException("Error while decrypting",
                            StorageCryptException.Reason.DecryptionError, e);
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
        }
    }
}
