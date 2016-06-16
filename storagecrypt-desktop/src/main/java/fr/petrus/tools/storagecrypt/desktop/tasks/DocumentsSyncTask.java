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

package fr.petrus.tools.storagecrypt.desktop.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsSyncProcess;
import fr.petrus.lib.core.processes.Process;
import fr.petrus.lib.core.result.ProgressAdapter;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

/**
 * The {@code Task} which synchronizes the local documents on remote storages.
 *
 * @see DocumentsSyncProcess
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DocumentsSyncTask extends ProcessTask {
    private static Logger LOG = LoggerFactory.getLogger(DocumentsSyncTask.class);

    /**
     * This class is used to report this {@code Task} state.
     */
    public static class SyncServiceState {
        /**
         * The progress among all the documents.
         */
        public Progress documentsListProgress = new Progress(0, 0);

        /**
         * The progress in the current document.
         */
        public Progress currentDocumentProgress = new Progress(0, 0);

        /**
         * The name of the current being processed.
         */
        public String currentDocumentName = null;

        /**
         * The current action being processed on the current document.
         */
        public SyncAction currentSyncAction = null;
    }

    private CloudAppKeys cloudAppKeys = null;
    private volatile int startRequests = 0;
    private SyncServiceState syncState = new SyncServiceState();

    /**
     * Creates a new {@code DocumentsSyncTask} instance.
     *
     * @param appWindow the application window
     */
    public DocumentsSyncTask(AppWindow appWindow) {
        super(appWindow);
        cloudAppKeys = appContext.getCloudAppKeys();
    }

    /**
     * Starts the process in the background if it is not currently running.
     */
    public void start() {
        if (cloudAppKeys.found()) {
            startRequests++;
            if (hasProcess()) {
                final DocumentsSyncProcess documentsSyncProcess = (DocumentsSyncProcess) getProcess();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            documentsSyncProcess.updateSyncQueue();
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                        }
                    }
                }.start();
            } else {
                final DocumentsSyncProcess documentsSyncProcess = new DocumentsSyncProcess(
                        appContext.getTextI18n(),
                        appContext.getNetwork(),
                        appContext.getAccounts(),
                        appContext.getEncryptedDocuments());
                setProcess(documentsSyncProcess);
                documentsSyncProcess.setProgressListener(new ProgressAdapter() {
                    @Override
                    public void onProgress(int i, int progress) {
                        switch (i) {
                            case 0:
                                syncState.documentsListProgress.setProgress(progress);
                                break;
                            case 1:
                                syncState.currentDocumentProgress.setProgress(progress);
                                break;
                        }
                        appWindow.updateDocumentsSyncProgress(syncState);
                    }

                    @Override
                    public void onSetMax(int i, int max) {
                        switch (i) {
                            case 0:
                                syncState.documentsListProgress.setMax(max);
                                break;
                            case 1:
                                syncState.currentDocumentProgress.setMax(max);
                                break;
                        }
                        appWindow.updateDocumentsSyncProgress(syncState);
                    }
                });
                documentsSyncProcess.setSyncActionListener(new DocumentsSyncProcess.SyncActionListener() {
                    @Override
                    public void onSyncActionStart(SyncAction syncAction,
                                                  EncryptedDocument encryptedDocument) {
                        syncState.currentSyncAction = syncAction;
                        if (null != syncAction && null != encryptedDocument) {
                            try {
                                syncState.currentDocumentName = encryptedDocument.logicalPath();
                            } catch (DatabaseConnectionClosedException e) {
                                syncState.currentDocumentName = encryptedDocument.getDisplayName();
                            }
                        }
                        appWindow.updateDocumentsSyncProgress(syncState);
                    }

                    @Override
                    public void onDocumentChanged(EncryptedDocument encryptedDocument) {
                        appWindow.update(true);
                    }
                });
                new Thread() {
                    @Override
                    public void run() {
                        LOG.debug("starting sync process()");
                        while (startRequests > 0) {
                            startRequests--;
                            if (startRequests < 0) {
                                startRequests = 0;
                            }
                            try {
                                documentsSyncProcess.run();
                            } catch (DatabaseConnectionClosedException e) {
                                LOG.error("Database is closed", e);
                            }
                        }
                        appWindow.resetDocumentsSyncProgress();
                        appWindow.update(true);
                        setProcess(null);
                        LOG.debug("exiting sync process()");
                    }
                }.start();
            }
        }
    }

    /**
     * Starts the process in the background if it is not currently running, or adds the given
     * {@code encryptedDocument} to the running process queue.
     *
     * @param encryptedDocument the document to be synchronized
     */
    public void syncDocument(EncryptedDocument encryptedDocument) {
        if (cloudAppKeys.found()) {
            Process process = getProcess();
            if (null == process) {
                start();
            } else {
                try {
                    ((DocumentsSyncProcess) process).enqueueDocument(encryptedDocument);
                } catch (DatabaseConnectionClosedException e) {
                    LOG.error("Database is closed", e);
                }
            }
        }
    }

    /**
     * Starts the process in the background if it is not currently running, or adds the given
     * {@code encryptedDocuments} to the running process queue.
     *
     * @param encryptedDocuments the documents to be synchronized
     */
    public void syncDocuments(Collection<EncryptedDocument> encryptedDocuments) {
        if (cloudAppKeys.found()) {
            Process process = getProcess();
            if (null == process) {
                start();
            } else {
                try {
                    ((DocumentsSyncProcess) process).enqueueDocuments(encryptedDocuments);
                } catch (DatabaseConnectionClosedException e) {
                    LOG.error("Database is closed", e);
                }
            }
        }
    }

    /**
     * Restarts the synchronization of the currently processed document if it is the given
     * {@code encryptedDocument}.
     *
     * @param encryptedDocument the encrypted document to restart synchronizing
     */
    public void restartCurrentSync(EncryptedDocument encryptedDocument) {
        if (cloudAppKeys.found()) {
            Process process = getProcess();
            if (null != process) {
                ((DocumentsSyncProcess) process).restartIfCurrent(encryptedDocument.getId());
            }
        }
    }
}
