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

package fr.petrus.lib.core.cloud.implementations.onedrive;

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
import fr.petrus.lib.core.rest.models.onedrive.NewFolderArg;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveItem;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveItems;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.utils.StreamUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The {@link RemoteDocument} implementation for OneDrive.
 *
 * @author Pierre Sagne
 * @since 18.02.2015
 */
public class OneDriveDocument extends AbstractRemoteDocument<OneDriveStorage, OneDriveDocument> {
    private static Logger LOG = LoggerFactory.getLogger(OneDriveDocument.class);

    private String id;
    private String mimeType;
    private String parentId;

    /**
     * Creates a new empty OneDriveDocument.
     *
     * @param oneDriveStorage a {@code OneDriveStorage} instance
     */
    public OneDriveDocument(OneDriveStorage oneDriveStorage) {
        super(oneDriveStorage);
        id = null;
        mimeType = null;
        parentId = null;
    }

    /**
     * Creates a OneDriveDocument from the result of an API request.
     *
     * @param oneDriveStorage a {@code OneDriveStorage} instance
     * @param accountName     the account user name
     * @param item            the {@code OneDriveItem} which represents this document
     */
    OneDriveDocument(OneDriveStorage oneDriveStorage, String accountName, OneDriveItem item) {
        this(oneDriveStorage);
        setAccountName(accountName);
        setName(item.name);
        if (null!=item.folder) {
            setMimeType(Constants.ONE_DRIVE.FOLDER_MIME_TYPE);
            setFolder(true);
        } else if (null!=item.file) {
            setMimeType(item.file.mimeType);
            setFolder(false);
        }
        setId(item.id);
        if (null != item.size) {
            setSize(item.size);
        }
        if (null!=item.parentReference) {
            setParentId(item.parentReference.id);
        }
        try {
            DateTime dateTime = new DateTime(item.lastModifiedDateTime);
            setModificationTime(dateTime.getMillis());
            setVersion(getModificationTime());
        } catch (Exception e) {
            LOG.error("Error while parsing date", e);
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.OneDrive;
    }

    /**
     * Sets the remote id of this document.
     *
     * @param id the remote id of this document
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the mime type of this document.
     *
     * @param mimeType the mime type of this document
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Sets the remote id of the parent folder of this document.
     *
     * @param parentId the remote id of the parent folder of this document
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns the mime type of this document.
     *
     * @return the mime type of this document
     */
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public OneDriveDocument childFile(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        OneDriveDocument document;
        try {
            document = childDocument(name);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to get file", e.getReason(), e);
        }

        if (!document.isFolder()) {
            return document;
        }

        throw new RemoteException("Failed to get file : the document found is not a file",
                RemoteException.Reason.NotAFile);
    }

    @Override
    public OneDriveDocument childFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        OneDriveDocument document;
        try {
            document = childDocument(name);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to get folder", e.getReason(), e);
        }

        if (document.isFolder()) {
            return document;
        }

        throw new RemoteException("Failed to get folder : the document found is not a folder",
                RemoteException.Reason.NotAFolder);
    }

    @Override
    public OneDriveDocument childDocument(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<OneDriveItems> response = storage.getApiService().getChildrenById(
                    account.getAuthHeader(), getId()).execute();
            if (response.isSuccessful()) {
                OneDriveItems items = response.body();
                if (null!=items && null!=items.value) {
                    for (OneDriveItem item : items.value) {
                        if (name.equals(item.name)) {
                            return new OneDriveDocument(storage, getAccountName(), item);
                        }
                    }
                }
                throw new RemoteException("Child document not found", RemoteException.Reason.NotFound);
            } else {
                throw storage.remoteException(account, response, "Failed to get child document");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get child document", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public List<OneDriveDocument> childDocuments(ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<OneDriveItems> response = storage.getApiService().getChildrenById(
                    account.getAuthHeader(), getId()).execute();
            if (response.isSuccessful()) {
                OneDriveItems items = response.body();

                List<OneDriveDocument> documents = new ArrayList<>();
                if (null!=items && null!=items.value) {
                    if (null!=listener) {
                        listener.onSetMax(0, items.value.size());
                        listener.pauseIfNeeded();
                        if (listener.isCanceled()) {
                            throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                        }
                    }
                    for (OneDriveItem item : items.value) {
                        documents.add(new OneDriveDocument(storage, getAccountName(), item));
                        if (null!=listener) {
                            listener.onProgress(0, documents.size());
                            listener.pauseIfNeeded();
                            if (listener.isCanceled()) {
                                throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                            }
                        }
                    }
                }

                return documents;
            } else {
                throw storage.remoteException(account, response, "Failed to get child documents");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get child documents", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument createChildFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            NewFolderArg body = new NewFolderArg(name);
            Response<OneDriveItem> response = storage.getApiService().createFolderById(
                    account.getAuthHeader(), getId(), body).execute();
            if (response.isSuccessful()) {
                return new OneDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to create folder");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to create folder", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument createChildFile(String name, String mimeType)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<OneDriveItem> response = storage.getApiService().createFileById(
                    account.getAuthHeader(), getId(), name,
                    RequestBody.create(MediaType.parse(mimeType), new byte[0])).execute();
            if (response.isSuccessful()) {
                return new OneDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to create file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to create file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument uploadNewChildFile(String name, String mimeType, File localFile,
                                             ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<OneDriveItem> response = storage.getApiService().uploadNewFileById(
                    account.getAuthHeader(), getId(), name,
                    new ProgressRequestBody(mimeType, localFile, listener)).execute();
            if (response.isSuccessful()) {
                return new OneDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload new file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument uploadNewChildData(String name, String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<OneDriveItem> response = storage.getApiService().uploadFileById(
                    account.getAuthHeader(), getId(), name,
                    RequestBody.create(MediaType.parse(mimeType), data)).execute();
            if (response.isSuccessful()) {
                return new OneDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file data");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload new file data", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument uploadFile(String mimeType, File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<OneDriveItem> response = storage.getApiService().uploadFileById(
                    account.getAuthHeader(), getParentId(), getName(),
                    new ProgressRequestBody(mimeType, localFile, listener)).execute();
            if (response.isSuccessful()) {
                return new OneDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to upload file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument uploadData(String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<OneDriveItem> response = storage.getApiService().uploadFileById(
                    account.getAuthHeader(), getParentId(), getName(),
                    RequestBody.create(MediaType.parse(mimeType), data)).execute();
            if (response.isSuccessful()) {
                return new OneDriveDocument(storage, getAccountName(), response.body());
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
            Response<ResponseBody> response = storage.getApiService().downloadDocumentById(
                    account.getAuthHeader(), getId()).execute();
            if (response.isSuccessful()) {
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
            Response<ResponseBody> response = storage.getApiService().downloadDocumentById(
                    account.getAuthHeader(), getId()).execute();
            if (response.isSuccessful()) {
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
