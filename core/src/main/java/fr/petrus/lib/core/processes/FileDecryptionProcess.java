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

package fr.petrus.lib.core.processes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.petrus.lib.core.ParentNotFoundException;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataStream;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The {@code Process} which handles a single file decryption.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class FileDecryptionProcess extends AbstractProcess<FileDecryptionProcess.Results> {

    private static Logger LOG = LoggerFactory.getLogger(FileDecryptionProcess.class);

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     *
     * <p>This process returns no results by this means so this implementation holds nothing
     */
    public static class Results extends BaseProcessResults<Void, Void> {
        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super(textI18n, false, false, false);
        }
    }

    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private ProgressListener progressListener = null;

    /**
     * Creates a new {@code FileDecryptionProcess}, providing its dependencies.
     *
     * @param crypto             a {@code Crypto} instance
     * @param keyManager         a {@code KeyManager} instance
     * @param textI18n           a {@code TextI18n} instance
     */
    public FileDecryptionProcess(Crypto crypto, KeyManager keyManager, TextI18n textI18n) {
        super(new Results(textI18n));
        this.crypto = crypto;
        this.keyManager = keyManager;
    }

    /**
     * Sets the {@code ProgressListener} which this process will report its progress to.
     *
     * @param progressListener the {@code ProgressListener} which this process will report its progress to
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Decrypts the given {@code srcEncryptedDocument} and saves the decrypted file with the given
     * {@code dstFilePath}.
     *
     * @param srcEncryptedDocument the {@code EncryptedDocument} to decrypt
     * @param dstFilePath the path of the destination decrypted file
     * @throws DatabaseConnectionClosedException if the database connection is closed
     * @throws StorageCryptException if an error occurs when decrypting the file
     */
    public void decryptFile(EncryptedDocument srcEncryptedDocument, String dstFilePath)
            throws DatabaseConnectionClosedException, StorageCryptException {
        start();
        if (null!= srcEncryptedDocument && null!=dstFilePath) {
            InputStream srcFileInputStream = null;
            OutputStream dstFileOutputStream = null;

            try {
                File dstFile = new File(dstFilePath);
                int size = (int) srcEncryptedDocument.getSize();
                if (null != progressListener) {
                    try {
                        progressListener.onMessage(0, srcEncryptedDocument.logicalPath());
                    } catch (ParentNotFoundException e) {
                        LOG.error("Database is closed", e);
                        progressListener.onMessage(0, srcEncryptedDocument.getDisplayName());
                    }
                    progressListener.onSetMax(0, size);
                }

                File srcFile;
                try {
                    srcFile = srcEncryptedDocument.file();
                    srcFileInputStream = new FileInputStream(srcFile);
                } catch (IOException e) {
                    throw new StorageCryptException("Error while opening source file : " + srcEncryptedDocument.getDisplayName(),
                            StorageCryptException.Reason.SourceFileOpenError, e);
                }

                try {
                    dstFileOutputStream = new FileOutputStream(dstFile);
                } catch (IOException e) {
                    throw new StorageCryptException("Error while opening destination file : " + dstFilePath,
                            StorageCryptException.Reason.DestinationFileOpenError, e);
                }

                try {
                    EncryptedDataStream encryptedDataStream = new EncryptedDataStream(crypto,
                            keyManager.getKeys(srcEncryptedDocument.getKeyAlias()));
                    encryptedDataStream.decrypt(srcFileInputStream, dstFileOutputStream, new ProcessProgressAdapter() {
                        @Override
                        public void onProgress(int i, int progress) {
                            if (null != progressListener) {
                                if (0 == i) {
                                    progressListener.onProgress(0, progress);
                                }
                            }
                        }

                        @Override
                        public void onSetMax(int i, int max) {
                            if (null != progressListener) {
                                if (0 == i) {
                                    progressListener.onSetMax(0, max);
                                }
                            }
                        }

                        @Override
                        public boolean isCanceled() {
                            return FileDecryptionProcess.this.isCanceled();
                        }

                        @Override
                        public void pauseIfNeeded() {
                            FileDecryptionProcess.this.pauseIfNeeded();
                        }
                    });
                } catch (CryptoException e) {
                    dstFile.delete();
                    throw new StorageCryptException("Error while decrypting",
                            StorageCryptException.Reason.DecryptionError, e);
                }
            } finally {
                if (null != srcFileInputStream) {
                    try {
                        srcFileInputStream.close();
                    } catch (IOException e) {
                        LOG.error("Error when closing source input stream", e);
                    }
                }
                if (null != dstFileOutputStream) {
                    try {
                        dstFileOutputStream.close();
                    } catch (IOException e) {
                        LOG.error("Error when closing destination output stream", e);
                    }
                }
            }
        }
    }
}
