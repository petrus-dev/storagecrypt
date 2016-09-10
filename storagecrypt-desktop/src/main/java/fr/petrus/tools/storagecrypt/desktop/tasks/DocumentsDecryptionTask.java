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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsDecryptionProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.desktop.ProgressWindowCreationException;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.DocumentsDecryptionProgressWindow;

/**
 * The {@code Task} which handles documents decryption.
 *
 * @see DocumentsDecryptionProcess
 *
 * @author Pierre Sagne
 * @since 20.08.2015
 */
public class DocumentsDecryptionTask extends ProcessTask {
    private static Logger LOG = LoggerFactory.getLogger(DocumentsDecryptionTask.class);

    /**
     * A decryption batch, holding a list of documents to decrypt, and the path of the folder to
     * store the decrypted files.
     */
    public static class DecryptionBatch {
        private List<EncryptedDocument> documents;
        private String folderPath;

        /**
         * Creates a new {@code DecryptionBatch} for the given {@code documents} and the given
         * destination {@code folderPath}.
         *
         * @param documents  the list of {@code EncryptedDocument}s to decrypt
         * @param folderPath the path of the folder where to store the decrypted documents
         */
        public DecryptionBatch(List<EncryptedDocument> documents, String folderPath) {
            this.documents = documents;
            this.folderPath = folderPath;
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
         * Gets the destination folder path.
         *
         * @return the destination folder path
         */
        public String getFolderPath() {
            return folderPath;
        }
    }

    private volatile int numBatchesToProcess = 0;
    private ConcurrentLinkedQueue<DecryptionBatch> decryptionBatches = new ConcurrentLinkedQueue<>();
    private DocumentsDecryptionProgressWindow.ProgressEvent taskProgressEvent =
            new DocumentsDecryptionProgressWindow.ProgressEvent();

    /**
     * Creates a new {@code DocumentsDecryptionTask} instance.
     *
     * @param appWindow the application window
     */
    public DocumentsDecryptionTask(AppWindow appWindow) {
        super(appWindow);
    }

    /**
     * Starts the decryption task in the background for the given {@code srcEncryptedDocument}.
     *
     * <p>If a decryption process is already running, adds a batch containing the
     * {@code srcEncryptedDocument} to the running process, to be processed when its work is done.
     *
     * <p>If the {@code srcEncryptedDocument} is a folder, all its contents will be decrypted too
     *
     * @param srcEncryptedDocument the document to decrypt
     * @param dstFolderPath        the path of the destination folder where to store the decrypted
     *                             document
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void decrypt(EncryptedDocument srcEncryptedDocument, String dstFolderPath)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> srcEncryptedDocuments = new ArrayList<>();
        srcEncryptedDocuments.add(srcEncryptedDocument);
        decrypt(srcEncryptedDocuments, dstFolderPath);
    }

    /**
     * Starts the decryption task in the background for the given {@code srcEncryptedDocuments}.
     *
     * <p>If a decryption process is already running, adds a batch containing the
     * {@code srcEncryptedDocuments} to the running process, to be processed when its work is done.
     *
     * <p>If some of the {@code srcEncryptedDocuments} are folders, all their contents will be
     * decrypted too
     *
     * @param srcEncryptedDocuments the documents to decrypt
     * @param dstFolderPath         the path of the destination folder where to store the decrypted
     *                              documents
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void decrypt(final List<EncryptedDocument> srcEncryptedDocuments,
                                     final String dstFolderPath)
            throws DatabaseConnectionClosedException {
        synchronized (this) {
            decryptionBatches.offer(new DecryptionBatch(
                    EncryptedDocument.unfoldAsList(srcEncryptedDocuments, true), dstFolderPath));
            numBatchesToProcess++;
        }
        try {
            final DocumentsDecryptionProgressWindow documentsDecryptionProgressWindow =
                    appWindow.getProgressWindow(DocumentsDecryptionProgressWindow.class);
            if (!documentsDecryptionProgressWindow.isClosed()) {
                documentsDecryptionProgressWindow.update(taskProgressEvent);
            }
            if (!hasProcess()) {
                final DocumentsDecryptionProcess documentsDecryptionProcess =
                        new DocumentsDecryptionProcess(
                                appContext.getCrypto(),
                                appContext.getKeyManager(),
                                appContext.getTextI18n());
                setProcess(documentsDecryptionProcess);
                documentsDecryptionProcess.setProgressListener(new ProgressListener() {
                    @Override
                    public void onMessage(int i, String message) {
                        taskProgressEvent.progresses[i+1].setMessage(message);
                        if (!documentsDecryptionProgressWindow.isClosed()) {
                            documentsDecryptionProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onProgress(int i, int progress) {
                        taskProgressEvent.progresses[i+1].setProgress(progress);
                        if (!documentsDecryptionProgressWindow.isClosed()) {
                            documentsDecryptionProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onSetMax(int i, int max) {
                        taskProgressEvent.progresses[i+1].setMax(max);
                        if (!documentsDecryptionProgressWindow.isClosed()) {
                            documentsDecryptionProgressWindow.update(taskProgressEvent);
                        }
                    }
                });

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            while (!decryptionBatches.isEmpty()) {
                                DecryptionBatch decryptionBatch = decryptionBatches.poll();
                                taskProgressEvent.progresses[0].setMessage(decryptionBatch.getFolderPath());
                                taskProgressEvent.progresses[0].setMax(numBatchesToProcess);
                                taskProgressEvent.progresses[0].setProgress(
                                        numBatchesToProcess - decryptionBatches.size() - 1);
                                taskProgressEvent.progresses[1].setMax(
                                        decryptionBatch.getDocuments().size());
                                if (!documentsDecryptionProgressWindow.isClosed()) {
                                    documentsDecryptionProgressWindow.update(taskProgressEvent);
                                }
                                if (null != decryptionBatch.getDocuments() &&
                                        !decryptionBatch.getDocuments().isEmpty()) {
                                    documentsDecryptionProcess.decryptDocuments(
                                            decryptionBatch.getDocuments(),
                                            decryptionBatch.getFolderPath());
                                }
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                        }
                        if (!documentsDecryptionProgressWindow.isClosed()) {
                            documentsDecryptionProgressWindow.close(true);
                        }
                        appWindow.onDocumentsDecryptionDone(documentsDecryptionProcess.getResults());
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
