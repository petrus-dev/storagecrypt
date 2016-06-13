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

import java.util.concurrent.ConcurrentLinkedQueue;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsUpdatesPushProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.desktop.ProgressWindowCreationException;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.DocumentsUpdatesPushProgressWindow;

/**
 * The {@code Task} which sends local documents modifications to the remote account.
 *
 * @see DocumentsUpdatesPushProcess
 *
 * @author Pierre Sagne
 * @since 20.08.2015
 */
public class DocumentsUpdatesPushTask extends ProcessTask {
    private static Logger LOG = LoggerFactory.getLogger(DocumentsUpdatesPushTask.class);

    private CloudAppKeys cloudAppKeys = null;
    private ConcurrentLinkedQueue<EncryptedDocument> updateRoots = new ConcurrentLinkedQueue<>();

    /**
     * Creates a new {@code DocumentsUpdatesPushTask} instance.
     *
     * @param appWindow the application window
     */
    public DocumentsUpdatesPushTask(AppWindow appWindow) {
        super(appWindow);
        cloudAppKeys = appContext.getCloudAppKeys();
    }

    /**
     * Enqueues the given {@code updateRoot} in the list of folders for which to push updates
     * then starts the updates push task in the background if it is not currently running.
     *
     * @param updateRoot the folder which will be scanned on the local filesystem and which children
     *                   updates will be pushed to the remote storage
     */
    public synchronized void pushUpdates(EncryptedDocument updateRoot) {
        if (cloudAppKeys.found()) {
            updateRoots.offer(updateRoot);
            if (null != updateRoot && !updateRoot.isUnsynchronized()) {
                try {
                    final DocumentsUpdatesPushProgressWindow updatesPushProgressWindow =
                            appWindow.getProgressWindow(DocumentsUpdatesPushProgressWindow.class);
                    if (!hasProcess()) {
                        final DocumentsUpdatesPushProgressWindow.ProgressEvent taskProgressEvent
                                = new DocumentsUpdatesPushProgressWindow.ProgressEvent();
                        final DocumentsUpdatesPushProcess documentsUpdatesPushProcess =
                                new DocumentsUpdatesPushProcess(appContext.getTextI18n());
                        setProcess(documentsUpdatesPushProcess);
                        documentsUpdatesPushProcess.setProgressListener(new ProgressListener() {
                            @Override
                            public void onMessage(int i, String message) {
                                taskProgressEvent.documentName = message;
                                if (!updatesPushProgressWindow.isClosed()) {
                                    updatesPushProgressWindow.update(taskProgressEvent);
                                }
                            }

                            @Override
                            public void onProgress(int i, int progress) {
                                taskProgressEvent.progresses[i].setProgress(progress);
                                if (!updatesPushProgressWindow.isClosed()) {
                                    updatesPushProgressWindow.update(taskProgressEvent);
                                }
                            }

                            @Override
                            public void onSetMax(int i, int max) {
                                taskProgressEvent.progresses[i].setMax(max);
                                if (!updatesPushProgressWindow.isClosed()) {
                                    updatesPushProgressWindow.update(taskProgressEvent);
                                }
                            }
                        });

                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    while (!updateRoots.isEmpty()) {
                                        EncryptedDocument updateRoot = updateRoots.poll();
                                        taskProgressEvent.rootName = updateRoot.storageText();
                                        if (!updatesPushProgressWindow.isClosed()) {
                                            updatesPushProgressWindow.update(taskProgressEvent);
                                        }
                                        documentsUpdatesPushProcess.pushUpdates(updateRoot);
                                    }
                                } catch (DatabaseConnectionClosedException e) {
                                    LOG.error("Database is closed", e);
                                }
                                if (!updatesPushProgressWindow.isClosed()) {
                                    updatesPushProgressWindow.close(true);
                                }
                                appWindow.onUpdatesPushDone(documentsUpdatesPushProcess.getResults());
                                setProcess(null);
                                LOG.debug("Exiting updates push thread");
                            }
                        }.start();
                    }
                } catch (ProgressWindowCreationException e) {
                    LOG.error("Failed to get progress window {}",
                            e.getProgressWindowClass().getCanonicalName(), e);
                }
            }
        }
    }
}
