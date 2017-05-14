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
import java.util.concurrent.ConcurrentLinkedQueue;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsMoveProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.desktop.ProgressWindowCreationException;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.DocumentsMoveProgressWindow;

/**
 * The {@code Task} which handles documents move.
 *
 * @see DocumentsMoveProcess
 *
 * @author Pierre Sagne
 * @since 13.05.2017
 */
public class DocumentsMoveTask extends ProcessTask {
    private static Logger LOG = LoggerFactory.getLogger(DocumentsMoveTask.class);

    /**
     * A move batch, holding a list of documents to move, and the destination folder.
     */
    public static class MoveBatch {
        private List<EncryptedDocument> documents;
        private EncryptedDocument folder;

        /**
         * Creates a new {@code MoveBatch} for the given {@code documents} and the given
         * destination {@code folder}.
         *
         * @param documents  the list of {@code EncryptedDocument}s to move
         * @param folder     the destination folder
         */
        public MoveBatch(List<EncryptedDocument> documents, EncryptedDocument folder) {
            this.documents = documents;
            this.folder = folder;
        }

        /**
         * Gets the list of documents of this batch.
         *
         * @return the documents of this batch
         */
        public List<EncryptedDocument> getDocuments() {
            return documents;
        }

        /**
         * Gets the destination folder.
         *
         * @return the destination folder
         */
        public EncryptedDocument getFolder() {
            return folder;
        }
    }

    private volatile int numBatchesToProcess = 0;
    private ConcurrentLinkedQueue<MoveBatch> moveBatches = new ConcurrentLinkedQueue<>();
    private DocumentsMoveProgressWindow.ProgressEvent taskProgressEvent =
            new DocumentsMoveProgressWindow.ProgressEvent();

    /**
     * Creates a new {@code DocumentsMoveTask} instance.
     *
     * @param appWindow the application window
     */
    public DocumentsMoveTask(AppWindow appWindow) {
        super(appWindow);
    }

    /**
     * Starts the move task in the background for the given {@code srcDocuments}.
     *
     * <p>If a move process is already running, adds a batch containing the
     * {@code srcDocuments} to the running process, to be processed when its work is done.
     *
     * @param srcDocuments the documents to move
     * @param dstFolder    the destination folder
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void move(final List<EncryptedDocument> srcDocuments, final EncryptedDocument dstFolder)
            throws DatabaseConnectionClosedException {
        synchronized (this) {
            moveBatches.offer(new MoveBatch(srcDocuments, dstFolder));
            numBatchesToProcess++;
        }
        try {
            final DocumentsMoveProgressWindow documentsMoveProgressWindow =
                    appWindow.getProgressWindow(DocumentsMoveProgressWindow.class);
            if (!documentsMoveProgressWindow.isClosed()) {
                documentsMoveProgressWindow.update(taskProgressEvent);
            }
            if (!hasProcess()) {
                final DocumentsMoveProcess documentsMoveProcess =
                        new DocumentsMoveProcess(
                                appContext.getCrypto(),
                                appContext.getKeyManager(),
                                appContext.getTextI18n(),
                                appContext.getFileSystem());
                setProcess(documentsMoveProcess);
                documentsMoveProcess.setProgressListener(new ProgressListener() {
                    @Override
                    public void onMessage(int i, String message) {
                        taskProgressEvent.progresses[i+1].setMessage(message);
                        if (!documentsMoveProgressWindow.isClosed()) {
                            documentsMoveProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onProgress(int i, int progress) {
                        taskProgressEvent.progresses[i+1].setProgress(progress);
                        if (!documentsMoveProgressWindow.isClosed()) {
                            documentsMoveProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onSetMax(int i, int max) {
                        taskProgressEvent.progresses[i+1].setMax(max);
                        if (!documentsMoveProgressWindow.isClosed()) {
                            documentsMoveProgressWindow.update(taskProgressEvent);
                        }
                    }
                });

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            while (!moveBatches.isEmpty()) {
                                MoveBatch moveBatch = moveBatches.poll();
                                taskProgressEvent.progresses[0].setMessage(
                                        moveBatch.getFolder().failSafeLogicalPath());
                                taskProgressEvent.progresses[0].setMax(numBatchesToProcess);
                                taskProgressEvent.progresses[0].setProgress(
                                        numBatchesToProcess - moveBatches.size() - 1);
                                taskProgressEvent.progresses[1].setMax(
                                        moveBatch.getDocuments().size());
                                if (!documentsMoveProgressWindow.isClosed()) {
                                    documentsMoveProgressWindow.update(taskProgressEvent);
                                }
                                if (null != moveBatch.getDocuments() &&
                                        !moveBatch.getDocuments().isEmpty()) {
                                    documentsMoveProcess.moveDocuments(
                                            moveBatch.getDocuments(),
                                            moveBatch.getFolder());
                                }
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                        }
                        if (!documentsMoveProgressWindow.isClosed()) {
                            documentsMoveProgressWindow.close(true);
                        }
                        appWindow.onDocumentsMoveDone(documentsMoveProcess.getResults());
                        setProcess(null);
                        LOG.debug("Exiting decryption thread");
                    }
                }.start();
            }
        } catch (ProgressWindowCreationException e) {
            LOG.error("Failed to get progress window {}",
                    e.getProgressWindowClass().getCanonicalName(), e);
        }
    }
}
