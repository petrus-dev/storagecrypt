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

import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * This interface provides utility methods for encrypting/decrypting and encoding/decoding data.
 *
 * @author Pierre Sagne
 * @since 22.03.2016
 */
public interface Crypto {

    /**
     * This method has to be called before actually using any other method, to be sure the
     * cryptographic library is properly initialized and registered.
     */
    void initProvider();

    /**
     * Returns wether AES 256 is supported.
     *
     * <p>If it returns false, the platform lacks the Unlimited Strength Java Cryptographic Extension.
     * The application should display an error message and quit if you plan to use AES 256.
     *
     * @return true AES 256 is supported, false otherwise
     */
    boolean isAes256Supported();

    /**
     * Generates an encryption key of the given length.
     *
     * @param keyLength the key length (256 for AES 256 encryption)
     * @return the generated key
     * @throws CryptoException if any cryptographic error occurs
     */
    SecretKey generateEncryptionKey(int keyLength) throws CryptoException;

    /**
     * Encrypts binary {@code data} with a {@code key} and returns an encrypted chunk of data.
     *
     * @param key  the encryption key
     * @param data the data to be encrypted
     * @return the encrypted data chunk
     * @throws CryptoException if any encryption error occurs
     */
    EncryptedDataChunk encrypt(SecretKey key, byte[] data) throws CryptoException;

    /**
     * Decrypts a {@code data} chunk with a {@code key} and returns the decrypted binary data.
     *
     * @param key  the encryption key
     * @param data the encrypted data chunk to be decrypted
     * @return the decrypted data
     * @throws CryptoException if any decryption error occurs
     */
    byte[] decrypt(SecretKey key, EncryptedDataChunk data) throws CryptoException;

    /**
     * Generates a signature key of the given length.
     *
     * @param keyLength the key length (256 for AES 256)
     * @return the generated key
     * @throws CryptoException if any cryptographic error occurs
     */
    SecretKey generateSignatureKey(int keyLength) throws CryptoException;

    /**
     * Initializes a MAC with a given signature {@code key} and returns..
     *
     * @param key the signature key
     * @return the MAC initialized with the given {@code key}
     * @throws CryptoException if any cryptographic error occurs
     */
    Mac initMac(SecretKey key) throws CryptoException;

    /**
     * Computes the signature of the given binary {@code data}, with the given {@code key}.
     *
     * @param key  the signature key
     * @param data the data to be signed with the {@code key}
     * @return the signature
     * @throws CryptoException if any cryptographic error occurs
     */
    byte[] computeSignature(SecretKey key, byte[] data) throws CryptoException;

    /**
     * Compares a {@code computedSignature} with a {@code referenceSignature}.
     *
     * @param computedSignature  the computed signature
     * @param referenceSignature the reference signature
     * @return true if both signatures match, false otherwise
     */
    boolean verifySignature(byte[] computedSignature, byte[] referenceSignature);

    /**
     * Computes the signature of the given binary {@code data} with the given {@code key},
     * compares it with the given {@code referenceSignature}, and returns whether they match.
     *
     * @param key                the signature key used to sign the binary {@code data}
     * @param data               the data to be verified
     * @param referenceSignature the reference signature to check the {@code data} against
     * @return true if the computed signature and the reference signature match, false otherwise
     * @throws CryptoException if any cryptographic error occurs
     */
    boolean verifySignature(SecretKey key, byte[] data, byte[] referenceSignature) throws CryptoException;

    /**
     * Encodes the given binary {@code data} into a Base64 String.
     *
     * @param data the binary data to encode
     * @return the Base64 encoded String
     */
    String encodeBase64(byte[] data);

    /**
     * Decodes the given Base64 String into binary data.
     *
     * @param data the Base64 encoded String to decode
     * @return the decoded binary data
     */
    byte[] decodeBase64(String data);

    /**
     * Encodes the given binary {@code data} into a "Url Safe" Base64 String.
     *
     * @param data the binary data to encode
     * @return the "Url Safe" Base64 encoded String
     */
    String encodeUrlSafeBase64(byte[] data);

    /**
     * Decode a "Url Safe" Base64 String into binary data.
     *
     * @param data the "Url Safe" Base64 encoded String
     * @return the decoded binary data
     */
    byte[] decodeUrlSafeBase64(String data);

    /**
     * Encodes the given binary data into a String, using UTF-8 charset.
     *
     * @param data the binary data to encode
     * @return the encoded String
     */
    String encodeUtf8(byte[] data);

    /**
     * Decodes the given String into binary data, using UTF-8 charset.
     *
     * @param data the String to be decoded
     * @return the decoded binary data
     */
    byte[] decodeUtf8(String data);

    /**
     * Securely generates a random CSRF token with a minimum given {@code size} (in bytes).
     *
     * @param size the size of the token (in bytes)
     * @return the Base64 encoded CSRF token
     * @throws NoSuchAlgorithmException if the requested algorithm is not available
     */
    String generateCSRFToken(int size) throws NoSuchAlgorithmException;

    /**
     * Securely generates a random password with a minimum given {@code size} (in bytes).
     *
     * @param size the size of the token
     * @return the random password
     * @throws NoSuchAlgorithmException if the requested algorithm is not available
     */
    String generateRandomPassword(int size) throws NoSuchAlgorithmException;
}
