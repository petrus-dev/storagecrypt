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
import fr.petrus.lib.core.ParentNotFoundException;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsEncryptionProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.desktop.ProgressWindowCreationException;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.DocumentsEncryptionProgressWindow;

/**
 * The {@code Task} which handles documents encryption.
 *
 * @see DocumentsEncryptionProcess
 *
 * @author Pierre Sagne
 * @since 20.08.2015
 */
public class DocumentsEncryptionTask extends ProcessTask {

    private static Logger LOG = LoggerFactory.getLogger(DocumentsEncryptionTask.class);

    /**
     * An encryption batch, holding a list of paths of documents to encrypt, the alias of the key
     * to encrypt the documents with and the {@code EncryptedDocument} folder to store the
     * encrypted files.
     */
    public static class EncryptionBatch {
        private EncryptedDocument parent;
        private String keyAlias;
        private List<String> documents;

        /**
         * Creates a new {@code EncryptionBatch} for the given {@code documents}, to be encrypted
         * with the given {@code keyAlias} and stored in the given {@code parent} destination folder.
         *
         * @param parent    the destination folder where to store the encrypted documents
         * @param keyAlias  the alias of the key to encrypt the documents with
         * @param documents the paths of the documents to encrypt
         */
        public EncryptionBatch(EncryptedDocument parent, String keyAlias, List<String> documents) {
            this.parent = parent;
            this.keyAlias = keyAlias;
            this.documents = documents;
        }

        /**
         * Returns the {@code EncryptedDocument} where the documents will be encrypted.
         *
         * @return the {@code Encrypted document} where the documents will be encrypted
         */
        public EncryptedDocument getParent() {
            return parent;
        }

        /**
         * Returns the alias of the key to encrypt the documents with.
         *
         * @return the alias of the key to encrypt the documents with
         */
        public String getKeyAlias() {
            return keyAlias;
        }

        /**
         * Returns the paths of the documents to encrypt.
         *
         * @return the paths of the documents to encrypt
         */
        public List<String> getDocuments() {
            return documents;
        }
    }

    private volatile int numBatchesToProcess = 0;
    private ConcurrentLinkedQueue<EncryptionBatch> encryptionBatches = new ConcurrentLinkedQueue<>();
    private DocumentsEncryptionProgressWindow.ProgressEvent taskProgressEvent =
            new DocumentsEncryptionProgressWindow.ProgressEvent();

    /**
     * Creates a new {@code DocumentsEncryptionTask} instance.
     *
     * @param appWindow the application window
     */
    public DocumentsEncryptionTask(AppWindow appWindow) {
        super(appWindow);
    }

    /**
     * Starts the encryption task in the background for the given {@code documents}.
     *
     * <p>If an encryption process is already running, adds a batch containing the
     * {@code documents} to the running process, to be processed when its work is done.
     *
     * <p>If some of the {@code documents} are folders, all their contents will be encrypted too
     *
     * @param parent    the destination folder where to store the encrypted documents
     * @param keyAlias  the alias of the key to encrypt the documents with
     * @param documents the paths of the documents to encrypt
     */
    public void encrypt(EncryptedDocument parent, String keyAlias, List<String> documents) {
        synchronized (this) {
            encryptionBatches.offer(new EncryptionBatch(parent, keyAlias, documents));
            numBatchesToProcess++;
        }
        try {
            final DocumentsEncryptionProgressWindow documentsEncryptionProgressWindow =
                    appWindow.getProgressWindow(DocumentsEncryptionProgressWindow.class);
            if (!documentsEncryptionProgressWindow.isClosed()) {
                documentsEncryptionProgressWindow.update(taskProgressEvent);
            }
            if (!hasProcess()) {
                final DocumentsEncryptionProcess documentsEncryptionProcess =
                        new DocumentsEncryptionProcess(
                                appContext.getCrypto(),
                                appContext.getKeyManager(),
                                appContext.getTextI18n(),
                                appContext.getFileSystem(),
                                appContext.getEncryptedDocuments());
                setProcess(documentsEncryptionProcess);
                documentsEncryptionProcess.setProgressListener(new ProgressListener() {
                    @Override
                    public void onMessage(int i, String message) {
                        taskProgressEvent.progresses[i+1].setMessage(message);
                        if (!documentsEncryptionProgressWindow.isClosed()) {
                            documentsEncryptionProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onProgress(int i, int progress) {
                        taskProgressEvent.progresses[i+1].setProgress(progress);
                        if (!documentsEncryptionProgressWindow.isClosed()) {
                            documentsEncryptionProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onSetMax(int i, int max) {
                        taskProgressEvent.progresses[i+1].setMax(max);
                        if (!documentsEncryptionProgressWindow.isClosed()) {
                            documentsEncryptionProgressWindow.update(taskProgressEvent);
                        }
                    }
                });

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            while (!encryptionBatches.isEmpty()) {
                                EncryptionBatch encryptionBatch = encryptionBatches.poll();
                                try {
                                    taskProgressEvent.progresses[0].setMessage(
                                            encryptionBatch.getParent().logicalPath());
                                } catch (ParentNotFoundException e) {
                                    LOG.error("Database closed", e);
                                    taskProgressEvent.progresses[0].setMessage(
                                            encryptionBatch.getParent().getDisplayName());
                                }
                                taskProgressEvent.progresses[0].setMax(numBatchesToProcess);
                                taskProgressEvent.progresses[0].setProgress(
                                        numBatchesToProcess - encryptionBatches.size() - 1);
                                taskProgressEvent.progresses[1].setMax(
                                        encryptionBatch.getDocuments().size());
                                if (!documentsEncryptionProgressWindow.isClosed()) {
                                    documentsEncryptionProgressWindow.update(taskProgressEvent);
                                }
                                if (null != encryptionBatch.getDocuments() &&
                                        !encryptionBatch.getDocuments().isEmpty()) {
                                    documentsEncryptionProcess.encryptDocuments(
                                            encryptionBatch.getDocuments(),
                                            encryptionBatch.getParent().getId(),
                                            encryptionBatch.getKeyAlias());
                                }
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                        }
                        if (!documentsEncryptionProgressWindow.isClosed()) {
                            documentsEncryptionProgressWindow.close(true);
                        }
                        appWindow.onDocumentsEncryptionDone(documentsEncryptionProcess.getResults());
                        setProcess(null);
                        LOG.debug("Exiting encryption thread");
                    }
                }.start();
            }
        } catch (ProgressWindowCreationException e) {
            LOG.error("Failed to get progress window {}",
                    e.getProgressWindowClass().getCanonicalName(), e);
        }
    }
}
