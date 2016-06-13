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

import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.FileDecryptionProcess;
import fr.petrus.lib.core.result.OnCompletedListener;
import fr.petrus.lib.core.result.ProgressAdapter;
import fr.petrus.tools.storagecrypt.desktop.ProgressWindowCreationException;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.FileDecryptionProgressWindow;

/**
 * The {@code Task} which handles a single file decryption.
 *
 * @see FileDecryptionProcess
 *
 * @author Pierre Sagne
 * @since 20.08.2015
 */
public class FileDecryptionTask extends ProcessTask {

    private static Logger LOG = LoggerFactory.getLogger(FileDecryptionTask.class);

    /**
     * Creates a new {@code FileDecryptionTask} instance.
     *
     * @param appWindow the application window
     */
    public FileDecryptionTask(AppWindow appWindow) {
        super(appWindow);
    }

    /**
     * Starts the decryption task in the background for the given {@code srcEncryptedDocument}.
     *
     * @param srcEncryptedDocument the document to decrypt
     * @param dstFilePath          the path of the destination decrypted file
     */
    public synchronized void decrypt(EncryptedDocument srcEncryptedDocument, String dstFilePath) {
        decrypt(srcEncryptedDocument, dstFilePath, null);
    }

    /**
     * Starts the decryption task in the background for the given {@code srcEncryptedDocument}.
     *
     * @param srcEncryptedDocument the document to decrypt
     * @param dstFilePath          the path of the destination decrypted file
     * @param onCompletedListener  the listener which will receive a message when the decryption
     *                             task is done.
     */
    public synchronized void decrypt(final EncryptedDocument srcEncryptedDocument, final String dstFilePath,
                                     final OnCompletedListener<String> onCompletedListener) {
        try {
            final FileDecryptionProgressWindow decryptionProgressWindow =
                    appWindow.getProgressWindow(FileDecryptionProgressWindow.class);
            if (!hasProcess()) {
                final FileDecryptionProgressWindow.ProgressEvent taskProgressEvent
                        = new FileDecryptionProgressWindow.ProgressEvent();
                final FileDecryptionProcess fileDecryptionProcess = new FileDecryptionProcess(
                        appContext.getCrypto(),
                        appContext.getKeyManager(),
                        appContext.getTextI18n());
                setProcess(fileDecryptionProcess);
                fileDecryptionProcess.setProgressListener(new ProgressAdapter() {
                    @Override
                    public void onProgress(int i, int progress) {
                        taskProgressEvent.progresses[i].setProgress(progress);
                        if (!decryptionProgressWindow.isClosed()) {
                            decryptionProgressWindow.update(taskProgressEvent);
                        }
                    }

                    @Override
                    public void onSetMax(int i, int max) {
                        taskProgressEvent.progresses[i].setMax(max);
                        if (!decryptionProgressWindow.isClosed()) {
                            decryptionProgressWindow.update(taskProgressEvent);
                        }
                    }
                });

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            fileDecryptionProcess.decryptFile(srcEncryptedDocument, dstFilePath);
                            if (null!=onCompletedListener && ! isCanceled()) {
                                onCompletedListener.onSuccess(dstFilePath);
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                        } catch (StorageCryptException e) {
                            LOG.error("Failed to decrypt file", e);
                            String message = textI18n.getExceptionDescription(e);
                            switch (e.getReason()) {
                                case SourceFileOpenError:
                                    message = textBundle.getString(
                                            "error_message_failed_to_open_source_file",
                                            srcEncryptedDocument.getFileName());
                                    break;
                                case DestinationFileOpenError:
                                    message = textBundle.getString(
                                            "error_message_failed_to_open_destination_file",
                                            dstFilePath);
                                    break;
                                case DecryptionError:
                                    message = textBundle.getString(
                                            "error_message_failed_to_decrypt_file",
                                            srcEncryptedDocument.getFileName());
                                    break;
                            }
                            if (null!=onCompletedListener) {
                                onCompletedListener.onFailed(
                                        new StorageCryptException(message, e.getReason(), e));
                            }
                        } finally {
                            if (!decryptionProgressWindow.isClosed()) {
                                decryptionProgressWindow.close(true);
                            }
                            setProcess(null);
                            LOG.debug("Exiting decryption thread");
                        }
                    }
                }.start();
            }
        } catch (ProgressWindowCreationException e) {
            LOG.error("Failed to get progress window {}",
                    e.getProgressWindowClass().getCanonicalName(), e);
        }
    }
}
