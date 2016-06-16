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

package fr.petrus.lib.core.cloud.implementations.hubic;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
import java.util.Locale;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.AbstractRemoteDocument;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.rest.ProgressRequestBody;
import fr.petrus.lib.core.rest.models.hubic.OpenStackObject;
import fr.petrus.lib.core.rest.services.hubic.OpenStackApiService;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.utils.StreamUtils;
import fr.petrus.lib.core.utils.StringUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The {@link RemoteDocument} implementation for HubiC.
 *
 * @author Pierre Sagne
 * @since 18.02.2015
 */
public class HubicDocument extends AbstractRemoteDocument<HubicStorage, HubicDocument> {
    private static Logger LOG = LoggerFactory.getLogger(HubicDocument.class);

    /** The remote path, without starting or trailing slash */
    private String path;

    private String mimeType;

    /**
     * Creates a new empty HubicDocument.
     *
     * @param hubicStorage a {@code HubicStorage} instance
     */
    public HubicDocument(HubicStorage hubicStorage) {
        super(hubicStorage);
        path = null;
        mimeType = null;
    }

    /**
     * Creates a HubicDocument from the result of an API request.
     *
     * @param hubicStorage    a {@code HubicStorage} instance
     * @param accountName     the account user name
     * @param openStackObject the {@code OpenStackObject} which represents this document
     */
    HubicDocument(HubicStorage hubicStorage, String accountName, OpenStackObject openStackObject) {
        this(hubicStorage);
        if (null!=openStackObject.subdir) {
            setPath(StringUtils.trimSlashes(openStackObject.subdir));
            setMimeType(Constants.HUBIC.FOLDER_MIME_TYPE);
        } else {
            setPath(StringUtils.trimSlashes(openStackObject.name));
            setMimeType(openStackObject.content_type);
            setSize(openStackObject.bytes);
            try {
                DateTime dateTime = new DateTime(openStackObject.last_modified);
                setModificationTime(dateTime.getMillis());
                setVersion(getModificationTime());
            } catch (Exception e) {
                LOG.error("Error while parsing date", e);
            }
        }
        setAccountName(accountName);
        setName(HubicDocument.getNameFromPath(getPath()));
        setFolder(Constants.HUBIC.FOLDER_MIME_TYPE.equals(getMimeType()));
    }

