package fr.petrus.lib.core.crypto.mac;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.crypto.CryptoException;

/**
 * TODO: JD
 * Created by pierre on 06/10/16.
 */

public class JcaMac implements Mac {
    private javax.crypto.Mac mac;

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
