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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.petrus.lib.core.result.ProcessProgressListener;

/**
 * A utility class for easily copying data from and to streams.
 *
 * @author Pierre Sagne
 * @since 25.01.2015
 */
public class StreamUtils {

    /**
     * Copies data from the given {@code inputStream} to the given {@code outputStream}, using a buffer
     * which size is {@code bufferSize}, reporting the progress and being controlled by the given
     * {@code listener}.
     *
     * @param outputStream the {@code OutputStream} to write the data to
     * @param inputStream  the {@code InputStream} to read the data from
     * @param bufferSize   the buffer size
     * @param listener     the listener to report and control the progress
     * @return the number of bytes that have been copied, or -1 if the given {@code inputStream} or
     *         {@code outputStream} is null or if the {@code bufferSize} is smaller than 1.
     * @throws IOException if an error occurs when reading or writing data
     */
    public static int copy(OutputStream outputStream, InputStream inputStream, int bufferSize,
                           ProcessProgressListener listener)
            throws IOException {
        if (null==inputStream || null==outputStream || bufferSize<1) {
            return -1;
        }
        byte[] buffer = new byte[bufferSize];
        int totalLength = 0;
        int bytesRead;
        while ((bytesRead=inputStream.read(buffer))!=-1) {
            if (null!=listener) {
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    return totalLength;
                }
            }
            totalLength += bytesRead;
            outputStream.write(buffer, 0, bytesRead);
            if (null!=listener) {
                listener.onProgress(0, totalLength);
            }
        }
        outputStream.flush();
        return totalLength;
    }

    /**
     * Reads no more than {@code maxReadBytes} bytes of data from the given {@code inputStream},
     * using a buffer which size is {@code bufferSize}, reporting the progress and being controlled
     * by the given {@code listener}.
     *
     * @param inputStream  the {@code InputStream} to read the data from
     * @param bufferSize   the buffer size
     * @param maxReadBytes the maximum number of bytes to read
     * @param listener     the listener to report and control the progress
     * @return the read data, or null if the given {@code inputStream} is null or if the
     *         {@code bufferSize} or {@code maxReadBytes} are smaller than 1.
     * @throws IOException if an error occurs when reading data
     */
    public static byte[] read(InputStream inputStream, int bufferSize, int maxReadBytes,
                              ProcessProgressListener listener)
            throws IOException {
        if (null==inputStream || bufferSize<1 || maxReadBytes<1) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        int totalLength = 0;
        int bytesRead;
        while ((bytesRead=inputStream.read(buffer))!=-1) {
            if (null!=listener) {
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    return null;
                }
            }
            totalLength += bytesRead;
            byteArrayOutputStream.write(buffer, 0, bytesRead);
            if (null!=listener) {
                listener.onProgress(0, totalLength);
            }
            if (totalLength+bufferSize>=maxReadBytes) {
                break;
            }
        }
        if (byteArrayOutputStream.size()<=0) {
            return null;
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Reads data from the given {@code inputStream}, using a buffer which size is {@code bufferSize},
     * reporting the progress and being controlled by the given {@code listener}.
     *
     * @param inputStream  the {@code InputStream} to read the data from
     * @param bufferSize   the buffer size
     * @param listener     the listener to report and control the progress
     * @return the read data, or null if the given {@code inputStream} is null or if the
     *         {@code bufferSize} is smaller than 1.
     * @throws IOException if an error occurs when reading data
     */
    public static byte[] read(InputStream inputStream, int bufferSize, ProcessProgressListener listener)
            throws IOException {
        if (null==inputStream || bufferSize<1) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead=inputStream.read(buffer))!=-1) {
            if (null!=listener) {
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    return null;
                }
            }
            byteArrayOutputStream.write(buffer, 0, bytesRead);
            if (null!=listener) {
                listener.onProgress(0, byteArrayOutputStream.size());
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Writes the given {@code data} to the given {@code outputStream}, using a buffer which size is
     * {@code bufferSize}, reporting the progress and being controlled by the given {@code listener}.
     *
     * @param outputStream  the {@code OutputStream} to write the data to
     * @param bufferSize   the buffer size
     * @param data         the data to write
     * @param listener     the listener to report and control the progress
     * @return true if all the given {@code data} was written
     * @throws IOException if an error occurs when reading data
     */
    public static boolean write(OutputStream outputStream, byte[] data, int bufferSize, ProcessProgressListener listener)
            throws IOException {
        if (null==outputStream || null==data || bufferSize<1) {
            return false;
        }
        for(int offset=0; offset<data.length; offset+=bufferSize) {
            if (null!=listener) {
                listener.onProgress(0, offset);
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    return false;
                }
            }
            int remaining = data.length - offset;
            int bytesToWrite = bufferSize<remaining?bufferSize:remaining;
            outputStream.write(data, offset, bytesToWrite);
        }
        outputStream.flush();
        return true;
    }
}
