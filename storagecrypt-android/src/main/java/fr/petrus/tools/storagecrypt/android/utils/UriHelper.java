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

package fr.petrus.tools.storagecrypt.android.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to access files from Android {@code Uri}s.
 *
 * @author Pierre Sagne
 * @since 30.12.2014
 */
public class UriHelper {
    private static final String TAG = "UriHelper";

    private Context context;
    private Uri uri;
    private String displayName;
    private int size;
    private Map<String, String> properties;

    /**
     * Creates a new {@code UriHelper} instance for the given {@code uri}.
     *
     * @param context the Android context
     * @param uri     the URI of the file to access
     */
    public UriHelper(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null);

        try {
            if (null != cursor && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                Log.d(TAG, "Uri file display name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    size = cursor.getInt(sizeIndex);
                    Log.d(TAG, "Uri file size : " + size);
                } else {
                    size = -1;
                    Log.d(TAG, "Uri file size unknown");
                }

                int nbColumns = cursor.getColumnCount();
                if (nbColumns > 0) {
                    properties = new HashMap<>();
                    for (int i = 0; i < nbColumns; i++) {
                        properties.put(cursor.getColumnName(i), cursor.getString(i));
                        Log.d(TAG, "Uri file property " + cursor.getColumnName(i) + " = " + cursor.getString(i));
                    }
                } else {
                    properties = null;
                }
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    /**
     * Returns the display name of the file targeted by the {@code Uri}.
     *
     * @return the display name of the file targeted by the {@code Uri}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the size of the file targeted by the {@code Uri}.
     *
     * @return the size of the file targeted by the {@code Uri}
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the MIME type of the file targeted by the {@code Uri}.
     *
     * @return the MIME type of the file targeted by the {@code Uri}
     */
    public String getMimeType() {
        return getProperty("mime_type");
    }

    /**
     * Returns the property with the given {@code name} for the file targeted by the {@code Uri}.
     *
     * @param name the name of the property to return.
     * @return the property with the given {@code name} for the file targeted by the {@code Uri}
     */
    public String getProperty(String name) {
        if (null==properties) {
            return null;
        }
        return properties.get(name);
    }

    /**
     * Deletes the file targeted by the {@code Uri}.
     */
    public void delete() {
        try {
            DocumentsContract.deleteDocument(context.getContentResolver(), uri);
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "Cannot delete file \""+displayName+"\"", e);
        }
    }

    /**
     * Opens an {@code InputStream} for reading the file targeted by the {@code Uri}.
     *
     * @param buffered if true, the {@code InputStream} will be buffered
     * @return the {@code InputStream} for reading the file targeted by the {@code Uri}
     * @throws FileNotFoundException if the file was not found at the given {@code Uri}
     */
    public InputStream openInputStream(boolean buffered) throws FileNotFoundException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (buffered && null!=inputStream) {
            return new BufferedInputStream(inputStream);
        }
        return inputStream;
    }

    /**
     * Opens an {@code OutputStream} for writing to the file targeted by the {@code Uri}.
     *
     * @param buffered if true, the {@code OutputStream} will be buffered
     * @return the {@code OutputStream} for writing to the file targeted by the {@code Uri}
     * @throws FileNotFoundException if the file was not found at the given {@code Uri}
     */
    public OutputStream openOutputStream(boolean buffered) throws FileNotFoundException {
        OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
        if (buffered && null!=outputStream) {
            return new BufferedOutputStream(outputStream);
        }
        return outputStream;
    }
}
