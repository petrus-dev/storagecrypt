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

package fr.petrus.tools.storagecrypt.android.platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.filesystem.AbstractFileSystem;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.tools.storagecrypt.R;

/**
 * The {@link FileSystem} implementation for the "Desktop" platform.
 *
 * @author Pierre Sagne
 * @since 24.08.2015
 */
public class AndroidFileSystem extends AbstractFileSystem {

    private Context context;

    /**
     * Creates a new {@code AndroidFileSystem} instance.
     *
     * @param context the Android context
     */
    AndroidFileSystem(Context context) {
        this.context = context;
    }

    /**
     * Returns the internal storage directory where the files of the application are stored.
     *
     * @param context the Android context
     * @return the internal storage directory where the files of the application are stored
     */
    public static File getInternalStorageDir(Context context) {
        return context.getFilesDir();
    }

    /**
     * Returns the path of the internal storage directory where the files of the application are stored.
     *
     * @param context the Android context
     * @return the path of the internal storage directory where the files of the application are stored
     */
    public static String getInternalStoragePath(Context context) {
        try {
            return getInternalStorageDir(context).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the external storage directory (SD card).
     *
     * @return the external storage directory (SD card)
     */
    public static File getExternalStorageDir() {
        return Environment.getExternalStorageDirectory();
    }

    /**
     * Returns the path of the external storage directory (SD card).
     *
     * @return the path of the external storage directory (SD card)
     */
    public static String getExternalStoragePath() {
        try {
            return getExternalStorageDir().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public File getAppDir() {
        File appExternalDir = new File(getExternalStorageDir(), Constants.FILE.APP_DIR_NAME);
        if (!appExternalDir.exists()) {
            appExternalDir.mkdirs();
        }
        return appExternalDir;
    }

    /**
     * {@inheritDoc}
     * <p>This implementation uses the native Android {@link MimeTypeMap} class to determine the
     * file MIME type
     */
    @Override
    public String getMimeType(String url) {
        return getMimeType(context, url);
    }

    /**
     * {@inheritDoc}
     * <p>This implementation uses the native Android {@link MimeTypeMap} class to determine the
     * file MIME type
     */
    @Override
    public String getMimeType(File file) {
        return getMimeType(file.toURI().toString());
    }

    @Override
    public File getCacheFilesDir() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefsCacheDir = prefs.getString(context.getString(R.string.pref_key_cache_dir), null);
        if (context.getString(R.string.pref_entryvalue_cache_dir_internal_cache).equals(prefsCacheDir)) {
            return context.getCacheDir();
        } else if (context.getString(R.string.pref_entryvalue_cache_dir_external_cache).equals(prefsCacheDir)) {
            return context.getExternalCacheDir();
        } else if (context.getString(R.string.pref_entryvalue_cache_dir_external_tmp_dir).equals(prefsCacheDir)) {
            return getTempFilesDir();
        }
        return context.getCacheDir();
    }

    @Override
    public void removeCacheFiles() {
        deleteFolder(getTempFilesDir());
        deleteFolderContent(context.getCacheDir());
        deleteFolderContent(context.getExternalCacheDir());
    }

    /**
     * Returns the mime type of the document at the given {@code url}.
     *
     * @param context the Android context
     * @param url the url of the document
     * @return the mime type of the document
     */
    public static String getMimeType(Context context, String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        if (null==type) {
            type = Constants.STORAGE.DEFAULT_BINARY_MIME_TYPE;
        }
        return type;
    }
}
