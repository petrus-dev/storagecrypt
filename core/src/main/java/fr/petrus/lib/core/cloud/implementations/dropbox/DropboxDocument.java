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

package fr.petrus.lib.core.cloud.implementations.dropbox;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.AbstractRemoteDocument;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.rest.ProgressRequestBody;
import fr.petrus.lib.core.rest.models.dropbox.DropboxFileMetadata;
import fr.petrus.lib.core.rest.models.dropbox.DropboxFolderMetadata;
import fr.petrus.lib.core.rest.models.dropbox.DropboxFolderResult;
import fr.petrus.lib.core.rest.models.dropbox.DropboxMetadata;
import fr.petrus.lib.core.rest.models.dropbox.GetMetadataArg;
import fr.petrus.lib.core.rest.models.dropbox.ListFolderArg;
import fr.petrus.lib.core.rest.models.dropbox.ListFolderContinueArg;
import fr.petrus.lib.core.rest.models.dropbox.PathArg;
import fr.petrus.lib.core.rest.models.dropbox.UploadCommitArg;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.utils.StreamUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The {@link RemoteDocument} implementation for Dropbox.
 *
 * @author Pierre Sagne
 * @since 18.02.2015
 */
public class DropboxDocument extends AbstractRemoteDocument<DropboxStorage, DropboxDocument> {
    private static Logger LOG = LoggerFactory.getLogger(DropboxDocument.class);

    private String id;
    private String path;
    private String parentId;

    /**
     * Creates a new empty DropboxDocument.
     *
     * @param dropboxStorage a {@code DropboxStorage} instance
     */
    public DropboxDocument(DropboxStorage dropboxStorage) {
        super(dropboxStorage);
        id = null;
        path = null;
        parentId = null;
    }

    /**
     * Creates a DropboxDocument from the result of an API request.
     *
     * @param dropboxStorage a {@code DropboxStorage} instance
     * @param accountName    the account user name
     * @param metadata       the {@code DropboxMetadata} which represents this document
     */
    DropboxDocument(DropboxStorage dropboxStorage, String accountName, DropboxMetadata metadata) {
        this(dropboxStorage);
        setAccountName(accountName);
        setId(metadata.id);
        setName(metadata.name);
        setPath(rebuildPath(metadata.path_lower, metadata.name));
        setFolder(null!=metadata.tag && metadata.tag.equals("folder"));
        if (null != metadata.rev) {
            setVersion(Long.parseLong(metadata.rev, 16));
        }
        if (null!=metadata.size) {
            setSize(metadata.size);
        }
        if (null!=metadata.server_modified) {
            try {
                DateTime dateTime = new DateTime(metadata.server_modified);
                //DateTimeFormatter formatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z").withLocale(Locale.ENGLISH);
                //DateTime dateTime = formatter.parseDateTime(metadata.server_modified);
                setModificationTime(dateTime.getMillis());
            } catch (Exception e) {
                LOG.error("Error while parsing date", e);
            }
        }
    }

    /**
     * Creates a DropboxDocument from the result of an API request.
     *
     * @param dropboxStorage a {@code DropboxStorage} instance
     * @param accountName    the account user name
     * @param metadata       the {@code DropboxFileMetadata} which represents this file
     */
    DropboxDocument(DropboxStorage dropboxStorage, String accountName, DropboxFileMetadata metadata) {
        this(dropboxStorage);
        setAccountName(accountName);
        setId(metadata.id);
        setName(metadata.name);
        setPath(rebuildPath(metadata.path_lower, metadata.name));
        setFolder(false);
        if (null != metadata.rev) {
            setVersion(Long.parseLong(metadata.rev, 16));
        }
        if (null!=metadata.size) {
            setSize(metadata.size);
        }
        if (null!=metadata.server_modified) {
            try {
                DateTime dateTime = new DateTime(metadata.server_modified);
                //DateTimeFormatter formatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z").withLocale(Locale.ENGLISH);
                //DateTime dateTime = formatter.parseDateTime(metadata.server_modified);
                setModificationTime(dateTime.getMillis());
            } catch (Exception e) {
                LOG.error("Error while parsing date", e);
            }
        }
    }

