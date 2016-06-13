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

package fr.petrus.lib.core.filesystem;

import java.io.File;
import java.util.List;

/**
 * This interface provides system independant access to the filesystem
 *
 * @author Pierre Sagne
 * @since 24.08.2015.
 */
public interface FileSystem {

    /**
     * Returns the folder where the application stores its data.
     *
     * @return the folder where the application stores its data
     */
    File getAppDir();

    /**
     * Returns the path of the folder where the application stores its data.
     *
     * @return the path of the folder where the application stores its data
     */
    String getAppDirPath();

    /**
     * Creates the folder where the application stores its data.
     *
     * @return true if the folder was successfully created
     */
    boolean createAppDir();

    /**
     * Returns the folder where the application stores the temporary files.
     *
     * @return the folder where the application stores the temporary files
     */
    File getTempFilesDir();

    /**
     * Returns the path of the folder where the application stores the temporary files.
     *
     * @return the path of the folder where the application stores the temporary files
     */
    File getCacheFilesDir();

    /**
     * Removes all the cache files created by the application.
     */
    void removeCacheFiles();

    /**
     * Returns the folder where the application stores the encrypted files which are not synchronized.
     *
     * @return the folder where the application stores the encrypted files which are not synchronized
     */
    File getLocalFilesDir();

    /**
     * Returns the folder where the application stores the encrypted files which are synchronized
     * with the Google Drive account registered with the given {@code accountName}.
     *
     * @param accountName the account user name
     * @return the folder where the application stores the encrypted files which are synchronized
     * with the Google Drive account registered with the given {@code accountName}
     */
    File getGDriveFilesDir(String accountName);

    /**
     * Returns the folder where the application stores the encrypted files which are synchronized
     * with the Dropbox account registered with the given {@code accountName}.
     *
     * @param accountName the account user name
     * @return the folder where the application stores the encrypted files which are synchronized
     * with the Dropbox account registered with the given {@code accountName}
     */
    File getDropboxFilesDir(String accountName);

    /**
     * Returns the folder where the application stores the encrypted files which are synchronized
     * with the Box.com account registered with the given {@code accountName}.
     *
     * @param accountName the account user name
     * @return the folder where the application stores the encrypted files which are synchronized
     * with the Box.com account registered with the given {@code accountName}
     */
    File getBoxFilesDir(String accountName);

    /**
     * Returns the folder where the application stores the encrypted files which are synchronized
     * with the HubiC account registered with the given {@code accountName}.
     *
     * @param accountName the account user name
     * @return the folder where the application stores the encrypted files which are synchronized
     * with the HubiC account registered with the given {@code accountName}
     */
    File getHubicFilesDir(String accountName);

    /**
     * Returns the folder where the application stores the encrypted files which are synchronized
     * with the OneDrive account registered with the given {@code accountName}.
     *
     * @param accountName the account user name
     * @return the folder where the application stores the encrypted files which are synchronized
     * with the OneDrive account registered with the given {@code accountName}
     */
    File getOneDriveFilesDir(String accountName);

    /**
     * Returns a list of file paths, built by recursively listing all the folder contents.
     *
     * @param document the root document (usually a folder)
     * @return the list of the paths of all the children of the given {@code document}, including
     *         the given {@code document} itself
     */
    List<String> getRecursiveDocumentsList(String document);

    /**
     * Filters a list of document paths, only returning the paths which represent folders.
     *
     * @param documents the documents list to filter
     * @return the filtered list containing only the paths which represent folders
     */
    List<String> getFoldersList(List<String> documents);

    /**
     * Filters a list of document paths, only returning the paths which represent files.
     *
     * @param documents the documents list to filter
     * @return the filtered list containing only the paths which represent files
     */
    List<String> getFilesList(List<String> documents);

    /**
     * Recursively deletes the all the contents of the given {@code folder}.
     *
     * <p>The given {@code folder} itself will not be deleted.
     *
     * @param folder the folder to delete the contents of
     */
    void deleteFolderContent(File folder);

    /**
     * Deletes the given {@code folder} and all his contents.
     *
     * @param folder the folder to delete
     */
    void deleteFolder(File folder);

    /**
     * Returns the mime type of the document at the given {@code url}.
     *
     * @param url the url of the document
     * @return the mime type of the document
     */
    String getMimeType(String url);

    /**
     * Returns the mime type of the given {@code file}.
     *
     * @param file the file
     * @return the mime type of the file
     */
    String getMimeType(File file);
}
