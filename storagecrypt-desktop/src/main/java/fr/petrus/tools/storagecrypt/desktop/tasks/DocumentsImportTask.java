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

import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsImportProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.desktop.ProgressWindowCreationException;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.DocumentsImportProgressWindow;

/**
 * The {@code Task} which imports documents from the filesystem or from the remote account.
 *
 * @see DocumentsImportProcess
 *
 * @author Pierre Sagne
 * @since 20.08.2015
 */
public class DocumentsImportTask extends ProcessTask {

    private static Logger LOG = LoggerFactory.getLogger(DocumentsImportTask.class);

    /**
     * Creates a new {@code DocumentsImportTask} instance.
     *
     * @param appWindow the application window
     */
    public DocumentsImportTask(AppWindow appWindow) {
        super(appWindow);
    }

    /**
     * Enqueues the given {@code importRoots} in the list of folders to import then starts the
     * import task in the background if it is not currently running.
     *
     * @param importRoots the folders which will be looked up on the remote storage and which
     *                    children will be imported
     */
    public synchronized void importDocuments(List<EncryptedDocument> importRoots) {
        try {
            final DocumentsImportProgressWindow importProgressWindow =
                    appWindow.getProgressWindow(DocumentsImportProgressWindow.class);
            if (hasProcess()) {
                DocumentsImportProcess documentsImportProcess = (DocumentsImportProcess) getProcess();
                documentsImportProcess.importDocuments(importRoots);
            } else {
                final DocumentsImportProgressWindow.ProgressEvent taskProgressEvent
                        = new DocumentsImportProgressWindow.ProgressEvent();
                final DocumentsImportProcess documentsImportProcess = new DocumentsImportProcess(
                        appContext.getCrypto(),
                        appContext.getKeyManager(),
                        appContext.getTextI18n(),
                        appContext.getAccounts(),
                        appContext.getEncryptedDocuments());
                setProcess(documentsImportProcess);
                documentsImportProcess.setProgressListener(new ProgressListener() {
                    @Override
                    public void onMessage(int i, String message) {
                        switch (i) {
                            case 0:
                                taskProgressEvent.rootName = message;
                                break;
                            case 1:
                                taskProgressEvent.documentName = message;
                                break;
                        }
                        if (!importProgressWindow.isClosed()) {
                            importProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onProgress(int i, int progress) {
                        taskProgressEvent.progresses[i].setProgress(progress);
                        if (!importProgressWindow.isClosed()) {
                            importProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onSetMax(int i, int max) {
                        taskProgressEvent.progresses[i].setMax(max);
                        if (!importProgressWindow.isClosed()) {
                            importProgressWindow.update(taskProgressEvent);
                        }
                    }
                });

                new Thread() {
                    @Override
                    public void run() {
                        if (!importProgressWindow.isClosed()) {
                            importProgressWindow.update(taskProgressEvent);
                        }
                        documentsImportProcess.importDocuments(importRoots);
                        try {
                            documentsImportProcess.run();
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Failed to decrypt file", e);
                        }
                        if (!importProgressWindow.isClosed()) {
                            importProgressWindow.close(true);
                        }
                        appWindow.onDocumentsImportDone(documentsImportProcess.getResults());
                        setProcess(null);
                        LOG.debug("Exiting import thread");
                    }
                }.start();
            }
        } catch (ProgressWindowCreationException e) {
            LOG.error("Failed to get progress window {}",
                    e.getProgressWindowClass().getCanonicalName(), e);
        }
    }
}