    /**
     * Creates a HubicDocument from the result of an API request.
     *
     * @param hubicStorage a {@code HubicStorage} instance
     * @param accountName  the account user name
     * @param path         the path of this document
     * @param response     the response of the API request
     */
    HubicDocument(HubicStorage hubicStorage, String accountName, String path, Response<?> response) {
        this(hubicStorage);
        setAccountName(accountName);
        setPath(StringUtils.trimSlashes(path));
        setName(HubicDocument.getNameFromPath(getPath()));

        String contentTypeHeaderValue = response.headers().get("Content-Type");
        if (null!=contentTypeHeaderValue) {
            setMimeType(contentTypeHeaderValue);
            setFolder(Constants.HUBIC.FOLDER_MIME_TYPE.equals(getMimeType()));
        }

        String contentLengthHeaderValue = response.headers().get("Content-Length");
        if (null!=contentLengthHeaderValue) {
            try {
                setSize(Long.parseLong(contentLengthHeaderValue));
            } catch (NumberFormatException e) {
                LOG.error("Error while parsing size", e);
            }
        }

        String lastModifiedHeaderValue = response.headers().get("Last-Modified");
        if (null!=lastModifiedHeaderValue) {
            try {
                DateTimeFormatter formatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss z")
                        .withLocale(Locale.ENGLISH);
                DateTime dateTime = formatter.parseDateTime(lastModifiedHeaderValue);
                setModificationTime(dateTime.getMillis());
                setVersion(getModificationTime());
            } catch (Exception e) {
                LOG.error("Error while parsing date", e);
            }
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.HubiC;
    }

    /**
     * Sets the path of this document.
     *
     * @param path the path of this document
     */
    public void setPath(String path) {
        if (null==path) {
            this.path = "";
        } else {
            this.path = StringUtils.trimSlashes(path);
        }
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
     * Returns the path of this document.
     *
     * @return the path of this document
     */
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     * HubiC documents do not have a "real" id, so the path is returned instead
     */
    @Override
    public String getId() {
        return path;
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
     * Returns the path of a child of this document.
     *
     * @param childName the name of the child document
     * @return the path of the child document
     */
    public String getChildPath(String childName) {
        if (null==path || path.isEmpty()) {
            return childName;
        }
        return path+"/"+childName;
    }

    /**
     * Returns the path of the parent folder of this document.
     *
     * @return the path of the parent folder of this document
     */
    public String getParentPath() {
        if (null==path || path.isEmpty()) {
            return "";
        }
        return new File(path).getParent();
    }

    /**
     * {@inheritDoc}
     * HubiC documents do not have a "real" id, so the path of the parent is returned instead
     */
    @Override
    public String getParentId() {
        return getParentPath();
    }

    @Override
    public HubicDocument childFile(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        return storage.file(getAccountName(), getChildPath(name));
    }

    @Override
    public HubicDocument childFolder(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        return storage.folder(getAccountName(), getChildPath(name));
    }

    @Override
    public HubicDocument childDocument(String name)
            throws DatabaseConnectionClosedException, RemoteException {
        return storage.document(getAccountName(), getChildPath(name));
    }

    @Override
    public List<HubicDocument> childDocuments(ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);

        List<HubicDocument> children = new ArrayList<>();
        try {
            Response<List<OpenStackObject>> response = openStackApiService.getFolderChildren(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER, getPath() + "/").execute();
            if (response.isSuccess()) {
                List<OpenStackObject> openStackObjects = response.body();
                if (null!=listener) {
                    listener.onSetMax(0, openStackObjects.size());
                    listener.pauseIfNeeded();
                    if (listener.isCanceled()) {
                        throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                    }
                }
                for (OpenStackObject openStackObject : openStackObjects) {
                    HubicDocument document = new HubicDocument(storage, account.getAccountName(),
                            openStackObject);
                    children.add(document);
                    if (null!=listener) {
                        listener.onProgress(0, children.size());
                        listener.pauseIfNeeded();
                        if (listener.isCanceled()) {
                            throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                        }
                    }
                }

                return children;
            } else {
                throw storage.cloudException(account, response, "Failed to get child documents");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get child documents", RemoteException.Reason.NetworkError, e);
        }
    }

    /**
     * Returns a virtual folder, as a child of this document.
     *
     * <p>The returned folder is qualified as "virtual" because it is not physically created on the
     * HubiC account.
     *
     * <p>In fact it is not possible to create a folder on HubiC. The folders exist as parts of the
     * path of files.
     *
     * @param name the name of the virtual folder
     * @return the virtual folder
     */
    public HubicDocument getVirtualChildFolder(String name) {
        return storage.virtualFolder(getAccountName(), getChildPath(name));
    }

    @Override
    public HubicDocument createChildFolder(String name) throws RemoteException {
        return getVirtualChildFolder(name);
    }

    @Override
    public HubicDocument createChildFile(String name, String mimeType)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);
        try {
            Response<ResponseBody> response = openStackApiService.uploadDocument(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER,
                    getChildPath(name),
                    RequestBody.create(MediaType.parse(mimeType), new byte[0])).execute();
            if (response.isSuccess()) {
                return new HubicDocument(storage, account.getAccountName(), getChildPath(name), response);
            } else {
                throw storage.cloudException(account, response, "Failed to create file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to create file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public HubicDocument uploadNewChildFile(String name, String mimeType, File localFile,
                                             ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);
        try {
            Response<ResponseBody> response = openStackApiService.uploadDocument(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER,
                    getChildPath(name),
                    new ProgressRequestBody(mimeType, localFile, listener)).execute();
            if (response.isSuccess()) {
                return new HubicDocument(storage, account.getAccountName(), getChildPath(name), response);
            } else {
                throw storage.cloudException(account, response, "Failed to upload new file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload new file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public HubicDocument uploadNewChildData(String name, String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);
        try {
            Response<ResponseBody> response = openStackApiService.uploadDocument(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER,
                    getChildPath(name),
                    RequestBody.create(MediaType.parse(mimeType), data)).execute();
            if (response.isSuccess()) {
                return new HubicDocument(storage, account.getAccountName(), getChildPath(name), response);
            } else {
                throw storage.cloudException(account, response, "Failed to upload new file data");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload new file data", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public HubicDocument uploadFile(String mimeType, File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);
        try {
            Response<ResponseBody> response = openStackApiService.uploadDocument(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER, getPath(),
                    new ProgressRequestBody(mimeType, localFile, listener)).execute();
            if (response.isSuccess()) {
                return new HubicDocument(storage, account.getAccountName(), getPath(), response);
            } else {
                throw storage.cloudException(account, response, "Failed to upload file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public HubicDocument uploadData(String mimeType, String fileName, byte[] data)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);
        try {
            Response<ResponseBody> response = openStackApiService.uploadDocument(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER, getPath(),
                    RequestBody.create(MediaType.parse(mimeType), data)).execute();
            if (response.isSuccess()) {
                return new HubicDocument(storage, account.getAccountName(), getPath(), response);
            } else {
                throw storage.cloudException(account, response, "Failed to upload file data");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to upload file data", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public void downloadFile(File localFile, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);
        try {
            Response<ResponseBody> response = openStackApiService.downloadDocument(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER,
                    getPath()).execute();
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
                throw storage.cloudException(account, response, "Failed to download file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to download file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public byte[] downloadData() throws DatabaseConnectionClosedException, RemoteException {
        Account account = storage.getRefreshedOpenStackAccount(getAccountName());
        OpenStackApiService openStackApiService = storage.getOpenStackApiService(account);
        try {
            Response<ResponseBody> response = openStackApiService.downloadDocument(
                    account.getOpenStackAccessToken(),
                    account.getOpenStackAccount(),
                    Constants.HUBIC.OPENSTACK_CONTAINER,
                    getPath()).execute();
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
                throw storage.cloudException(account, response, "Failed to download file data");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to download file data", RemoteException.Reason.NetworkError, e);
        }
    }
}