    /**
     * Creates a DropboxDocument from the result of an API request.
     *
     * @param dropboxStorage a {@code DropboxStorage} instance
     * @param accountName    the account user name
     * @param metadata       the {@code DropboxFolderMetadata} which represents this folder
     */
    DropboxDocument(DropboxStorage dropboxStorage, String accountName, DropboxFolderMetadata metadata) {
        this(dropboxStorage);
        setAccountName(accountName);
        setId(metadata.id);
        setPath(rebuildPath(metadata.path_lower));
        setName(metadata.name);
        setFolder(true);
    }

    /**
     * Rebuilds the path, by replacing the app folder name (which is case sensitive)
     * in the lower case path.
     *
     * @param pathLower the lower case path
     * @return the rebuilt path, with corrected case
     */
    private static String rebuildPath(String pathLower) {
        if (null==pathLower) {
            return null;
        }
        return pathLower.replaceFirst(Constants.FILE.APP_DIR_NAME.toLowerCase(), Constants.FILE.APP_DIR_NAME);
    }

    /**
     * Rebuilds the path, by replacing the app folder name and the file name
     * (which are case sensitive) in the lower case path.
     *
     * @param pathLower the lower case path
     * @param name the document name
     * @return the rebuilt path, with corrected case
     */
    private static String rebuildPath(String pathLower, String name) {
        return DropboxDocument.getChildPath(DropboxDocument.getParentPath(rebuildPath(pathLower)), name);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.Dropbox;
    }

    /**
     * Sets the remote id of this document.
     *
     * @param id the remote id of this document
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the path of this document.
     *
     * @param path the path of this document
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Sets the remote id of the parent folder of this document.
     *
     * @param parentId the remote id of the parent folder of this document
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * Returns the path of this document.
     *
     * @return the path of this document
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the search path of a child of this document, for searching with API.
     *
     * <p>It is built with the id of this document and the name of the child
     *
     * @param childName the name of the child document
     * @return the search path of the child document
     */
    public String getSearchPath(String childName) {
        return id+"/"+childName;
    }

    /**
     * Returns the path of a child of this document.
     *
     * @param childName the name of the child document
     * @return the path of the child document
     */
    public String getChildPath(String childName) {
        return DropboxDocument.getChildPath(path, childName);
    }

    /**
     * Returns the path of the parent folder of this document.
     *
     * @return the path of the parent folder of this document
     */
    public String getParentPath() {
        return DropboxDocument.getParentPath(path);
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public DropboxDocument childFile(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        return storage.file(getAccountName(), getSearchPath(name));
    }

    @Override
    public DropboxDocument childFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        return storage.folder(getAccountName(), getSearchPath(name));
    }

    @Override
    public DropboxDocument childDocument(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        return storage.document(getAccountName(), getSearchPath(name));
    }

