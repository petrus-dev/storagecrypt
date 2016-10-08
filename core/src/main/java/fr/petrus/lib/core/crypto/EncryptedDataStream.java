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

package fr.petrus.lib.core.crypto;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.crypto.mac.Mac;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.utils.NumberConv;
import fr.petrus.lib.core.utils.StreamUtils;

/**
 * This class encrypts and decrypts a stream made of {@link EncryptedDataChunk}s.
 *
 * @author Pierre Sagne
 * @since 05.02.2015
 */
public class EncryptedDataStream {

    private static Logger LOG = LoggerFactory.getLogger(EncryptedDataStream.class);

    /**
     * Section types : an EncryptedDataStream is made of several data chunks and a global signature
     */
    private static final int SECTION_TYPE_SIGNATURE = 0;
    private static final int SECTION_TYPE_CHUNK = 1;

    /**
     * The number of bytes used in the serialized array to represent the format version.
     */
    private static final int VERSION_BYTES = 2;

    /**
     * The number of bytes used in the serialized array to represent lengths.
     */
    private static final int LENGTH_BYTES = 4;

    /**
     * The format version
     */
    private short version;

    private Crypto crypto;

    private SecretKeys secretKeys;

    /**
     * Creates a new EncryptedDataStream, which data will be processed with the given {@code secretKeys}.
     *
     * @param secretKeys the secret keys used to encrypt, decrypt, sign and verify the data
     */
    public EncryptedDataStream(Crypto crypto, SecretKeys secretKeys) {
        this.crypto = crypto;
        this.secretKeys = secretKeys;
        this.version = Constants.CRYPTO.STREAM_VERSION;
    }

