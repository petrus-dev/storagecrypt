package fr.petrus.tools.storagecrypt.desktop.platform.crypto;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.crypto.mac.Mac;

/**
 * A class which performs a Mac signature using BouncyCastle LightWeight API
 *
 * @author Pierre Sagne
 * @since 08.10.2016
 */

public class BCLightWeightApiMac implements Mac {
    private HMac mac;

    /**
     * Creates a new JcaMac initialized with the given {@code key}
     *
     * @param key the key used to initialize the Mac
     */
    public BCLightWeightApiMac(SecretKey key) {
        final Digest digest = new SHA256Digest();
        mac = new HMac(digest);
        mac.init(new KeyParameter(key.getEncoded()));
    }

    @Override
    public void update(byte[] data) {
        mac.update(data, 0, data.length);
    }

    @Override
    public byte[] doFinal() {
        final byte[] resultBuffer = new byte[mac.getUnderlyingDigest().getDigestSize()];
        mac.doFinal(resultBuffer, 0);
        return resultBuffer;
    }
}
