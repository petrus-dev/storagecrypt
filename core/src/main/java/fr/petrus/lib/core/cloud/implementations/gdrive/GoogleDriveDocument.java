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

package fr.petrus.lib.core.cloud.implementations.gdrive;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.AbstractRemoteDocument;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.rest.ProgressRequestBody;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveItem;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveItems;
import fr.petrus.lib.core.rest.models.gdrive.NewItemArg;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.utils.StreamUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The {@link RemoteDocument} implementation for Google Drive.
 *
 * @author Pierre Sagne
 * @since 18.02.2015
 */
public class GoogleDriveDocument
        extends AbstractRemoteDocument<GoogleDriveStorage, GoogleDriveDocument> {
    private static Logger LOG = LoggerFactory.getLogger(GoogleDriveDocument.class);

    private String id;
    private String mimeType;
    private List<String> parentIds;

    /**
     * Creates a new empty GoogleDriveDocument.
     *
     * @param googleDriveStorage a {@code GoogleDriveStorage} instance
     */
    public GoogleDriveDocument(GoogleDriveStorage googleDriveStorage) {
        super(googleDriveStorage);
        id = null;
        mimeType = null;
        parentIds = new ArrayList<>();
    }

    /**
     * Creates a GoogleDriveDocument from the result of an API request.
     *
     * @param googleDriveStorage a {@code GoogleDriveStorage} instance
     * @param accountName        the account user name
     * @param item               the {@code GoogleDriveItem} which represents this document
     */
    GoogleDriveDocument(GoogleDriveStorage googleDriveStorage, String accountName, GoogleDriveItem item) {
        this(googleDriveStorage);
        setAccountName(accountName);
        setName(item.title);
        setMimeType(item.mimeType);
        setFolder(Constants.GOOGLE_DRIVE.FOLDER_MIME_TYPE.equals(getMimeType()));
        setId(item.id);
        if (null!=item.version) {
            setVersion(item.version);
        }
        if (null != item.fileSize) {
            setSize(item.fileSize);
        }
        try {
            DateTime dateTime = new DateTime(item.modifiedDate);
            setModificationTime(dateTime.getMillis());
        } catch (Exception e) {
            LOG.error("Error while parsing date", e);
        }
        if (null!=item.parents) {
            for (GoogleDriveItem.ParentRef parentRef : item.parents) {
                addParentId(parentRef.id);
            }
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.GoogleDrive;
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
     * Adds the remote id of a parent folder of this document.
     *
     * <p>Google Drive allows documents to have multiple parents.
     *
     * @param parentId the parent id
     */
    public void addParentId(String parentId) {
        if (null!=parentId) {
            if (!parentIds.contains(parentId)) {
                parentIds.add(parentId);
            }
        }
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

    /**
     * Returns the ids of the parents of this document.
     *
     * <p>Google Drive allows documents to have multiple parents.
     *
     * @return the ids of the parents of this document
     */
    public List<String> getParentIds() {
        return parentIds;
    }

    @Override
    public String getParentId() {
        if (parentIds.isEmpty()) {
            return null;
        }
        return parentIds.get(0);
    }

    @Override
    public GoogleDriveDocument childFile(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());

        String nextPageToken = null;
        do {
            String query = "title = '" + name + "' and "
                    + "mimeType != '" + Constants.GOOGLE_DRIVE.FOLDER_MIME_TYPE + "' and "
                    + "'" + getId() + "' in parents and trashed = false";
            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", query);
            if (null != nextPageToken) {
                params.put("pageToken", nextPageToken);
            }

            try {
                Response<GoogleDriveItems> response = storage.getApiService().getItems(
                        account.getAuthHeader(), params).execute();
                if (response.isSuccessful()) {
                    GoogleDriveItems googleDriveItems = response.body();
                    if (null != googleDriveItems.items) {
                        for (GoogleDriveItem item : googleDriveItems.items) {
                            if (name.equals(item.title)) {
                                return new GoogleDriveDocument(storage, getAccountName(), item);
                            }
                        }
                    }
                    nextPageToken = googleDriveItems.nextPageToken;
                } else {
                    throw storage.remoteException(account, response, "Failed to get child folder");
                }
            } catch (IOException | RuntimeException e) {
                throw new RemoteException("Failed to get child folder", RemoteException.Reason.NetworkError, e);
            }
        } while (nextPageToken!=null);
        throw new RemoteException("Child file not found", RemoteException.Reason.NotFound);
    }

    @Override
    public GoogleDriveDocument childFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());

        String nextPageToken = null;
        do {
            String query = "title = '" + name + "' and "
                    + "mimeType = '" + Constants.GOOGLE_DRIVE.FOLDER_MIME_TYPE + "' and "
                    + "'" + getId() + "' in parents and trashed = false";
            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", query);
            if (null != nextPageToken) {
                params.put("pageToken", nextPageToken);
            }

            try {
                Response<GoogleDriveItems> response = storage.getApiService().getItems(
                        account.getAuthHeader(), params).execute();
                if (response.isSuccessful()) {
                    GoogleDriveItems googleDriveItems = response.body();
                    if (null != googleDriveItems.items) {
                        for (GoogleDriveItem item : googleDriveItems.items) {
                            if (name.equals(item.title)) {
                                return new GoogleDriveDocument(storage, getAccountName(), item);
                            }
                        }
                    }
                    nextPageToken = googleDriveItems.nextPageToken;
                } else {
                    throw storage.remoteException(account, response, "Failed to get child folder");
                }
            } catch (IOException | RuntimeException e) {
                throw new RemoteException("Failed to get child folder", RemoteException.Reason.NetworkError, e);
            }
        } while (nextPageToken!=null);
        throw new RemoteException("Child folder not found", RemoteException.Reason.NotFound);
    }

    @Override
    public GoogleDriveDocument childDocument(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());

        String nextPageToken = null;
        do {
            String query = "title = '" + name + "' and '" + getId() + "' in parents and trashed = false";
            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", query);
            if (null != nextPageToken) {
                params.put("pageToken", nextPageToken);
            }

            try {
                Response<GoogleDriveItems> response = storage.getApiService().getItems(
                        account.getAuthHeader(), params).execute();
                if (response.isSuccessful()) {
                    GoogleDriveItems googleDriveItems = response.body();
                    if (null != googleDriveItems.items) {
                        for (GoogleDriveItem item : googleDriveItems.items) {
                            if (name.equals(item.title)) {
                                return new GoogleDriveDocument(storage, getAccountName(), item);
                            }
                        }
                    }
                    nextPageToken = googleDriveItems.nextPageToken;
                } else {
                    throw storage.remoteException(account, response, "Failed to get chid document");
                }
            } catch (IOException | RuntimeException e) {
                throw new RemoteException("Failed to get chid document", RemoteException.Reason.NetworkError, e);
            }
        } while (nextPageToken!=null);
        throw new RemoteException("Child document not found", RemoteException.Reason.NotFound);
    }

    @Override
    public List<GoogleDriveDocument> childDocuments(ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());

        List<GoogleDriveDocument> children = new ArrayList<>();
        String nextPageToken = null;
        do {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("q", "trashed = false");
            if (null != nextPageToken) {
                params.put("pageToken", nextPageToken);
            }

            try {
                Response<GoogleDriveItems> response = storage.getApiService().getChildren(
                        account.getAuthHeader(), getId(), params).execute();
                if (response.isSuccessful()) {
                    GoogleDriveItems googleDriveItems = response.body();
                    if (null != googleDriveItems.items) {
                        if (null!=listener) {
                            listener.onSetMax(0, children.size() + googleDriveItems.items.size());
                            listener.pauseIfNeeded();
                            if (listener.isCanceled()) {
                                throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                            }
                        }
                        for (GoogleDriveItem item : googleDriveItems.items) {
                            children.add(storage.document(getAccountName(), item.id));
                            if (null!=listener) {
                                listener.onProgress(0, children.size());
                                listener.pauseIfNeeded();
                                if (listener.isCanceled()) {
                                    throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                                }
                            }
                        }
                    }
                    nextPageToken = googleDriveItems.nextPageToken;
                } else {
                    throw storage.remoteException(account, response, "Failed to get child documents");
                }
            } catch (IOException | RuntimeException e) {
                throw new RemoteException("Failed to get child documents", RemoteException.Reason.NetworkError, e);
            }
        } while (nextPageToken!=null);

        return children;
    }

    @Override
    public GoogleDriveDocument createChildFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            NewItemArg body = new NewItemArg(name, getId(), Constants.GOOGLE_DRIVE.FOLDER_MIME_TYPE);
            Response<GoogleDriveItem> response = storage.getApiService().createFolder(
                    account.getAuthHeader(), body).execute();
            if (response.isSuccessful()) {
                return new GoogleDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to create folder");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to create folder", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public GoogleDriveDocument createChildFile(String name, String mimeType)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            NewItemArg body = new NewItemArg(name, getId(), mimeType);
            Response<GoogleDriveItem> response = storage.getApiService().createFile(
                    account.getAuthHeader(), body).execute();
            if (response.isSuccessful()) {
                return new GoogleDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to create file");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to create file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public GoogleDriveDocument uploadNewChildFile(String name, String mimeType, File localFile,
                                             ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MediaType.parse("multipart/related"))
                    .addPart(RequestBody.create(MediaType.parse("application/json; charset=UTF-8"),
                            new NewItemArg(name, getId(), mimeType).toString().getBytes("UTF8")))
                    .addPart(new ProgressRequestBody(mimeType, localFile, listener))
                    .build();

            Response<GoogleDriveItem> response = storage.getApiService().uploadNewFile(
                    account.getAuthHeader(), body).execute();
            if (response.isSuccessful()) {
                return new GoogleDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to upload new file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public GoogleDriveDocument uploadNewChildData(String name, String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MediaType.parse("multipart/related"))
                    .addPart(RequestBody.create(MediaType.parse("application/json; charset=UTF-8"),
                            new NewItemArg(name, getId(), mimeType).toString().getBytes("UTF8")))
                    .addPart(RequestBody.create(MediaType.parse(mimeType), data))
                    .build();

            Response<GoogleDriveItem> response = storage.getApiService().uploadNewFile(
                    account.getAuthHeader(), body).execute();
            if (response.isSuccessful()) {
                return new GoogleDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file data");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to upload new file data", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public GoogleDriveDocument uploadFile(String mimeType, File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<GoogleDriveItem> response = storage.getApiService().uploadFile(
                    account.getAuthHeader(), getId(),
                    new ProgressRequestBody(mimeType, localFile, listener)).execute();
            if (response.isSuccessful()) {
                return new GoogleDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to upload file");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to upload file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public GoogleDriveDocument uploadData(String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<GoogleDriveItem> response = storage.getApiService().uploadFile(
                    account.getAuthHeader(), getId(),
                    RequestBody.create(MediaType.parse(mimeType), data)).execute();
            if (response.isSuccessful()) {
                return new GoogleDriveDocument(storage, getAccountName(), response.body());
            } else {
                throw storage.remoteException(account, response, "Failed to upload file data");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to upload file data", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public void downloadFile(File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<ResponseBody> response = storage.getApiService().downloadItem(
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
                } catch (IOException | RuntimeException e) {
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
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to download file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public byte[] downloadData() throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<ResponseBody> response = storage.getApiService().downloadItem(
                    account.getAuthHeader(), getId()).execute();
            if (response.isSuccessful()) {
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    inputStream = new BufferedInputStream(response.body().byteStream());
                    StreamUtils.copy(outputStream, inputStream, Constants.FILE.BUFFER_SIZE, null);
                } catch (IOException | RuntimeException e) {
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
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to download file data", RemoteException.Reason.NetworkError, e);
        }
    }
}
