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

import android.os.Bundle;
import android.util.Log;

import java.util.List;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.processes.DocumentsUpdatesPushProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.lib.core.Progress;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsUpdatesPushDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ProgressDialogFragment;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsUpdatesPushTask;

/**
 * The {@code ThreadService} which sends local documents modifications to the remote account.
 *
 * @see DocumentsUpdatesPushTask
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DocumentsUpdatesPushService extends ThreadService<DocumentsUpdatesPushService> {
    private static final String TAG = "DocumentsUpdatesPushSvc";

    /**
     * The argument used to pass the ID of the "top level" folder which documents to synchronize.
     */
    public static final String ROOT_ID = "rootId";

    /**
     * The argument used to pass the IDs of the "top level" folders which documents to synchronize.
     */
    public static final String ROOT_IDS = "rootIds";

    private TextI18n textI18n = null;
    private Network network = null;
    private EncryptedDocuments encryptedDocuments = null;
    private DocumentsUpdatesPushProcess documentsUpdatesPushProcess = null;

    /**
     * Creates a new {@code DocumentsUpdatesPushService} instance.
     */
    public DocumentsUpdatesPushService() {
        super(TAG, AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG);
    }

    @Override
    public void initDependencies() {
        super.initDependencies();
        textI18n = appContext.getTextI18n();
        network = appContext.getNetwork();
        encryptedDocuments = appContext.getEncryptedDocuments();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final TaskProgressEvent progressEvent = new TaskProgressEvent(
                AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG, 2);
        documentsUpdatesPushProcess = new DocumentsUpdatesPushProcess(textI18n, network);
        documentsUpdatesPushProcess.setProgressListener(new ProgressListener() {
            @Override
            public void onMessage(int i, String message) {
                progressEvent.setMessage(i, message).postSticky();
            }

            @Override
            public void onProgress(int i, int progress) {
                progressEvent.setProgress(i, progress).postSticky();
            }

            @Override
            public void onSetMax(int i, int max) {
                progressEvent.setMax(i, max).postSticky();
            }
        });
    }

    @Override
    protected void runIntent(int command, Bundle parameters) {
        setProcess(documentsUpdatesPushProcess);
        if (null!=parameters) {
            try {
                long updatesPushRoot = parameters.getLong(ROOT_ID, -1);
                long[] updatesPushRoots = parameters.getLongArray(ROOT_IDS);
                if (updatesPushRoot > 0) {
                    pushUpdates(updatesPushRoot);
                } else if (null != updatesPushRoots) {
                    pushUpdates(updatesPushRoots);
                }
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
            }
        }
        setProcess(null);
    }

    private void pushUpdates(long updatesPushRootId) throws DatabaseConnectionClosedException {
        EncryptedDocument updatesPushRoot =
                encryptedDocuments.encryptedDocumentWithId(updatesPushRootId);

        if (null!= updatesPushRoot && !updatesPushRoot.isUnsynchronized()) {
            new ShowDialogEvent(new ProgressDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG)
                    .setTitle(getString(R.string.progress_text_pushing_updates))
                    .setCancelButton(true).setPauseButton(true)
                    .setProgresses(new Progress(false), new Progress(false))).postSticky();
            documentsUpdatesPushProcess.pushUpdates(updatesPushRoot);
            documentsUpdatesPushProcess.run();
            DocumentListChangeEvent.postSticky();
        }
        new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG)
                .postSticky();
        new DocumentsUpdatesPushDoneEvent(documentsUpdatesPushProcess.getResults()).postSticky();
    }

    private void pushUpdates(long[] updatesPushRootIds) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> updatesPushRoots =
                encryptedDocuments.encryptedDocumentsWithIds(updatesPushRootIds);

        if (null!= updatesPushRoots && !updatesPushRoots.isEmpty()) {
            new ShowDialogEvent(new ProgressDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG)
                    .setTitle(getString(R.string.progress_text_pushing_updates))
                    .setCancelButton(true).setPauseButton(true)
                    .setProgresses(new Progress(false), new Progress(false))).postSticky();
            documentsUpdatesPushProcess.pushUpdates(updatesPushRoots);
            documentsUpdatesPushProcess.run();
            DocumentListChangeEvent.postSticky();
        }
        new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG)
                .postSticky();
        new DocumentsUpdatesPushDoneEvent(documentsUpdatesPushProcess.getResults()).postSticky();
    }
}
