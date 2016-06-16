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

package fr.petrus.lib.core.utils;

import java.nio.ByteBuffer;

/**
 * A utility class for easily converting an integer to and from a byte array.
 *
 * @author Pierre Sagne
 * @since 25.01.2015
 */
public class NumberConv {
    /**
     * Takes an integer value and converts it to a byte array.
     *
     * @param i the integer to convert
     * @return the byte array
     */
    public static byte[] intToByteArray(int i) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(i);
        return byteBuffer.array();
    }

    /**
     * Takes a byte array and converts the 4 bytes located at the given {@code offset} into an integer.
     *
     * @param b      the byte array
     * @param offset the offset where to extract the integer from
     * @return the integer
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.put(b, offset, 4);
        return byteBuffer.getInt(0);
    }

    /**
     * Takes a short integer value and converts it to a byte array.
     *
     * @param i the short integer to convert
     * @return the byte array
     */
    public static byte[] shortToByteArray(short i) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.putShort(i);
        return byteBuffer.array();
    }

    /**
     * Takes a byte array and converts the 2 bytes located at the given {@code offset} into a short integer.
     *
     * @param b      the byte array
     * @param offset the offset where to extract the short integer from
     * @return the short integer
     */
    public static short byteArrayToShort(byte[] b, int offset) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.put(b, offset, 2);
        return byteBuffer.getShort(0);
    }
}
