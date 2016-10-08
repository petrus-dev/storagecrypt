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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.crypto.mac.Mac;
import fr.petrus.lib.core.utils.NumberConv;

/**
 * This class stores an encrypted data block, along with its initialization vector, and signature.
 *
 * <p>Serialization to a single array and deserialization are also supported.
 *
 * @author Pierre Sagne
 * @since 05.02.2015
 */
public class EncryptedDataChunk {
    private Crypto crypto;

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

    /**
     * The signature of the data.
     */
    private byte[] signature;

    /**
     * The initialization vector used which was used to seed the encryption.
     */
    private byte[] iv;

    /**
     * The encrypted data.
     */
    private byte[] data;

    /**
     * Creates a new empty EncryptedDataChunk.
     *
     * @param crypto : the {@code Crypto instance} which will be used to perform cryptographic operations.
     */
    public EncryptedDataChunk(Crypto crypto) {
        this.crypto = crypto;
        reset();
    }

    /**
     * Resets the stored encrypted data
     */
    private void reset() {
        version = Constants.CRYPTO.CHUNK_VERSION;
        signature = null;
        iv = null;
        data = null;
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
        if (data.length<offset+Constants.CRYPTO.CHUNK_PREFIX.length) {
            return false;
        }
        for (int i=0; i<Constants.CRYPTO.CHUNK_PREFIX.length; i++) {
            if (data[offset+i]!=Constants.CRYPTO.CHUNK_PREFIX[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the size of the serialized encrypted data including signature, and initialization vector.
     *
     * @return the total size of the serialized data
     */
    public int getEncryptedSize() {
        // reserve length for the prefix and for the length indicators
        int size = Constants.CRYPTO.CHUNK_PREFIX.length + VERSION_BYTES + 3*LENGTH_BYTES;

        // add the size of the signature
        if (null!=signature) {
            size += signature.length;
        }

        // add the size of the initialization vector
        if (null!=iv) {
            size += iv.length;
        }

        // finally add the length of the encrypted data
        if (null!=data) {
            size += data.length;
        }
        return size;
    }

    /**
     * Returns the signature of this chunk.
     *
     * @return the signature of this chunk
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Returns the initialization vector of this chunk.
     *
     * @return the initialization vector of this chunk
     */
    public byte[] getIV() {
        return iv;
    }

    /**
     * Returns the encrypted data of this chunk.
     *
     * @return the encrypted data of this chunk
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Sets the initialization vector for this chunk.
     *
     * @param iv the initialization vector for this chunk
     */
    public void setIV(byte[] iv) {
        this.iv = iv;
    }

    /**
     * Sets the encrypted data for this chunk.
     *
     * @param data the encrypted data for this chunk
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Serializes the signature, initialization vector and encrypted data.
     *
     * @return the serialized data, or null if there is no initialization vector or no data
     */
    public byte[] merge() {
        /* if there is no initialization vector or no signature, return null */
        if (null==iv || null==data ) {
            return null;
        }

        /* Compute the total length of the resulting array */
        int totalLength = getEncryptedSize();

        byte[] result = new byte[totalLength];
        int currentPos = 0;

        /* prepend the chunk prefix */
        System.arraycopy(Constants.CRYPTO.CHUNK_PREFIX, 0, result, currentPos,
                Constants.CRYPTO.CHUNK_PREFIX.length);
        currentPos += Constants.CRYPTO.CHUNK_PREFIX.length;

        /* add the format version number */
        System.arraycopy(NumberConv.shortToByteArray(Constants.CRYPTO.CHUNK_VERSION), 0, result,
                currentPos, VERSION_BYTES);
        currentPos += VERSION_BYTES;

        if (null==signature) {
            /* if there is no signature, just output length = 0 */
            System.arraycopy(NumberConv.intToByteArray(0), 0, result, currentPos, LENGTH_BYTES);
            currentPos += LENGTH_BYTES;
        } else {
            /* if signature is present, output its length ...*/
            System.arraycopy(NumberConv.intToByteArray(signature.length), 0, result, currentPos, LENGTH_BYTES);
            currentPos += LENGTH_BYTES;

            /* ... then the signature itself */
            System.arraycopy(signature, 0, result, currentPos, signature.length);
            currentPos += signature.length;
        }

        /* output the initialization vector length ... */
        System.arraycopy(NumberConv.intToByteArray(iv.length), 0, result, currentPos, LENGTH_BYTES);
        currentPos += LENGTH_BYTES;

        /* ... then the initialization vector itself */
        System.arraycopy(iv, 0, result, currentPos, iv.length);
        currentPos += iv.length;

        /* finally, output the encrypted data length ... */
        System.arraycopy(NumberConv.intToByteArray(data.length), 0, result, currentPos, LENGTH_BYTES);
        currentPos += LENGTH_BYTES;

        /* ... then the encrypted data itself */
        System.arraycopy(data, 0, result, currentPos, data.length);
        return result;
    }

    /**
     * Parses the given {@code encryptedData}.
     *
     * @param encryptedData the raw serialized encrypted data chunk
     * @return true if the given {@code encryptedData} was successfully parsed
     */
    public boolean parseEncryptedData(byte[] encryptedData) {
        reset();

        /* if there is no data to proceed, leave everything empty */
        if (null==encryptedData || 0==encryptedData.length) {
            return false;
        }

        if (!checkPrefix(encryptedData, 0)) {
            return false;
        }

        /* Begin after the prefix */
        int currentPos = Constants.CRYPTO.CHUNK_PREFIX.length;

        /* Get the format version */
        version = NumberConv.byteArrayToShort(encryptedData, currentPos);
        currentPos += VERSION_BYTES;

        /* Get the signature length */
        int signatureLength = NumberConv.byteArrayToInt(encryptedData, currentPos);
        currentPos += LENGTH_BYTES;

        /* Get the signature */
        if (signatureLength > 0) {
            signature = new byte[signatureLength];
            System.arraycopy(encryptedData, currentPos, signature, 0, signatureLength);
            currentPos += signatureLength;
        }

        /* Get the Initialization Vector length */
        int ivLength = NumberConv.byteArrayToInt(encryptedData, currentPos);
        currentPos += LENGTH_BYTES;

        /* Get the Initialization Vector */
        if (ivLength > 0) {
            iv = new byte[ivLength];
            System.arraycopy(encryptedData, currentPos, iv, 0, ivLength);
            currentPos += ivLength;
        }

        /* Get the encrypted data length */
        int dataLength = NumberConv.byteArrayToInt(encryptedData, currentPos);
        currentPos += LENGTH_BYTES;

        /* Get the encrypted data */
        if (dataLength>0) {
            data = new byte[dataLength];
            System.arraycopy(encryptedData, currentPos, data, 0, dataLength);
        }
        return true;
    }

    /**
     * Reads the signature, initialization vector and encrypted data from an {@code inputStream}.
     *
     * <p>This method does not close the {@code inputStream}.
     *
     * @param inputStream the input stream to read from
     * @return true if at least the initialization vector and data where present, false otherwise
     * @throws IOException if an error occurs while reading
     */
    public boolean read(InputStream inputStream) throws IOException {
        /* Read and check the prefix */
        byte[] prefix = new byte[Constants.CRYPTO.CHUNK_PREFIX.length];
        if (prefix.length != inputStream.read(prefix)) {
            if (!Arrays.equals(Constants.CRYPTO.CHUNK_PREFIX, prefix)) {
                return false;
            }
        }

        byte[] versionBytes = new byte[VERSION_BYTES];

        /* Read the version */
        if (versionBytes.length != inputStream.read(versionBytes)) {
            return false;
        }
        version = NumberConv.byteArrayToShort(versionBytes, 0);


        byte[] lengthBytes = new byte[LENGTH_BYTES];

        /* Read the signature length */
        if (lengthBytes.length != inputStream.read(lengthBytes)) {
            return false;
        }
        int signatureLength = NumberConv.byteArrayToInt(lengthBytes, 0);

        /* Read the signature itself */
        if (signatureLength>0) {
            signature = new byte[signatureLength];
            if (signature.length != inputStream.read(signature)) {
                return false;
            }
        } else {
            signature = null;
        }

        /* Read the initialization vector length */
        if (lengthBytes.length != inputStream.read(lengthBytes)) {
            return false;
        }
        int ivLength = NumberConv.byteArrayToInt(lengthBytes, 0);

        /* Read the initialization vector itself */
        if (ivLength>0) {
            iv = new byte[ivLength];
            if (iv.length != inputStream.read(iv)) {
                return false;
            }
        } else {
            iv = null;
        }

        /* Read the encrypted data length */
        if (lengthBytes.length != inputStream.read(lengthBytes)) {
            return false;
        }
        int dataLength = NumberConv.byteArrayToInt(lengthBytes, 0);

        /* Read the encrypted data itself */
        if (dataLength>0) {
            data = new byte[dataLength];
            if (data.length != inputStream.read(data)) {
                return false;
            }
        } else {
            data = null;
        }
        return true;
    }

    /**
     * Reads the signature, initialization vector and encrypted data from an {@code inputStream},
     * specifying a read {@code bufferSize}.
     *
     * <p>This method does not close the {@code inputStream}.
     *
     * @param inputStream the input stream to read
     * @param bufferSize the input buffer size
     * @return true if at least the initialization vector and data where present and bufferSize > 0,
     *         false otherwise
     * @throws IOException if an error occurs while reading
     */
    public boolean read(InputStream inputStream, int bufferSize) throws IOException, CryptoException {
        /* Return false if inputStream is null or bufferSize is 0 or less */
        if (null==inputStream || bufferSize<=1) {
            return false;
        }

        /* Read and check the prefix */
        byte[] prefix = new byte[Constants.CRYPTO.CHUNK_PREFIX.length];
        if (prefix.length != inputStream.read(prefix)) {
            if (!Arrays.equals(Constants.CRYPTO.CHUNK_PREFIX, prefix)) {
                return false;
            }
        }

        byte[] versionBytes = new byte[VERSION_BYTES];

        /* Read the version */
        if (versionBytes.length != inputStream.read(versionBytes)) {
            return false;
        }
        version = NumberConv.byteArrayToShort(versionBytes, 0);

        byte[] lengthBytes = new byte[LENGTH_BYTES];

        /* Read the signature length */
        if (lengthBytes.length != inputStream.read(lengthBytes)) {
            return false;
        }
        int signatureLength = NumberConv.byteArrayToInt(lengthBytes, 0);

        /* Read the signature itself */
        if (signatureLength>0) {
            signature = new byte[signatureLength];
            if (signature.length != inputStream.read(signature)) {
                return false;
            }
        } else {
            signature = null;
        }

        /* Read the initialization vector length */
        if (lengthBytes.length != inputStream.read(lengthBytes)) {
            return false;
        }
        int ivLength = NumberConv.byteArrayToInt(lengthBytes, 0);

        /* Read the initialization vector itself */
        if (ivLength>0) {
            iv = new byte[ivLength];
            if (iv.length != inputStream.read(iv)) {
                return false;
            }
        } else {
            iv = null;
        }

        /* Read the encrypted data length */
        if (lengthBytes.length != inputStream.read(lengthBytes)) {
            return false;
        }
        int dataLength = NumberConv.byteArrayToInt(lengthBytes, 0);

        /* Read the encrypted data itself, in blocks of "bufferSize" length,
         * or in one block if the data size is less than the specified buffer size */
        if (dataLength>0) {
            data = new byte[dataLength];
            for(int offset=0; offset<dataLength; offset+=bufferSize) {
                int remaining = dataLength - offset;
                int bytesToRead = bufferSize<remaining?bufferSize:remaining;
                if (inputStream.read(data, offset, bytesToRead)!=bytesToRead) {
                    return false;
                }
            }
        } else {
            data = null;
        }
        return true;
    }

    /**
     * Writes the signature, initialization vector and encrypted data to an {@code outputStream}
     *
     * <p>This method does not close the {@code outputStream}.
     *
     * @param outputStream the output stream to write to
     * @return true if everything was written
     * @throws IOException if an error occurs while writing
     */
    public boolean write(OutputStream outputStream) throws IOException {
        /* Write the prefix */
        outputStream.write(Constants.CRYPTO.CHUNK_PREFIX);

        /* Write the format version */
        outputStream.write(NumberConv.shortToByteArray(Constants.CRYPTO.CHUNK_VERSION));

        /* Write the signature length and the signature itself */
        if (null==signature) {
            outputStream.write(NumberConv.intToByteArray(0));
        } else {
            outputStream.write(NumberConv.intToByteArray(signature.length));
            outputStream.write(signature);
        }

        /* Write the initialization vector length and the initialization vector itself */
        if (null==iv) {
            outputStream.write(NumberConv.intToByteArray(0));
        } else {
            outputStream.write(NumberConv.intToByteArray(iv.length));
            outputStream.write(iv);
        }

        /* Write the encrypted data length and the encrypted data itself */
        if (null==data) {
            outputStream.write(NumberConv.intToByteArray(0));
        } else {
            outputStream.write(NumberConv.intToByteArray(data.length));
            outputStream.write(data);
        }

        outputStream.flush();
        return true;
    }

    /**
     * Writes the signature, initialization vector and encrypted data to an {@code outputStream},
     * specifying a write {@code bufferSize}
     *
     * <p>This method does not close the {@code outputStream}.
     *
     * @param outputStream the output stream
     * @param bufferSize   the buffer size
     * @return true if everything was written, false otherwise or if the buffer size is less than 1
     * @throws IOException if an error occurs while writing
     */
    public boolean write(OutputStream outputStream, int bufferSize) throws IOException {
        /* Return false if outputStream is null or bufferSize is 0 or less */
        if (null==outputStream || bufferSize<=0) {
            return false;
        }

        /* Write the prefix */
        outputStream.write(Constants.CRYPTO.CHUNK_PREFIX);

        /* Write the format version */
        outputStream.write(NumberConv.shortToByteArray(Constants.CRYPTO.CHUNK_VERSION));

        /* Write the signature length and the signature itself */
        if (null==signature) {
            outputStream.write(NumberConv.intToByteArray(0));
        } else {
            outputStream.write(NumberConv.intToByteArray(signature.length));
            outputStream.write(signature);
        }

        /* Write the initialization vector length and the initialization vector itself */
        if (null==iv) {
            outputStream.write(NumberConv.intToByteArray(0));
        } else {
            outputStream.write(NumberConv.intToByteArray(iv.length));
            outputStream.write(iv);
        }

        /* Write the encrypted data length and the encrypted data itself */
        if (null==data) {
            outputStream.write(NumberConv.intToByteArray(0));
        } else {
            outputStream.write(NumberConv.intToByteArray(data.length));
            for(int offset=0; offset<data.length; offset+=bufferSize) {
                int remaining = data.length - offset;
                outputStream.write(data, offset, bufferSize<remaining?bufferSize:remaining);
            }
        }

        outputStream.flush();
        return true;
    }

    /**
     * Computes a signature of the initialization vector and encrypted data with a given {@code key},
     * and place the resulting signature in the {@code signature} field of this instance.
     *
     * @param key the key used to sign the initialization vector and encrypted data
     * @throws CryptoException if any cryptography error occurs
     */
    public void sign(SecretKey key) throws CryptoException {
        signature = computeSignature(key);
    }

    /**
     * Generates a signature of the initialization vector and encrypted data with a given {@code key},
     * and compares it to the reference signature, which should be present in the {@code signature}
     * field of this instance.
     *
     * @param key the key used to sign the encrypted data
     * @return true if the computed signature matches the reference signature
     * @throws CryptoException if any cryptography error occurs
     */
    public boolean verify(SecretKey key) throws CryptoException {
        if (null==iv || 0==iv.length || null==data) {
            return false;
        }
        byte[] computedSignature = computeSignature(key);
        return Arrays.equals(computedSignature, signature);
    }

    /**
     * Computes a signature of the initialization vector and encrypted data with a given {@code key}.
     *
     * @param key the key used to sign the initialization vector and encrypted data
     * @throws CryptoException if any cryptography error occurs
     */
    private byte[] computeSignature(SecretKey key) throws CryptoException {
        Mac mac = crypto.initMac(key);
        mac.update(NumberConv.intToByteArray(iv.length));
        mac.update(iv);
        mac.update(NumberConv.intToByteArray(data.length));
        mac.update(data);
        return mac.doFinal();
    }
}