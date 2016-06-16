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

package fr.petrus.lib.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataChunk;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.crypto.SecretKeys;
import fr.petrus.lib.core.utils.StringUtils;

/**
 * This class handles the encrypted metadata of a document.
 *
 * <p>The metadata consists of the name of the document and its MIME type.
 *
 * <p>It is stored encrypted with the key referenced by the provided alias.
 *
 * @author Pierre Sagne
 * @since 13.11.2015
 */
public class EncryptedDocumentMetadata {

    private static Logger LOG = LoggerFactory.getLogger(EncryptedDocumentMetadata.class);

    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private String keyAlias = null;
    private String mimeType = null;
    private String displayName = null;

    /**
     * Creates an {@code EncryptedDocumentMetadata} instance, providing its dependencies.
     *
     * @param crypto            a {@code Crypto} instance
     * @param keyManager        a {@code KeyManager} instance
     */
    public EncryptedDocumentMetadata(Crypto crypto, KeyManager keyManager) {
        this.crypto = crypto;
        this.keyManager = keyManager;
    }

    /**
     * Sets the given {@code mimeType} and {@code displayName} of the document this metadata refers to,
     * and the given {@code keyAlias}, which will be used to encrypt or decrypt it.
     *
     * @param mimeType the mime type of the document which this metadata refers to
     * @param displayName the name of the document which this metadata refers to
     * @param keyAlias the alias of the key to encrypt or decrypt this metadata with
     */
    public void setMetadata(String mimeType, String displayName, String keyAlias) {
        this.mimeType = mimeType;
        this.displayName = displayName;
        this.keyAlias = keyAlias;
    }

    /**
     * Returns the alias of the key this metadata is encrypted with.
     *
     * @return the alias of the key this metadata is encrypted with
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Returns the mime type of the document which this metadata refers to.
     *
     * @return the mime type of the document which this metadata refers to
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the name of the document which this metadata refers to.
     *
     * @return the name of the document which this metadata refers to
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Decrypts the given {@code encryptedMetadata} and stores the decrypted metadata in this object.
     *
     * @param encryptedMetadata the encrypted metadata to decrypt
     * @throws StorageCryptException if no key can decrypt the given {@code encryptedMetadata}
     */
    public void decrypt(String encryptedMetadata) throws StorageCryptException {
        for (String alias : keyManager.getKeyAliases()) {
            try {
                String[] documentNameData = decryptMetadata(encryptedMetadata, keyManager.getKeys(alias));
                this.keyAlias = alias;
                this.mimeType = documentNameData[0];
                this.displayName = documentNameData[1];
                return;
            } catch (StorageCryptException e) {
                LOG.info("Unable to decrypt file with {} key, trying the next one.", alias);
            } catch (CryptoException e) {
                LOG.info("Unable to get {} key", alias);
            }
        }
        throw new StorageCryptException("No key matches", StorageCryptException.Reason.KeyNotFound);
    }

    /**
     * Encrypts the metadata as a byte array.
     *
     * @return the encrypted metadata or null if the {@code displayName}, {@code mimeType} or
     *         {@code keyAlias} is null
     * @throws StorageCryptException if an error occurs
     */
    public byte[] encryptToBytes() throws StorageCryptException {

        if (null==displayName || null==mimeType || null==keyAlias) {
            return null;
        }

        SecretKeys secretKeys;
        try {
            secretKeys = keyManager.getKeys(keyAlias);
        } catch (CryptoException e) {
            throw new StorageCryptException("Error: key not found.", StorageCryptException.Reason.KeyNotFound);
        }

        if (null==secretKeys) {
            throw new StorageCryptException("Error: key not found.", StorageCryptException.Reason.KeyNotFound);
        }

        try {
            String unencryptedName = Constants.CRYPTO.ENCRYPTED_DOCUMENT_NAME_HEADER +
                    Constants.CRYPTO.ENCRYPTED_DOCUMENT_NAME_SEPARATOR + mimeType +
                    Constants.CRYPTO.ENCRYPTED_DOCUMENT_NAME_SEPARATOR + displayName;
            EncryptedDataChunk encryptedDataChunk = crypto.encrypt(secretKeys.getEncryptionKey(),
                    crypto.decodeUtf8(unencryptedName));
            encryptedDataChunk.sign(secretKeys.getSignatureKey());
            return encryptedDataChunk.merge();
        } catch (CryptoException e) {
            throw new StorageCryptException("Error while encrypting file name",
                    StorageCryptException.Reason.EncryptionError, e);
        }
    }

