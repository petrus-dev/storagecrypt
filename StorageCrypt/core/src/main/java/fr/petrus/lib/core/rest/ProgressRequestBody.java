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

package fr.petrus.lib.core.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.result.ProcessProgressListener;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * This class extends Retrofit2 {@link RequestBody}, making possible to monitor upload progress
 *
 * @author Pierre Sagne
 * @since 28.02.2016
 */
public class ProgressRequestBody extends RequestBody {
    private String mimeType;
    private File file;
    private ProcessProgressListener listener;

    private static final int DEFAULT_BUFFER_SIZE = 2048;

    /**
     * Creates a new {@code ProgressRequestBody} instance.
     *
     * @param mimeType the mime type in the data to upload
     * @param file     the file to upload
     * @param listener the listener to report the upload progress to
     */
    public ProgressRequestBody(String mimeType, File file, ProcessProgressListener listener) {
        this.mimeType = mimeType;
        this.file = file;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(mimeType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long uploaded = 0;
        long uploadedSinceLastProgressUpdate = 0;

        if (null!=listener) {
            listener.onSetMax(0, (int)fileLength);
            if (listener.isCanceled()) {
                throw new IOException("Canceled");
            }
            listener.pauseIfNeeded();
        }

        try (FileInputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                uploaded += read;
                if (null!=listener) {
                    uploadedSinceLastProgressUpdate += read;
                    if (uploadedSinceLastProgressUpdate >= Constants.STORAGE.CLOUD_SYNC_PROGRESS_UPDATE_DELTA) {
                        uploadedSinceLastProgressUpdate %= Constants.STORAGE.CLOUD_SYNC_PROGRESS_UPDATE_DELTA;
                        listener.onProgress(0, (int) uploaded);
                        if (listener.isCanceled()) {
                            throw new IOException("Canceled");
                        }
                        listener.pauseIfNeeded();
                    }
                }
                sink.write(buffer, 0, read);
            }
        }
    }
}