    /**
     * Encrypts and signs data from the given {@code inputStream}, then writes the result to the
     * given {@code outputStream}, followed with a global signature.
     *
     * @param inputStream  the input stream to read the clear data from.
     * @param outputStream the output stream to write the encrypted data to.
     * @param listener     a listener used to report the progress and handle pause/cancelation
     * @throws CryptoException if any cryptographic error occurs
     */
    public void encrypt(InputStream inputStream, OutputStream outputStream, final ProcessProgressListener listener)
            throws CryptoException {

        try {
            /* Write the prefix */
            outputStream.write(Constants.CRYPTO.STREAM_PREFIX);

            /* Write the format version */
            outputStream.write(NumberConv.shortToByteArray(Constants.CRYPTO.STREAM_VERSION));
        } catch (IOException e) {
            throw new CryptoException("Failed to write the data stream header", e);
        }

        /* Initialise the global signature object */
        Mac globalMac;
        try {
            globalMac = crypto.initMac(secretKeys.getSignatureKey());
        } catch (CryptoException e) {
            throw new CryptoException("Failed to initialize global signature", e);
        }

        int processedBytes = 0;
        while (true) {
            if (null!=listener) {
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    return;
                }
            }
            byte[] chunkData = null;
            try {
                chunkData = StreamUtils.read(inputStream, Constants.FILE.BUFFER_SIZE,
                        Constants.CRYPTO.MAX_CHUNK_SIZE, new ProcessProgressAdapter() {
                            @Override
                            public boolean isCanceled() {
                                return null!=listener && listener.isCanceled();
                            }

                            @Override
                            public void pauseIfNeeded() {
                                if (null!=listener) {
                                    listener.pauseIfNeeded();
                                }
                            }
                        });
            } catch (IOException e) {
                LOG.error("IOException : ", e);
            }
            if (null == chunkData) {
                byte[] globalSignature = globalMac.doFinal();
                try {
                    outputStream.write(SECTION_TYPE_SIGNATURE);
                    outputStream.write(NumberConv.intToByteArray(globalSignature.length));
                    outputStream.write(globalSignature);
                    outputStream.flush();
                } catch (IOException e) {
                    throw new CryptoException("Failed to write global signature", e);
                }
                return;
            }

            EncryptedDataChunk cipherDataChunk;
            try {
                cipherDataChunk = crypto.encrypt(secretKeys.getEncryptionKey(), chunkData);
            } catch (CryptoException e) {
                throw new CryptoException("Failed to encrypt data", e);
            }

            try {
                cipherDataChunk.sign(secretKeys.getSignatureKey());
            } catch (CryptoException e) {
                throw new CryptoException("Failed to compute data chunk signature", e);
            }
            globalMac.update(cipherDataChunk.getSignature());

            try {
                outputStream.write(SECTION_TYPE_CHUNK);
                cipherDataChunk.write(outputStream);
            } catch (IOException e) {
                throw new CryptoException("Failed to write encrypted data", e);
            }

            processedBytes += chunkData.length;

            if (null!=listener) {
                listener.onProgress(0, processedBytes);
            }
        }
    }

    /**
     * Decrypts data from the given {@code inputStream}, then writes the result to the given
     * {@code outputStream}.
     *
     * @param inputStream  the input stream to read encrypted data.
     * @param outputStream the output stream to write the decrypted data.
     * @param listener     a listener used to report the progress and handle pause/cancelation
     * @throws CryptoException if any cryptographic error occurs
     */
    public void decrypt(InputStream inputStream, OutputStream outputStream, final ProcessProgressListener listener)
            throws CryptoException {

        try {
            /* Read and check the prefix */
            byte[] prefix = new byte[Constants.CRYPTO.STREAM_PREFIX.length];
            if (prefix.length != inputStream.read(prefix)) {
                if (!Arrays.equals(Constants.CRYPTO.STREAM_PREFIX, prefix)) {
                    throw new CryptoException("The data stream prefix does not match");
                }
            }

            byte[] versionBytes = new byte[VERSION_BYTES];

            /* Read the version */
            if (versionBytes.length != inputStream.read(versionBytes)) {
                throw new CryptoException("Failed to read the data stream header");
            }
            version = NumberConv.byteArrayToShort(versionBytes, 0);
        } catch (IOException e) {
            throw new CryptoException("Failed to read the data stream header", e);
        }

        Mac globalMac;
        try {
            globalMac = crypto.initMac(secretKeys.getSignatureKey());
        } catch (CryptoException e) {
            throw new CryptoException("Failed to initialize global signature", e);
        }

        int processedBytes = 0;
        int numChunk = 0;
        while (true) {
            if (null!=listener) {
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    return;
                }
            }
            int sectionType;
            try {
                sectionType = inputStream.read();
            } catch (IOException e) {
                throw new CryptoException("Failed to read section type", e);
            }
            if (SECTION_TYPE_SIGNATURE == sectionType) {
                byte[] globalSignatureLengthBytes = new byte[LENGTH_BYTES];
                byte[] globalSignature = null;
                try {
                    if (inputStream.read(globalSignatureLengthBytes) != globalSignatureLengthBytes.length) {
                        throw new CryptoException("Failed to read global signature length");
                    }
                    int globalSignatureLength = NumberConv.byteArrayToInt(globalSignatureLengthBytes, 0);
                    if (globalSignatureLength > 0) {
                        globalSignature = new byte[globalSignatureLength];
                        if (inputStream.read(globalSignature) != globalSignature.length) {
                            throw new CryptoException("Failed to read global signature");
                        }
                    }
                } catch (IOException e) {
                    throw new CryptoException("Failed to read global signature", e);
                }

                byte[] computedGlobalSignature = globalMac.doFinal();
                if (!Arrays.equals(computedGlobalSignature, globalSignature)) {
                    throw new CryptoException("Failed to verify global signature or data was tampered with");
                }
                return;
            } else if (SECTION_TYPE_CHUNK == sectionType) {
                EncryptedDataChunk encryptedDataChunk = new EncryptedDataChunk(crypto);
                try {
                    encryptedDataChunk.read(inputStream, Constants.FILE.BUFFER_SIZE);
                } catch (IOException e) {
                    throw new CryptoException("Failed to read data", e);
                }

                boolean signatureOk;
                try {
                    signatureOk = encryptedDataChunk.verify(secretKeys.getSignatureKey());
                } catch (CryptoException e) {
                    throw new CryptoException("Failed to compute data chunk signature", e);
                }
                if (!signatureOk) {
                    throw new CryptoException("Failed to verify data chunk signature or data was tampered with");
                }
                globalMac.update(encryptedDataChunk.getSignature());

                byte[] dataChunk;
                try {
                    dataChunk = crypto.decrypt(secretKeys.getEncryptionKey(), encryptedDataChunk);
                } catch (CryptoException e) {
                    throw new CryptoException("Failed to decrypt data", e);
                }

                try {
                    StreamUtils.write(outputStream, dataChunk, Constants.FILE.BUFFER_SIZE, new ProcessProgressAdapter() {
                        @Override
                        public boolean isCanceled() {
                            return null!=listener && listener.isCanceled();
                        }

                        @Override
                        public void pauseIfNeeded() {
                            if (null!=listener) {
                                listener.pauseIfNeeded();
                            }
                        }
                    });
                    outputStream.flush();
                } catch (IOException e) {
                    throw new CryptoException("Failed to write decrypted data", e);
                }
                numChunk++;
                processedBytes += encryptedDataChunk.getEncryptedSize();
            } else {
                throw new CryptoException("Wrong message section");
            }
            if (null!=listener) {
                listener.onProgress(0, processedBytes);
            }
        }
    }

    /**
     * Checks if the given data has with the chunk prefix at the given offset
     *
     * @param data the data to check
     * @param offset the offset where to check for the prefix
     * @return true if the given {@code data} contains the prefix at the given {@code offset}
     */
    private boolean checkPrefix(byte[] data, int offset) {
        if (null==data) {
            return false;
        }
        if (data.length<offset+Constants.CRYPTO.STREAM_PREFIX.length) {
            return false;
        }
        for (int i=0; i<Constants.CRYPTO.STREAM_PREFIX.length; i++) {
            if (data[offset+i]!=Constants.CRYPTO.STREAM_PREFIX[i]) {
                return false;
            }
        }
        return true;
    }
}
