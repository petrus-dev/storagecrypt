package fr.petrus.lib.core.crypto.mac;

/**
 * The interface used to generate a Mac signature
 *
 * @author Pierre Sagne
 * @since 06.10.2016
 */

public interface Mac {

    /**
     * Processes the given array of bytes
     *
     * @param data the bytes to process
     */
    void update(byte[] data);

    /**
     * Finishes the MAC operation and returns the signature
     *
     * @return the signature
     */
    byte[] doFinal();
}
