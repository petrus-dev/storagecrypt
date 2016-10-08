package fr.petrus.lib.core.crypto.mac;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.crypto.CryptoException;

/**
 * A class which performs a Mac signature using JCA
 *
 * @author Pierre Sagne
 * @since 06.10.2016
 */

public class JcaMac implements Mac {
    private javax.crypto.Mac mac;

    /**
     * Creates a new JcaMac initialized with the given {@code key}
     *
     * @param key the key used to initialize the Mac
     * @throws CryptoException if any cryptographic error occurs
     */
    public JcaMac(SecretKey key) throws CryptoException {
        try {
            mac = javax.crypto.Mac.getInstance(Constants.CRYPTO.MAC_ALGO);
            mac.init(key);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public void update(byte[] data) {
        mac.update(data);
    }

    @Override
    public byte[] doFinal() {
        return mac.doFinal();
    }
}
