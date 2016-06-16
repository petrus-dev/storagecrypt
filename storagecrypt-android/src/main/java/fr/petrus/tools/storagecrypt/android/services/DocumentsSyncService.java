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

import java.util.ArrayList;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsSyncProcess;
import fr.petrus.lib.core.result.ProgressAdapter;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsSyncServiceEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;

/**
 * The {@code ThreadService} which synchronizes the local documents on remote storages.
 *
 * @see DocumentsSyncTask
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DocumentsSyncService extends ThreadService<DocumentsSyncService> {
    private static final String TAG = "DocumentsSyncSvc";

    /**
     * The argument used to pass the ID of an encrypted document.
     */
    public static final String DOCUMENT_ID = "documentId";

    /**
     * The argument used to pass the list of IDs of encrypted documents.
     */
    public static final String DOCUMENT_IDS = "documentIds";

    /**
     * The {@code COMMAND} value to add an {@code EncryptedDocument} to the synchronization queue.
     */
    public static final int COMMAND_ENQUEUE_DOCUMENT     = 10;

    /**
     * The {@code COMMAND} value to add a list of {@code EncryptedDocument}s to the synchronization
     * queue.
     */
    public static final int COMMAND_ENQUEUE_DOCUMENTS    = 11;

    /**
     * The {@code COMMAND} value to restart the synchronization of an {@code EncryptedDocument} if
     * it is currently processed.
     */
    public static final int COMMAND_RESTART_CURRENT_SYNC = 12;

    private TextI18n textI18n = null;
    private Network network = null;
    private Accounts accounts = null;
    private EncryptedDocuments encryptedDocuments = null;
    private DocumentsSyncProcess documentsSyncProcess = null;
    private Thread updateSyncQueueThread = null;
    private boolean updateRequested = false;

    /**
     * Creates a new {@code DocumentsSyncService} instance.
     */
    public DocumentsSyncService() {
        super(TAG, AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_SYNC_PROGRESS_DIALOG);
    }

    @Override
    public void initDependencies() {
        super.initDependencies();
        textI18n = appContext.getTextI18n();
        network = appContext.getNetwork();
        accounts = appContext.getAccounts();
        encryptedDocuments = appContext.getEncryptedDocuments();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final DocumentsSyncServiceEvent documentsSyncServiceEvent = new DocumentsSyncServiceEvent();
        final TaskProgressEvent taskProgressEvent = new TaskProgressEvent(
                AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_SYNC_PROGRESS_DIALOG, 2);
        taskProgressEvent.getProgress(0).setIndeterminate();
        documentsSyncProcess = new DocumentsSyncProcess(textI18n, network, accounts, encryptedDocuments);
        documentsSyncProcess.setProgressListener(new ProgressAdapter() {
            @Override
            public void onProgress(int i, int progress) {
                switch (i) {
                    case 0:
                        documentsSyncServiceEvent.getDocumentsListProgress().setProgress(progress);
                        documentsSyncServiceEvent.postSticky();
                        break;
                    case 1:
                        documentsSyncServiceEvent.getCurrentDocumentProgress().setProgress(progress);
                        documentsSyncServiceEvent.postSticky();
                        break;
                }
                taskProgressEvent.setProgress(i, progress).postSticky();
            }

            @Override
            public void onSetMax(int i, int max) {
                switch (i) {
                    case 0:
                        documentsSyncServiceEvent.getDocumentsListProgress().setMax(max);
                        documentsSyncServiceEvent.postSticky();
                        break;
                    case 1:
                        documentsSyncServiceEvent.getCurrentDocumentProgress().setMax(max);
                        documentsSyncServiceEvent.postSticky();
                        break;
                }
                taskProgressEvent.setMax(i, max).postSticky();
            }
        });
        documentsSyncProcess.setSyncActionListener(new DocumentsSyncProcess.SyncActionListener() {
            @Override
            public void onSyncActionStart(SyncAction syncAction, EncryptedDocument encryptedDocument) {
                documentsSyncServiceEvent.setSyncAction(syncAction);
                if (null!=syncAction && null!= encryptedDocument) {
                    documentsSyncServiceEvent.setCurrentDocumentName(encryptedDocument.getDisplayName());
                    String documentName;
                    try {
                        documentName = encryptedDocument.logicalPath();
                    } catch (DatabaseConnectionClosedException e) {
                        documentName = encryptedDocument.getDisplayName();
                    }
                    switch (syncAction) {
                        case Upload:
                            taskProgressEvent.setMessage(getString(R.string.progress_message_uploading_remote_document,
                                    documentName));
                            break;
                        case Download:
                            taskProgressEvent.setMessage(getString(R.string.progress_message_downloading_remote_document,
                                    documentName));
                            break;
                        case Deletion:
                            taskProgressEvent.setMessage(getString(R.string.progress_message_deleting_remote_document,
                                    documentName));
                            break;
                    }
                    taskProgressEvent.postSticky();
                }
                documentsSyncServiceEvent.postSticky();
            }

            @Override
            public void onDocumentChanged(EncryptedDocument encryptedDocument) {
                DocumentListChangeEvent.postSticky();
            }
        });
    }

    @Override
    protected void runIntent(int command, Bundle parameters) {
        setProcess(documentsSyncProcess);
        try {
            documentsSyncProcess.run();
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }
        new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_SYNC_PROGRESS_DIALOG).postSticky();
        new DocumentsSyncServiceEvent().postSticky();
        DocumentListChangeEvent.postSticky();
        setProcess(null);
    }

    @Override
    protected void refreshIntent(int command, Bundle parameters) {
        if (isRunning()) {
            switch (command) {
                case COMMAND_START:
                    updateRequested = true;
                    if (null==updateSyncQueueThread) {
                        updateSyncQueueThread = new Thread() {
                            @Override
                            public void run() {
                                while (updateRequested) {
                                    updateRequested = false;
                                    try {
                                        documentsSyncProcess.updateSyncQueue();
                                    } catch (DatabaseConnectionClosedException e) {
                                        Log.e(TAG, "Database is closed", e);
                                    }
                                }
                                updateSyncQueueThread = null;
                            }
                        };
                        updateSyncQueueThread.start();
                    }
                    break;
                case COMMAND_ENQUEUE_DOCUMENT:
                    if (null!=parameters) {
                        try {
                            EncryptedDocument encryptedDocument =
                                    encryptedDocuments.encryptedDocumentWithId(parameters.getLong(DOCUMENT_ID, -1));
                            if (null != encryptedDocument) {
                                documentsSyncProcess.enqueueDocument(encryptedDocument);
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            Log.e(TAG, "Database is closed", e);
                        }
                    }
                    break;
                case COMMAND_ENQUEUE_DOCUMENTS:
                    if (null!=parameters) {
                        try {
                            long[] documentIds = parameters.getLongArray(DOCUMENT_IDS);
                            if (null != documentIds && documentIds.length > 0) {
                                ArrayList<EncryptedDocument> documentsList = new ArrayList<>();
                                for (long documentId : documentIds) {
                                    EncryptedDocument encryptedDocument =
                                            encryptedDocuments.encryptedDocumentWithId(documentId);
                                    if (null != encryptedDocument) {
                                        encryptedDocuments.add(encryptedDocument);
                                    }
                                }
                                documentsSyncProcess.enqueueDocuments(documentsList);
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            Log.e(TAG, "Database is closed", e);
                        }
                    }
                    break;
                case COMMAND_RESTART_CURRENT_SYNC:
                    if (null!=parameters) {
                        documentsSyncProcess.restartIfCurrent(parameters.getLong(DOCUMENT_ID, -1));
                    }
                    break;
            }
        }
    }
}
