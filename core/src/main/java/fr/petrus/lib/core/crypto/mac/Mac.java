package fr.petrus.lib.core.crypto.mac;

/**
 * TODO : JD
 * Created by pierre on 06/10/16.
 */

public interface Mac {
    void update(byte[] data);
    byte[] doFinal();
}