    /**
     * Encrypts the metadata as an "Url Safe" Base64 encrypted String.
     *
     * @return the encrypted metadata or null if the {@code displayName}, {@code mimeType} or
     *         {@code keyAlias} is null
     * @throws StorageCryptException if an error occurs
     */
    public String encryptToBase64() throws StorageCryptException {
        byte[] encryptedData = encryptToBytes();
        if (null==encryptedData) {
            return null;
        } else {
            return crypto.encodeUrlSafeBase64(encryptedData);
        }
    }

    /**
     * Decrypts the encrypted metadata and returns the result as a String array.
     *
     * @param encryptedMetadata the encrypted metadata
     * @param secretKeys        the secret keys to decrypt the metadata with
     * @return the resulting String array with the mime type as the first element, then the name
     * @throws StorageCryptException if an error occurs when decrypting or parsing the metadata
     */
    private String[] decryptMetadataFromBytes(byte[] encryptedMetadata, SecretKeys secretKeys)
            throws StorageCryptException {

        if (null==secretKeys) {
            throw new StorageCryptException("Error: key not found.", StorageCryptException.Reason.KeyNotFound);
        }

        EncryptedDataChunk encryptedDataChunk = new EncryptedDataChunk(crypto);
        if (!encryptedDataChunk.parseEncryptedData(encryptedMetadata)) {
            throw new StorageCryptException("Error: encrypted metadata cannot be parsed.",
                    StorageCryptException.Reason.EncryptedNameDecryptionError);
        }

        String documentData;
        try {
            if (!encryptedDataChunk.verify(secretKeys.getSignatureKey())) {
                throw new StorageCryptException("Error: encrypted name signature verification failed.",
                        StorageCryptException.Reason.EncryptedNameSignatureVerificationError);
            }
            documentData = crypto.encodeUtf8(crypto.decrypt(secretKeys.getEncryptionKey(), encryptedDataChunk));
        } catch (CryptoException e) {
            throw new StorageCryptException("Error while decrypting file name",
                    StorageCryptException.Reason.EncryptedNameDecryptionError, e);
        }

        String[] splitData = StringUtils.splitAtFirstSeparator(documentData, Constants.CRYPTO.ENCRYPTED_DOCUMENT_NAME_SEPARATOR);
        if (null==splitData) {
            throw new StorageCryptException("Error: impossible to parse document data.",
                    StorageCryptException.Reason.DecryptedNameParsingError);
        }

        String header = splitData[0];
        String fileNameData = splitData[1];

        if (!header.equals(Constants.CRYPTO.ENCRYPTED_DOCUMENT_NAME_HEADER)) {
            throw new StorageCryptException("Error: impossible to parse document data : bad header.",
                    StorageCryptException.Reason.DecryptedNameParsingErrorBadHeader);
        }

        splitData = StringUtils.splitAtFirstSeparator(fileNameData, Constants.CRYPTO.ENCRYPTED_DOCUMENT_NAME_SEPARATOR);
        if (null==splitData) {
            throw new StorageCryptException("Error: impossible to parse document data.",
                    StorageCryptException.Reason.DecryptedNameParsingError);
        }

        return splitData;
    }

    /**
     * Decrypts the encrypted metadata and returns the result as a String array.
     *
     * @param encryptedMetadata the encrypted metadata
     * @param secretKeys        the secret keys to decrypt the metadata with
     * @return the resulting String array with the mime type as the first element, then the name
     * @throws StorageCryptException if an error occurs when decrypting or parsing the metadata
     */
    private String[] decryptMetadata(String encryptedMetadata, SecretKeys secretKeys)
            throws StorageCryptException {
        return decryptMetadataFromBytes(crypto.decodeUrlSafeBase64(encryptedMetadata), secretKeys);
    }
}