    @Override
    public List<DropboxDocument> childDocuments(ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<DropboxFolderResult> response = storage.getApiService().listFolder(account.getAuthHeader(),
                    new ListFolderArg(getPath())).execute();
            if (response.isSuccess()) {
                List<DropboxDocument> children = new ArrayList<>();
                DropboxFolderResult dropboxFolderResult;
                do {
                    dropboxFolderResult = response.body();
                    if (null != dropboxFolderResult.entries) {
                        if (null != listener) {
                            listener.onSetMax(0, children.size() + dropboxFolderResult.entries.size());
                            listener.pauseIfNeeded();
                            if (listener.isCanceled()) {
                                throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                            }
                        }
                        for (DropboxMetadata childMetadata : dropboxFolderResult.entries) {
                            DropboxDocument child = new DropboxDocument(storage, getAccountName(), childMetadata);
                            child.setParentId(getId());
                            children.add(child);
                            if (null != listener) {
                                listener.onProgress(0, children.size());
                                listener.pauseIfNeeded();
                                if (listener.isCanceled()) {
                                    throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                                }
                            }
                        }
                    }
                    if (dropboxFolderResult.has_more) {
                        response = storage.getApiService().listFolderContinue(account.getAuthHeader(),
                                new ListFolderContinueArg(dropboxFolderResult.cursor)).execute();
                        if (response.isSuccess()) {
                            dropboxFolderResult = response.body();
                        } else {
                            throw storage.remoteException(account, response, "Failed to get child documents");
                        }
                    } else {
                        dropboxFolderResult = null;
                    }
                } while (null!=dropboxFolderResult);

                return children;
            } else {
                throw storage.remoteException(account, response, "Failed to get child documents");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get child documents", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public DropboxDocument createChildFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<DropboxFolderMetadata> response = storage.getApiService().createFolder(account.getAuthHeader(),
                    new PathArg(getChildPath(name))).execute();
            if (response.isSuccess()) {
                DropboxDocument document = new DropboxDocument(storage, getAccountName(), response.body());
                String parentPath = document.getParentPath();
                if (null != parentPath && !parentPath.isEmpty() && !parentPath.equals("/")) {
                    Response<DropboxMetadata> parentMetadataResponse = storage.getApiService().getMetadata(
                            account.getAuthHeader(),
                            new GetMetadataArg(document.getParentPath())).execute();
                    if (parentMetadataResponse.isSuccess()) {
                        document.setParentId(parentMetadataResponse.body().id);
                    } else {
                        throw storage.remoteException(account, response, "Failed to create folder");
                    }
                }
                return document;
            } else {
                throw storage.remoteException(account, response, "Failed to create folder");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to create folder", RemoteException.Reason.NetworkError);
        }
    }

