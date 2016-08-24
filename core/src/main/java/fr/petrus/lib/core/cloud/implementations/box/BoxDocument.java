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

package fr.petrus.lib.core.cloud.implementations.box;

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
import fr.petrus.lib.core.cloud.exceptions.NetworkException;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.rest.ProgressRequestBody;
import fr.petrus.lib.core.rest.models.box.BoxItem;
import fr.petrus.lib.core.rest.models.box.BoxItems;
import fr.petrus.lib.core.rest.models.box.NewItemArg;
import fr.petrus.lib.core.rest.models.box.UpdateDescriptionArg;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.utils.StreamUtils;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The {@link RemoteDocument} implementation for Box.com.
 *
 * @author Pierre Sagne
 * @since 18.02.2015
 */
public class BoxDocument extends AbstractRemoteDocument<BoxStorage, BoxDocument> {
    private static Logger LOG = LoggerFactory.getLogger(BoxDocument.class);

    private String id;
    private String parentId;

    /**
     * Creates a new empty BoxDocument.
     *
     * @param boxStorage a {@code BoxStorage} instance
     */
    public BoxDocument(BoxStorage boxStorage) {
        super(boxStorage);
        id = null;
        parentId = null;
    }

    /**
     * Creates a BoxDocument from the result of an API request.
     *
     * @param boxStorage  a {@code BoxStorage} instance
     * @param accountName the account user name
     * @param boxItem     the {@code BoxItem} which represents this document
     */
    BoxDocument(BoxStorage boxStorage, String accountName, BoxItem boxItem) {
        this(boxStorage);
        setAccountName(accountName);
        setName(boxItem.name);
        setId(boxItem.id);
        setFolder("folder".equals(boxItem.type));
        if (null != boxItem.parent) {
            setParentId(boxItem.parent.id);
        }
        if (null != boxItem.etag) {
            setVersion(Long.parseLong(boxItem.etag));
        }
        if (null!=boxItem.size) {
            setSize(boxItem.size);
        }
        try {
            DateTime dateTime = new DateTime(boxItem.modified_at);
            setModificationTime(dateTime.getMillis());
        } catch (Exception e) {
            LOG.error("Error while parsing date", e);
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.Box;
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

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public BoxDocument childFile(String name)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());

        int total_count;
        int offset = 0;
        do {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("fields", "name,version_number,size,parent,created_at,modified_at");
            if (0!=offset) {
                params.put("offset", String.valueOf(offset));
            }

            try {
                Response<BoxItems> response = storage.getApiService().getFolderItems(account.getAuthHeader(),
                        getId(), params).execute();
                if (response.isSuccessful()) {
                    BoxItems boxItems = response.body();
                    if (null!= boxItems.entries) {
                        for (BoxItem entry : boxItems.entries) {
                            if (name.equals(entry.name) && "file".equals(entry.type)) {
                                return new BoxDocument(storage, getAccountName(), entry);
                            }
                        }
                    }
                    total_count = boxItems.total_count;
                    offset = boxItems.offset + boxItems.limit;
                } else {
                    throw storage.remoteException(account, response, "Failed to get child file");
                }
            } catch (IOException | RuntimeException e) {
                throw new NetworkException("Failed to get child file", e);
            }

        } while (offset < total_count);
        throw new RemoteException("Failed to get child file : not found", RemoteException.Reason.NotFound);
    }

