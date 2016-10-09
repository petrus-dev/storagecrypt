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

package fr.petrus.tools.storagecrypt.desktop.platform.crypto;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataChunk;
import fr.petrus.lib.core.crypto.keystore.KeyStore;
import fr.petrus.lib.core.crypto.mac.Mac;

/**
 * The {@link Crypto} implementation for the Desktop platform.
 *
 * <p>This implementation uses BouncyCastle LightWeight API.
 *
 * @author Pierre Sagne
 * @since 08.10.2016
 */
public class DesktopBCLightWeightApiCrypto extends DesktopAbstractCrypto {
    private final BlockCipher blockCipher = new AESFastEngine();

    @Override
    public void initProvider() {
    }

    @Override
    public boolean isAes256Supported() {
        return true;
    }

    @Override
    public SecretKey generateEncryptionKey(int keyLength) {
        return new SecretKeySpec(generateRandomByteArray(keyLength/8), Constants.CRYPTO.AES_ENCRYPT_ALGO);
    }

    @Override
    public SecretKey generateSignatureKey(int keyLength) {
        return new SecretKeySpec(generateRandomByteArray(keyLength/8), Constants.CRYPTO.AES_ENCRYPT_ALGO);
    }

    @Override
    public EncryptedDataChunk encrypt(SecretKey key, byte[] data) throws CryptoException {
        EncryptedDataChunk result = new EncryptedDataChunk(this);

        // generate random IV
        result.setIV(generateRandomByteArray(blockCipher.getBlockSize()));

        // setup cipher parameters with key and IV
        KeyParameter keyParam = new KeyParameter(key.getEncoded());
        CipherParameters params = new ParametersWithIV(keyParam, result.getIV());

        // setup AES cipher in CBC mode with PKCS7 padding
        BlockCipherPadding padding = new PKCS7Padding();
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(blockCipher), padding);
        cipher.reset();
        cipher.init(true, params);

        // encrypt and add padding
        byte[] out = new byte[cipher.getOutputSize(data.length)];
        int processBytes = cipher.processBytes(data, 0, data.length, out, 0);
        try {
            cipher.doFinal(out, processBytes);
        } catch (InvalidCipherTextException e) {
            throw new CryptoException(e);
        }

        result.setData(out);
        return result;
    }

    @Override
    public byte[] decrypt(SecretKey key, EncryptedDataChunk dataChunk) throws CryptoException {
        // setup cipher parameters with key and IV
        KeyParameter keyParam = new KeyParameter(key.getEncoded());
        CipherParameters params = new ParametersWithIV(keyParam, dataChunk.getIV());

        // setup AES cipher in CBC mode with PKCS7 padding
        BlockCipherPadding padding = new PKCS7Padding();
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(blockCipher), padding);
        cipher.reset();
        cipher.init(false, params);

        // create a temporary buffer to decode into (it'll include padding)
        byte[] buffer = new byte[cipher.getOutputSize(dataChunk.getData().length)];
        int decryptedLength = cipher.processBytes(dataChunk.getData(), 0, dataChunk.getData().length, buffer, 0);
        try {
            decryptedLength += cipher.doFinal(buffer, decryptedLength);
        } catch (InvalidCipherTextException e) {
            throw new CryptoException(e);
        }

        // remove padding
        byte[] out = new byte[decryptedLength];
        System.arraycopy(buffer, 0, out, 0, decryptedLength);

        return out;
    }

    @Override
    public Mac initMac(SecretKey key) throws CryptoException {
        return new BCLightWeightApiMac(key);
    }

    @Override
    public KeyStore newKeyStore() throws CryptoException {
        return new BCLightWeightApiKeyStoreUber();
    }
}
