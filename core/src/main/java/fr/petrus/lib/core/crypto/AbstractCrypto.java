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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.crypto.keystore.JcaKeyStoreUber;
import fr.petrus.lib.core.crypto.keystore.KeyStore;
import fr.petrus.lib.core.crypto.mac.JcaMac;
import fr.petrus.lib.core.crypto.mac.Mac;

/**
 * This abstract class implements the methods which are the same for all implementations of the
 * {@code Crypto} interface.
 *
 * @author Pierre Sagne
 * @since 15.12.2014
 */
public abstract class AbstractCrypto implements Crypto {
    private static Logger LOG = LoggerFactory.getLogger(AbstractCrypto.class);

    @Override
    public boolean isAes256Supported() {
        try {
            if (Cipher.getMaxAllowedKeyLength(Constants.CRYPTO.AES_ENCRYPT_ALGO) >= 256) {
                return true;
            }
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Failed to get max key length for AES", e);
        }
        return false;
    }

    @Override
    public SecretKey generateEncryptionKey(int keyLength) throws CryptoException {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(Constants.CRYPTO.AES_ENCRYPT_ALGO);
            kg.init(keyLength, new SecureRandom());
            return new SecretKeySpec((kg.generateKey()).getEncoded(), Constants.CRYPTO.AES_ENCRYPT_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public EncryptedDataChunk encrypt(SecretKey key, byte[] data) throws CryptoException {
        try {
            Cipher c = Cipher.getInstance(Constants.CRYPTO.AES_FULL_ENCRYPT_ALGO);
            c.init(Cipher.ENCRYPT_MODE, key, new SecureRandom());
            AlgorithmParameters params = c.getParameters();
            EncryptedDataChunk result = new EncryptedDataChunk(this);
            result.setIV(params.getParameterSpec(IvParameterSpec.class).getIV());
            result.setData(c.doFinal(data));
            return result;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException |
                BadPaddingException | InvalidKeyException | InvalidParameterSpecException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public byte[] decrypt(SecretKey key, EncryptedDataChunk dataChunk) throws CryptoException {
        try {
            Cipher c = Cipher.getInstance(Constants.CRYPTO.AES_FULL_ENCRYPT_ALGO);
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(dataChunk.getIV()));
            return c.doFinal(dataChunk.getData());
        } catch (NoSuchPaddingException  | InvalidAlgorithmParameterException |
                NoSuchAlgorithmException | IllegalBlockSizeException |
                BadPaddingException | InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public SecretKey generateSignatureKey(int keyLength) throws CryptoException {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(Constants.CRYPTO.MAC_ALGO);
            kg.init(keyLength, new SecureRandom());
            return new SecretKeySpec((kg.generateKey()).getEncoded(), Constants.CRYPTO.AES_ENCRYPT_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public Mac initMac(SecretKey key) throws CryptoException {
        return new JcaMac(key);
    }

    @Override
    public byte[] computeSignature(SecretKey key, byte[] data) throws CryptoException {
        Mac mac = initMac(key);
        mac.update(data);
        return mac.doFinal();
    }

    @Override
    public boolean verifySignature(byte[] computedSignature, byte[] referenceSignature) {
        return Arrays.equals(computedSignature, referenceSignature);
    }

    @Override
    public boolean verifySignature(SecretKey key, byte[] data, byte[] referenceSignature) throws CryptoException {
        byte[] computedSignature = computeSignature(key, data);
        return verifySignature(computedSignature, referenceSignature);
    }

    @Override
    public String encodeUtf8(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] decodeUtf8(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     * This implementation generates a random byte array of the given {@code size} (in bytes)
     * and returns the Base64 encoded result.
     *
     * <p>The result size will be greater than the given {@code size} because of the Base64 encoding.
     */
    @Override
    public String generateCSRFToken(int size) throws NoSuchAlgorithmException {
        byte[] random = new byte[size];
        new SecureRandom().nextBytes(random);
        return encodeBase64(random);
    }

    /**
     * {@inheritDoc}
     * This implementation generates a random byte array of the given {@code size} (in bytes)
     * and returns the "Url Safe" Base64 encoded result.
     *
     * <p>The result size will be greater than the given {@code size} because of the Base64 encoding.
     */
    @Override
    public String generateRandomPassword(int size) throws NoSuchAlgorithmException {
        byte[] random = new byte[size];
        new SecureRandom().nextBytes(random);
        return encodeUrlSafeBase64(random);
    }

    @Override
    public KeyStore newKeyStore() throws CryptoException {
        return new JcaKeyStoreUber();
    }
}