    @Override
    public BoxDocument childFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());

        int total_count;
        int offset = 0;
        do {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("fields", "name,version_number,size,parent,created_at,modified_at");
            if (0!=offset) {
                params.put("offset", String.valueOf(offset));
            }

            try {
                Response<BoxItems> response = storage.getApiService().getFolderItems(account.getAuthHeader(),
                        getId(), params).execute();
                if (response.isSuccessful()) {
                    BoxItems boxItems = response.body();
                    if (null != boxItems.entries) {
                        for (BoxItem entry : boxItems.entries) {
                            if (name.equals(entry.name) && "folder".equals(entry.type)) {
                                return new BoxDocument(storage, getAccountName(), entry);
                            }
                        }
                    }
                    total_count = boxItems.total_count;
                    offset = boxItems.offset + boxItems.limit;
                } else {
                    throw storage.remoteException(account, response, "Failed to get child folder");
                }
            } catch (IOException | RuntimeException e) {
                throw new NetworkException("Failed to get folder", e);
            }
        } while (offset < total_count);
        throw new RemoteException("Failed to get child folder : not found", RemoteException.Reason.NotFound);
    }

    @Override
    public BoxDocument childDocument(String name)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());

        int total_count;
        int offset = 0;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("fields", "name,version_number,size,parent,created_at,modified_at");
        do {
            if (0!=offset) {
                params.put("offset", String.valueOf(offset));
            } else {
                params.remove("offset");
            }

            try {
                Response<BoxItems> response = storage.getApiService().getFolderItems(account.getAuthHeader(),
                        getId(), params).execute();
                if (response.isSuccessful()) {
                    BoxItems boxItems = response.body();
                    if (null!= boxItems.entries) {
                        for (BoxItem entry : boxItems.entries) {
                            if (name.equals(entry.name)) {
                                return new BoxDocument(storage, getAccountName(), entry);
                            }
                        }
                    }
                    total_count = boxItems.total_count;
                    offset = boxItems.offset + boxItems.limit;
                } else {
                    throw storage.remoteException(account, response, "Failed to get child document");
                }
            } catch (IOException | RuntimeException e) {
                throw new NetworkException("Failed to get child document", e);
            }
        } while (offset < total_count);
        throw new RemoteException("Failed to get child document : not found", RemoteException.Reason.NotFound);
    }

    @Override
    public List<BoxDocument> childDocuments(ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());

        List<BoxDocument> children = new ArrayList<>();

        int total_count;
        int offset = 0;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("fields", "name,version_number,size,parent,created_at,modified_at");
        do {
            if (0!=offset) {
                params.put("offset", String.valueOf(offset));
            } else {
                params.remove("offset");
            }

            try {
                Response<BoxItems> response = storage.getApiService().getFolderItems(account.getAuthHeader(),
                        getId(), params).execute();
                if (response.isSuccessful()) {
                    BoxItems boxItems = response.body();
                    if (null != boxItems.entries) {
                        if (null!=listener) {
                            listener.onSetMax(0, children.size() + boxItems.entries.size());
                            listener.pauseIfNeeded();
                            if (listener.isCanceled()) {
                                throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                            }
                        }
                        for (BoxItem entry : boxItems.entries) {
                            children.add(new BoxDocument(storage, getAccountName(), entry));
                            if (null!=listener) {
                                listener.onProgress(0, children.size());
                                listener.pauseIfNeeded();
                                if (listener.isCanceled()) {
                                    throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                                }
                            }
                        }
                    }
                    total_count = boxItems.total_count;
                    offset = boxItems.offset + boxItems.limit;
                } else {
                    throw storage.remoteException(account, response, "Failed to get child documents");
                }
            } catch (IOException | RuntimeException e) {
                throw new NetworkException("Failed to get child documents", e);
            }
        } while (offset < total_count);
        return children;
    }

    @Override
    public BoxDocument createChildFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<BoxItem> response;
            response = storage.getApiService().createFolder(account.getAuthHeader(),
                    new NewItemArg(name, getId())).execute();
            if (response.isSuccessful()) {
                Response<BoxItem> updateResponse = storage.getApiService().updateFolderDescription(
                        account.getAuthHeader(), response.body().id,
                        new UpdateDescriptionArg(Constants.BOX.DESCRIPTION_STRING)).execute();
                if (updateResponse.isSuccessful()) {
                    return new BoxDocument(storage, getAccountName(), updateResponse.body());
                }
            }
            throw storage.remoteException(account, response, "Failed to create folder");
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to create folder", e);
        }
    }

    @Override
    public BoxDocument createChildFile(String name, String mimeType)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("attributes", new NewItemArg(name, getId()).toString())
                    .addFormDataPart("file", name, RequestBody.create(MediaType.parse(mimeType), new byte[0]))
                    .build();
            Response<BoxItems> response = storage.getUploadApiService().createFile(account.getAuthHeader(), body).execute();
            if (response.isSuccessful()) {
                BoxItems boxItems = response.body();
                if (null != boxItems.entries) {
                    for (BoxItem item : boxItems.entries) {
                        if (name.equals(item.name) && "file".equals(item.type)) {
                            Response<BoxItem> updateResponse = storage.getApiService().updateFileDescription(
                                    account.getAuthHeader(), item.id,
                                    new UpdateDescriptionArg(Constants.BOX.DESCRIPTION_STRING)).execute();
                            if (updateResponse.isSuccessful()) {
                                return new BoxDocument(storage, getAccountName(), updateResponse.body());
                            } else {
                                throw storage.remoteException(account, updateResponse, "Failed to create file");
                            }
                        }
                    }
                    if (boxItems.total_count > boxItems.entries.size()) {
                        LOG.debug("document not found but more results are present : limit={}, offset={}, total_count={}",
                                boxItems.limit, boxItems.offset, boxItems.total_count);
                    }
                }
            } else {
                throw storage.remoteException(account, response, "Failed to create file");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to create file", e);
        }
        throw new RemoteException("Failed to create file : not found in response", RemoteException.Reason.NotFound);
    }

    @Override
    public BoxDocument uploadNewChildFile(String name, String mimeType, File localFile,
                                             ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("attributes", new NewItemArg(name, getId()).toString())
                    .addFormDataPart("file", localFile.getName(), new ProgressRequestBody(mimeType, localFile, listener))
                    .build();
            Response<BoxItems> response = storage.getUploadApiService().uploadNewFile(account.getAuthHeader(),
                    body).execute();
            if (response.isSuccessful()) {
                BoxItems boxItems = response.body();
                if (null != boxItems.entries) {
                    for (BoxItem item : boxItems.entries) {
                        if (name.equals(item.name) && "file".equals(item.type)) {
                            Response<BoxItem> updateResponse = storage.getApiService().updateFileDescription(
                                    account.getAuthHeader(), item.id,
                                    new UpdateDescriptionArg(Constants.BOX.DESCRIPTION_STRING)).execute();
                            if (updateResponse.isSuccessful()) {
                                return new BoxDocument(storage, getAccountName(), updateResponse.body());
                            } else {
                                throw storage.remoteException(account, updateResponse, "Failed to upload new file");
                            }
                        }
                    }
                    if (boxItems.total_count > boxItems.entries.size()) {
                        LOG.debug("document not found but more results are present : limit={}, offset={}, total_count={}",
                                boxItems.limit, boxItems.offset, boxItems.total_count);
                    }
                }
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to upload new file", e);
        }
        throw new RemoteException("Failed to upload new file : not found in response", RemoteException.Reason.NotFound);
    }

    @Override
    public BoxDocument uploadNewChildData(String name, String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("attributes", new NewItemArg(name, getId()).toString())
                    .addFormDataPart("file", fileName, RequestBody.create(MediaType.parse(mimeType), data))
                    .build();
            Response<BoxItems> response = storage.getUploadApiService().uploadNewFile(account.getAuthHeader(),
                    body).execute();
            if (response.isSuccessful()) {
                BoxItems boxItems = response.body();
                if (null != boxItems.entries) {
                    for (BoxItem item : boxItems.entries) {
                        if (name.equals(item.name) && "file".equals(item.type)) {
                            Response<BoxItem> updateResponse = storage.getApiService().updateFileDescription(
                                    account.getAuthHeader(), item.id,
                                    new UpdateDescriptionArg(Constants.BOX.DESCRIPTION_STRING)).execute();
                            if (updateResponse.isSuccessful()) {
                                return new BoxDocument(storage, getAccountName(), updateResponse.body());
                            } else {
                                throw storage.remoteException(account, updateResponse, "Failed to upload new file");
                            }
                        }
                    }
                    if (boxItems.total_count > boxItems.entries.size()) {
                        LOG.debug("document not found but more results are present : limit={}, offset={}, total_count={}",
                                boxItems.limit, boxItems.offset, boxItems.total_count);
                    }
                }
            } else {
                throw storage.remoteException(account, response, "Failed to upload new file");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to upload new file", e);
        }
        throw new RemoteException("Failed to upload new file : not found in response", RemoteException.Reason.NotFound);
    }

    @Override
    public BoxDocument uploadFile(String mimeType, File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("attributes", new NewItemArg(getName(), getParentId()).toString())
                    .addFormDataPart("file", localFile.getName(), new ProgressRequestBody(mimeType, localFile, listener))
                    .build();
            Response<BoxItems> response = storage.getUploadApiService().uploadFile(
                    account.getAuthHeader(), getId(), body).execute();
            if (response.isSuccessful()) {
                BoxItems boxItems = response.body();
                if (null != boxItems.entries) {
                    for (BoxItem item : boxItems.entries) {
                        if (getId().equals(item.id)) {
                            return new BoxDocument(storage, getAccountName(), item);
                        }
                    }
                }
            } else {
                throw storage.remoteException(account, response, "Failed to upload file");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to upload file", e);
        }
        throw new RemoteException("Failed to upload file : not found in response", RemoteException.Reason.NotFound);
    }

    @Override
    public BoxDocument uploadData(String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("attributes", new NewItemArg(getName(), getParentId()).toString())
                    .addFormDataPart("file", fileName, RequestBody.create(MediaType.parse(mimeType), data))
                    .build();
            Response<BoxItems> response = storage.getUploadApiService().uploadFile(account.getAuthHeader(),
                    getId(), body).execute();
            if (response.isSuccessful()) {
                BoxItems boxItems = response.body();
                if (null != boxItems.entries) {
                    for (BoxItem item : boxItems.entries) {
                        if (getId().equals(item.id)) {
                            return new BoxDocument(storage, getAccountName(), item);
                        }
                    }
                }
                throw new RemoteException("Failed to upload file data: not found in response", RemoteException.Reason.NotFound);
            } else {
                throw storage.remoteException(account, response, "Failed to upload file data");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to upload file data", e);
        }
    }

    @Override
    public void downloadFile(File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<ResponseBody> response = storage.getApiService().downloadFile(account.getAuthHeader(),
                    getId()).execute();
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
            throw new NetworkException("Failed to download file", e);
        }
    }

    @Override
    public byte[] downloadData() throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = storage.refreshedAccount(getAccountName());
        try {
            Response<ResponseBody> response = storage.getApiService().downloadFile(
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
            throw new NetworkException("Failed to download file data", e);
        }
    }
}