    @Override
    public DropboxDocument createChildFile(String name, String mimeType)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<DropboxFileMetadata> response = storage.getContentApiService().uploadFile(account.getAuthHeader(),
                    new UploadCommitArg(getChildPath(name)),
                    RequestBody.create(MediaType.parse(mimeType), new byte[0])).execute();
            if (response.isSuccess()) {
                DropboxDocument document = new DropboxDocument(storage, getAccountName(), response.body());
                String parentPath = document.getParentPath();
                if (null != parentPath && !parentPath.isEmpty() && !parentPath.equals("/")) {
                    Response<DropboxMetadata> parentMetadataResponse = storage.getApiService().getMetadata(
                            account.getAuthHeader(),
                            new GetMetadataArg(document.getParentPath())).execute();
                    if (parentMetadataResponse.isSuccess()) {
                        document.setParentId(parentMetadataResponse.body().id);
                    } else {
                        throw storage.remoteException(account, response, "Failed to create file");
                    }
                }
                return document;
            } else {
                throw storage.remoteException(account, response, "Failed to create file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to create file", RemoteException.Reason.NetworkError);
        }
    }

    @Override
    public DropboxDocument uploadNewChildFile(String name, String mimeType, File localFile,
                                             ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<DropboxFileMetadata> response = storage.getContentApiService().uploadFile(
                    account.getAuthHeader(),
                    new UploadCommitArg(getChildPath(name)),
                    new ProgressRequestBody(mimeType, localFile, listener)).execute();
            if (response.isSuccess()) {
                DropboxDocument document = new DropboxDocument(storage, getAccountName(), response.body());
                String parentPath = document.getParentPath();
                if (null != parentPath && !parentPath.isEmpty() && !parentPath.equals("/")) {
                    Response<DropboxMetadata> parentMetadataResponse = storage.getApiService().getMetadata(
                            account.getAuthHeader(),
                            new GetMetadataArg(document.getParentPath())).execute();
                    if (parentMetadataResponse.isSuccess()) {
                        document.setParentId(parentMetadataResponse.body().id);
                    } else {
                        throw storage.remoteException(account, response, "Failed to upload new file");
                    }
                }
                return document;
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload new file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public DropboxDocument uploadNewChildData(String name, String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<DropboxFileMetadata> response = storage.getContentApiService().uploadFile(
                    account.getAuthHeader(),
                    new UploadCommitArg(getChildPath(name)),
                    RequestBody.create(MediaType.parse(mimeType), data)).execute();
            if (response.isSuccess()) {
                DropboxDocument document = new DropboxDocument(storage, getAccountName(), response.body());
                String parentPath = document.getParentPath();
                if (null != parentPath && !parentPath.isEmpty() && !parentPath.equals("/")) {
                    Response<DropboxMetadata> parentMetadataResponse = storage.getApiService().getMetadata(
                            account.getAuthHeader(),
                            new GetMetadataArg(document.getParentPath())).execute();
                    if (parentMetadataResponse.isSuccess()) {
                        document.setParentId(parentMetadataResponse.body().id);
                    } else {
                        throw storage.remoteException(account, response, "Failed to upload new file data");
                    }
                }
                return document;
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file data");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload new file data", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public DropboxDocument uploadFile(String mimeType, File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<DropboxFileMetadata> response = storage.getContentApiService().uploadFile(
                    account.getAuthHeader(),
                    new UploadCommitArg(getPath()),
                    new ProgressRequestBody(mimeType, localFile, listener)).execute();
            if (response.isSuccess()) {
                DropboxDocument document = new DropboxDocument(storage, getAccountName(), response.body());
                document.setParentId(getParentId());
                return document;
            } else {
                throw storage.remoteException(account, response, "Failed to upload file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public DropboxDocument uploadData(String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<DropboxFileMetadata> response = storage.getContentApiService().uploadFile(
                    account.getAuthHeader(),
                    new UploadCommitArg(getPath()),
                    RequestBody.create(MediaType.parse(mimeType), data)).execute();
            if (response.isSuccess()) {
                DropboxDocument document = new DropboxDocument(storage, getAccountName(), response.body());
                document.setParentId(getParentId());
                return document;
            } else {
                throw storage.remoteException(account, response, "Failed to upload file data");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload file data", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public void downloadFile(File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<ResponseBody> response = storage.getContentApiService().downloadFile(
                    account.getAuthHeader(),
                    new PathArg(getId())).execute();
            if (response.isSuccess()) {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    if (!localFile.exists()) {
                        localFile.createNewFile();
                    }
                    inputStream = new BufferedInputStream(response.body().byteStream());
                    outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                    StreamUtils.copy(outputStream, inputStream, Constants.FILE.BUFFER_SIZE, listener);
                } catch (IOException e) {
                    throw new RemoteException("Failed to download file", RemoteException.Reason.UnknownError, e);
                } finally {
                    if (null!=inputStream) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            LOG.error("Error when closing input stream", e);
                        }
                    }
                    if (null!=outputStream) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            LOG.error("Error when closing output stream", e);
                        }
                    }
                }
            } else {
                throw storage.remoteException(account, response, "Failed to download file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to download file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public byte[] downloadData() throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            LOG.debug("downloading file with id : "+getId());
            Response<ResponseBody> response = storage.getContentApiService().downloadFile(
                    account.getAuthHeader(),
                    new PathArg(getId())).execute();
            if (response.isSuccess()) {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    inputStream = new BufferedInputStream(response.body().byteStream());
                    StreamUtils.copy(outputStream, inputStream, Constants.FILE.BUFFER_SIZE, null);
                } catch (IOException e) {
                    throw new RemoteException("Failed to download file data", RemoteException.Reason.UnknownError, e);
                } finally {
                    if (null!=inputStream) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            LOG.error("Error when closing input stream", e);
                        }
                    }
                }
                return outputStream.toByteArray();
            } else {
                throw storage.remoteException(account, response, "Failed to download file data");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to download file data", RemoteException.Reason.NetworkError, e);
        }
    }
}
