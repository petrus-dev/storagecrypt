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

import java.util.List;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.FilesEncryptionDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ProgressDialogFragment;
import fr.petrus.tools.storagecrypt.android.processes.FilesEncryptionProcess;
import fr.petrus.tools.storagecrypt.android.tasks.FilesEncryptionTask;

/**
 * The {@code ThreadService} which handles files encryption.
 *
 * @see FilesEncryptionTask
 *
 * @author Pierre Sagne
 * @since 29.12.2015
 */
public class FilesEncryptionService extends ThreadService<FilesEncryptionService> {
    private static final String TAG = "FilesEncryptionSvc";

    /**
     * The argument used to pass the URIs of the files to encrypt.
     */
    public static final String SRC_FILE_URIS = "srcFileUris";

    /**
     * The argument used to pass the ID of the destination folder where to encrypt the files.
     */
    public static final String DST_FOLDER_ID = "dstFolderId";

    /**
     * The argument used to pass alias of the key to encrypt the files with.
     */
    public static final String DST_KEY_ALIAS = "dstKeyAlias";

    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private TextI18n textI18n = null;
    private EncryptedDocuments encryptedDocuments = null;
    private FilesEncryptionProcess documentsEncryptProcess = null;

    /**
     * Creates a new {@code FilesEncryptionService} instance.
     */
    public FilesEncryptionService() {
        super(TAG, AndroidConstants.MAIN_ACTIVITY.FILES_ENCRYPTION_PROGRESS_DIALOG);
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
    public void onCreate() {
        super.onCreate();
        final TaskProgressEvent taskProgressEvent = new TaskProgressEvent(
                AndroidConstants.MAIN_ACTIVITY.FILES_ENCRYPTION_PROGRESS_DIALOG, 2);
        documentsEncryptProcess =
                new FilesEncryptionProcess(crypto, keyManager, textI18n, encryptedDocuments);
        documentsEncryptProcess.setProgressListener(new ProgressListener() {
            @Override
            public void onMessage(int i, String message) {
                taskProgressEvent.setMessage(i, message).postSticky();
            }

            @Override
            public void onProgress(int i, int progress) {
                taskProgressEvent.setProgress(i, progress).postSticky();
            }

            @Override
            public void onSetMax(int i, int max) {
                taskProgressEvent.setMax(i, max).postSticky();
            }
        });
    }

    @Override
    protected void runIntent(int command, Bundle parameters) {
        setProcess(documentsEncryptProcess);
        if (null!=parameters) {
            List<Uri> srcFileUris = parameters.getParcelableArrayList(SRC_FILE_URIS);
            long dstFolderId = parameters.getLong(DST_FOLDER_ID, -1);
            String dstKeyAlias = parameters.getString(DST_KEY_ALIAS);

            if (null!=srcFileUris && srcFileUris.size()>0) {
                new ShowDialogEvent(new ProgressDialogFragment.Parameters()
                        .setDialogId(AndroidConstants.MAIN_ACTIVITY.FILES_ENCRYPTION_PROGRESS_DIALOG)
                        .setTitle(getString(R.string.progress_text_encrypting_documents))
                        .setCancelButton(true).setPauseButton(true)
                        .setProgresses(new Progress(false), new Progress(false))).postSticky();
                try {
                    documentsEncryptProcess.encryptFiles(getBaseContext(), srcFileUris, dstFolderId, dstKeyAlias);
                } catch (DatabaseConnectionClosedException e) {
                    Log.e(TAG, "Database is closed", e);
                }
                new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.FILES_ENCRYPTION_PROGRESS_DIALOG).postSticky();
            }
            new FilesEncryptionDoneEvent(documentsEncryptProcess.getResults()).postSticky();
        }
        setProcess(null);
    }
}
