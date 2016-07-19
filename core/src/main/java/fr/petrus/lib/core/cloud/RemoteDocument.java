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

package fr.petrus.lib.core.cloud;

import java.io.File;
import java.util.List;

import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.result.ProcessProgressListener;

/**
 * This interface is used to access remote documents, stored in the cloud.
 *
 * @param <S> the {@link RemoteStorage} implementation
 * @param <D> the {@link RemoteDocument} implementation
 * @author Pierre Sagne
 * @since 12.03.2016
 */
public interface RemoteDocument<S extends RemoteStorage<S, D>, D extends RemoteDocument<S, D>> {
    /**
     * Returns the StorageType of this document.
     *
     * @return the StorageType of this document
     */
    StorageType getStorageType();

    /**
     * Returns an instance of the {@link RemoteStorage} where this document is stored.
     *
     * @return the {@link RemoteStorage} instance where this document is stored
     */
    S getStorage();

    /**
     * Sets the user name of the account where this document is stored.
     *
     * @param accountName the user name of the account where this document is stored
     */
    void setAccountName(String accountName);

    /**
     * Sets the name of this document.
     *
     * @param name the name of this document
     */
    void setName(String name);

    /**
     * Sets the size (in bytes) of this document.
     *
     * @param size the size (in bytes) of this document
     */
    void setSize(long size);

    /**
     * Sets whether this document is a folder.
     *
     * @param folder true if this document is a folder, false if it is a file
     */
    void setFolder(boolean folder);

    /**
     * Sets the remote version of this document.
     *
     * @param version the remote version
     */
    void setVersion(long version);

    /**
     * Sets the remote modification time (in ms from the epoch).
     *
     * @param modificationTime the remote modification time (in ms from the epoch)
     */
    void setModificationTime(long modificationTime);

    /**
     * Returns the user name of the account where this document is stored.
     *
     * @return the user name of the account where this document is stored
     */
    String getAccountName();

    /**
     * Returns the name of this document.
     *
     * @return the name of this document
     */
    String getName();

    /**
     * Returns the remote id of this document.
     *
     * @return the remote id of this document
     */
    String getId();

    /**
     * Returns the remote id of the parent folder.
     *
     * @return the remote id of the parent folder
     */
    String getParentId();

    /**
     * Returns the size (in bytes) of this document.
     *
     * @return the size (in bytes) of this document
     */
    long getSize();

    /**
     * Returns whether this document is a folder.
     *
     * @return true if this is a folder, false if it is a file
     */
    boolean isFolder();

    /**
     * Returns the remote version of this document.
     *
     * @return the remote version of this document
     */
    long getVersion();

    /**
     * Returns the remote modification time (in ms from the epoch).
     *
     * @return the remote modification time (in ms from the epoch)
     */
    long getModificationTime();

    /**
     * Returns the remote changes for this this document and its children (if it is a folder), recursively.
     *
     * @param changes  a {@link RemoteChanges} instance, where the results are added
     * @param listener a listener which allows to track the progress, and cancel/pause it.
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void getRecursiveChanges(RemoteChanges changes, ProcessProgressListener listener) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns the children of this document, if it is a folder, or an empty list if it is a file.
     *
     * @param documents a list of documents where the results are added
     * @param listener  a listener which allows to track the progress, and cancel/pause it
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void getRecursiveChildren(List<D> documents, ProcessProgressListener listener) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns a child file of this document, with the specified name.
     *
     * @param name the name of the child file
     * @return the remote file if one was found, null otherwise
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D childFile(String name) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns a child folder of this document, with the specified name.
     *
     * @param name the name of the child folder
     * @return the remote folder if one was found, null otherwise
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D childFolder(String name) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns a child document of this document, with the specified name.
     *
     * @param name the name of the child document
     * @return the remote document if one was found, null otherwise
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D childDocument(String name) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns all the children documents of this document.
     *
     * @param listener  a listener which allows to track the progress, and cancel/pause it
     * @return the list of children.
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    List<D> childDocuments(ProcessProgressListener listener) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Creates a folder as a child of this document.
     *
     * @param name the name of the new folder
     * @return the newly created remote folder
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D createChildFolder(String name) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Creates a folder as a child of this document,
     * adding its associated metadata file as a child of the newly created folder.
     *
     * @param name     the name of the new folder
     * @param metadata the contents of the metadata file
     * @return the newly created remote folder
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D createChildFolderWithMetadata(String name, byte[] metadata) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Creates a file as a child of this document.
     *
     * @param name the name of the new file
     * @param mimeType the mime type of the new file
     * @return the newly created remote file
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D createChildFile(String name, String mimeType) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Creates a file as a child of this document, and uploads its contents.
     *
     * @param name the name of the new file
     * @param mimeType the mime type of the new file
     * @param localFile the local file which contents will be uploaded
     * @param listener  a listener which allows to track the progress, and cancel/pause it
     * @return the newly created remote file
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D uploadNewChildFile(String name, String mimeType, File localFile, ProcessProgressListener listener)
            throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Creates a file as a child of this document, and uploads its contents.
     *
     * @param name the name of the new file
     * @param mimeType the mime type of the new file
     * @param fileName the file name (may be used for caching the transfers on the server side)
     * @param data     the binary contents of the file
     * @return the newly created remote file
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D uploadNewChildData(String name, String mimeType, String fileName, byte[] data)
            throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Uploads the contents of this file (the remote file must already exist).
     *
     * @param mimeType the mime type of the new file
     * @param localFile the local file which contents will be uploaded
     * @param listener  a listener which allows to track the progress, and cancel/pause it
     * @return the updated remote file
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D uploadFile(String mimeType, File localFile, ProcessProgressListener listener)
            throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Uploads the contents of this file (the remote file must already exist).
     *
     * @param mimeType the mime type of the new file
     * @param fileName the file name (may be used for caching the transfers on the server side)
     * @param data     the binary contents of the file
     * @return the updated remote file
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D uploadData(String mimeType, String fileName, byte[] data)
            throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Downloads the contents of this remote file, and stores it to the provided {@code localFile}.
     *
     * @param localFile the local file where the downloaded contents will be downloaded
     * @param listener  a listener which allows to track the progress, and cancel/pause it
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void downloadFile(File localFile, ProcessProgressListener listener)
            throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Downloads the contents of this remote file and returns it as a byte array.
     *
     * @return the downloaded binary contents
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    byte[] downloadData() throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Sends a request to delete this remote document (file or folder).
     *
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void delete() throws RemoteException, DatabaseConnectionClosedException;
}
