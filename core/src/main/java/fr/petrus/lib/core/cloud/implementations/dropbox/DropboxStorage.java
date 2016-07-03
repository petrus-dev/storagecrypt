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

import com.google.gson.Gson;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import fr.petrus.lib.core.cloud.AbstractRemoteStorage;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteStorage;
import fr.petrus.lib.core.cloud.appkeys.AppKeys;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.rest.models.dropbox.DropboxLatestCursorResult;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.RemoteChange;
import fr.petrus.lib.core.cloud.RemoteChanges;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.rest.models.dropbox.DropboxFolderResult;
import fr.petrus.lib.core.rest.models.dropbox.DropboxMetadata;
import fr.petrus.lib.core.rest.models.dropbox.DropboxSpaceUsage;
import fr.petrus.lib.core.rest.models.dropbox.GetMetadataArg;
import fr.petrus.lib.core.rest.models.dropbox.ListFolderArg;
import fr.petrus.lib.core.rest.models.dropbox.ListFolderContinueArg;
import fr.petrus.lib.core.rest.models.dropbox.PathArg;
import fr.petrus.lib.core.rest.models.dropbox.error.DropboxError;
import fr.petrus.lib.core.rest.models.OauthTokenResponse;
import fr.petrus.lib.core.rest.models.dropbox.DropboxUser;
import fr.petrus.lib.core.rest.services.dropbox.DropboxApiService;
import fr.petrus.lib.core.rest.services.dropbox.DropboxContentApiService;
import fr.petrus.lib.core.rest.services.dropbox.DropboxRestClient;
import fr.petrus.lib.core.result.ProcessProgressListener;

/**
 * The {@link RemoteStorage} implementation for Dropbox.
 *
 * @author Pierre Sagne
 * @since 10.02.2015
 */
public class DropboxStorage extends AbstractRemoteStorage<DropboxStorage, DropboxDocument> {
    private static Logger LOG = LoggerFactory.getLogger(DropboxStorage.class);

    private DropboxApiService apiService;
    private DropboxContentApiService contentApiService;

    /**
     * Creates a new DropboxStorage, providing its dependencies.
     *
     * @param crypto       a {@code Crypto} instance
     * @param cloudAppKeys a {@code CloudAppKeys} instance
     * @param accounts     a {@code Accounts} instance
     */
    public DropboxStorage(Crypto crypto, CloudAppKeys cloudAppKeys, Accounts accounts) {
        super(crypto, cloudAppKeys, accounts);
        DropboxRestClient client = new DropboxRestClient();
        apiService = client.getApiService();
        contentApiService = client.getContentApiService();
    }

    /**
     * Returns the {@code DropboxApiService}, used to call the underlying API.
     *
     * @return the {@code DropboxApiService}
     */
    DropboxApiService getApiService() {
        return apiService;
    }

    /**
     * Returns the {@code DropboxContentApiService}, used to call the underlying API.
     *
     * @return the {@code DropboxContentApiService}
     */
    DropboxContentApiService getContentApiService() {
        return contentApiService;
    }

    @Override
    public RemoteException.Reason retrofitErrorReason(Response<?> response) {
        if (!response.isSuccess()) {
            switch (response.code()) {
                case 400:
                    return RemoteException.Reason.BadRequest;
                case 401:
                    return RemoteException.Reason.Unauthorized;
                case 403:
                    return RemoteException.Reason.Forbidden;
                case 404:
                    return RemoteException.Reason.NotFound;
                case 405:
                    return RemoteException.Reason.MethodNotAllowed;
                case 409:
                    Gson gson = new Gson();
                    Reader reader = new InputStreamReader(response.errorBody().byteStream());
                    try {
                        DropboxError errorBody = gson.fromJson(reader, DropboxError.class);
                        if (null!=errorBody.error_summary) {
                            if (errorBody.error_summary.startsWith("path/not_found")) {
                                return RemoteException.Reason.NotFound;
                            }
                            if (errorBody.error_summary.startsWith("reset")) {
                                return RemoteException.Reason.CursorExpired;
                            }
                        }
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            LOG.error("Error when closing reader", e);
                        }
                    }
                    return RemoteException.Reason.Conflict;
                case 412:
                    return RemoteException.Reason.PreconditionFailed;
                case 429:
                    return RemoteException.Reason.TooManyRequests;
                case 500:
                    return RemoteException.Reason.InternalServerError;
                case 503:
                    return RemoteException.Reason.Unavailable;
                default:
                    return RemoteException.Reason.UnknownError;
            }
        }
        return RemoteException.Reason.NotAnError;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.Dropbox;
    }

    @Override
    public String oauthAuthorizeUrl(boolean mobileVersion) throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getDropboxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        String url = Constants.DROPBOX.OAUTH_URL
                + "?client_id=" + appKeys.getClientId()
                + "&redirect_uri=" + appKeys.getRedirectUri()
                + "&response_type=" + Constants.DROPBOX.RESPONSE_TYPE;

        try {
            url += "&state=" + requestCSRFToken();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't generate a CSRF token", e);
        }

        return url;
    }

    @Override
    public String oauthAuthorizeRedirectUri() throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getDropboxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }
        return appKeys.getRedirectUri();
    }

    @Override
    public Account connectWithAccessCode(Map<String, String> responseParameters)
            throws DatabaseConnectionClosedException, RemoteException {

        AppKeys appKeys = cloudAppKeys.getDropboxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", responseParameters.get("code"));
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("redirect_uri", appKeys.getRedirectUri());
        params.put("grant_type", Constants.DROPBOX.AUTHORIZATION_CODE_GRANT_TYPE);

        try {
            Response<OauthTokenResponse> response = apiService.getOauthToken(params).execute();
            if (response.isSuccess()) {
                OauthTokenResponse oauthTokenResponse = response.body();
                String accountName = accountNameFromAccessToken(oauthTokenResponse.access_token);

                Account account = createAccount();
                account.setAccountName(accountName);
                account.setAccessToken(oauthTokenResponse.access_token);
                accounts.add(account);
                return account;
            } else {
                throw new RemoteException("Failed to get oauth token", retrofitErrorReason(response));
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get oauth token", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public String accountNameFromAccessToken(String accessToken) throws RemoteException {
        if (null==accessToken) {
            throw new RemoteException("Failed to get account name : access token is null",
                    RemoteException.Reason.AccessTokenIsNull);
        }

        try {
            Response<DropboxUser> response = apiService.getAccountInfo("Bearer " + accessToken).execute();
            if (response.isSuccess()) {
                return response.body().email;
            } else {
                throw new RemoteException("Failed to get account name", retrofitErrorReason(response));
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get account name", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public Account refreshToken(String accountName)
            throws DatabaseConnectionClosedException, RemoteException {
        return account(accountName);
    }

    @Override
    public Account refreshQuota(Account account)
            throws DatabaseConnectionClosedException, RemoteException {
        try {
            Response<DropboxSpaceUsage> response = apiService.getSpaceUsage(account.getAuthHeader()).execute();
            if (response.isSuccess()) {
                DropboxSpaceUsage dropboxSpaceUsage = response.body();
                if (null!=dropboxSpaceUsage.allocation && null!=dropboxSpaceUsage.allocation.allocated) {
                    account.setQuotaAmount(dropboxSpaceUsage.allocation.allocated);
                }
                if (null!=dropboxSpaceUsage.used) {
                    account.setQuotaUsed(dropboxSpaceUsage.used);
                }
                account.update();
                return account;
            } else {
                throw remoteException(account, response, "Failed to get quota");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get quota", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public void revokeToken(String accountName)
            throws DatabaseConnectionClosedException, RemoteException {
        if (null==accountName) {
            throw new RemoteException("Failed to revoke access token : account name is null",
                    RemoteException.Reason.AccountNameIsNull);
        }

        Account account = account(accountName);
        if (null==account) {
            throw new RemoteException("Failed to revoke access token : account is null",
                    RemoteException.Reason.AccountNotFound);
        }

        try {
            Response<ResponseBody> response = apiService.revokeOauthToken(account.getAuthHeader()).execute();
            if (!response.isSuccess()) {
                throw remoteException(account, response, "Failed to revoke access token");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to revoke access token", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public DropboxDocument rootFolder(String accountName) throws RemoteException {
        DropboxDocument root = new DropboxDocument(this);
        root.setAccountName(accountName);
        root.setId("");
        root.setPath("/");
        root.setName("");
        root.setFolder(true);
        return root;
    }

    @Override
    public DropboxDocument folder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        DropboxDocument document;
        try {
            document = document(accountName, id);
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
    public DropboxDocument file(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        DropboxDocument document;
        try {
            document = document(accountName, id);
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
    public DropboxDocument document(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<DropboxMetadata> response = apiService.getMetadata(account.getAuthHeader(), new GetMetadataArg(id)).execute();
            if (response.isSuccess()) {
                DropboxDocument document = new DropboxDocument(this, accountName, response.body());
                String parentPath = document.getParentPath();
                if (null != parentPath && !parentPath.isEmpty() && !parentPath.equals("/")) {
                    Response<DropboxMetadata> parentMetadataResponse = apiService.getMetadata(account.getAuthHeader(),
                            new GetMetadataArg(document.getParentPath())).execute();
                    if (parentMetadataResponse.isSuccess()) {
                        document.setParentId(parentMetadataResponse.body().id);
                    } else {
                        throw remoteException(account, response, "Failed to get document");
                    }
                }
                return document;
            } else {
                throw remoteException(account, response, "Failed to get document");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get document", RemoteException.Reason.NetworkError);
        }
    }

    @Override
    public RemoteChanges changes(String accountName, String lastChangeId, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        Response<DropboxFolderResult> response;
        try {
            if (null==lastChangeId) {
                response = apiService.listFolder(account.getAuthHeader(),
                        new ListFolderArg("/" + Constants.FILE.APP_DIR_NAME, true)).execute();
            } else {
                Response<DropboxLatestCursorResult> latestCursorResultResponse = apiService.getLatestCursor(
                        account.getAuthHeader(),
                        new ListFolderArg("/" + Constants.FILE.APP_DIR_NAME, true)).execute();
                if (latestCursorResultResponse.isSuccess()) {
                    lastChangeId = latestCursorResultResponse.body().cursor;
                    response = apiService.listFolderContinue(account.getAuthHeader(),
                            new ListFolderContinueArg(lastChangeId)).execute();
                } else {
                    throw remoteException(account, latestCursorResultResponse, "Failed to get changes");
                }
            }
            if (!response.isSuccess()) {
                RemoteException remoteException = remoteException(account, response, "Failed to get changes");
                if (remoteException.getReason() == RemoteException.Reason.CursorExpired) {
                    response = apiService.listFolder(account.getAuthHeader(),
                            new ListFolderArg("/" + Constants.FILE.APP_DIR_NAME, true)).execute();
                    if (!response.isSuccess()) {
                        throw remoteException(account, response, "Failed to get changes");
                    }
                } else {
                    throw remoteException;
                }
            }

            DropboxFolderResult dropboxFolderResult = response.body();
            RemoteChanges changes = new RemoteChanges();
            Map<String, String> idsByPath = new HashMap<>();
            do {
                if (null != dropboxFolderResult.entries) {
                    if (null != listener) {
                        listener.onSetMax(0, changes.getChanges().size() + dropboxFolderResult.entries.size());
                        listener.pauseIfNeeded();
                        if (listener.isCanceled()) {
                            throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                        }
                    }
                    for (DropboxMetadata childMetadata : dropboxFolderResult.entries) {
                        DropboxDocument document = new DropboxDocument(this, accountName, childMetadata);
                        //Logger.d(TAG, "idsByPath : "+document.getPath()+" => "+document.getId());
                        idsByPath.put(document.getPath(), document.getId());
                        document.setParentId(idsByPath.get(document.getParentPath()));
                        if ("deleted".equals(childMetadata.tag)) {
                            if (null!=childMetadata.id) {
                                changes.putChange(childMetadata.id, RemoteChange.deletion(childMetadata.id, document));
                            } else {
                                //TODO : get the real id ?
                                changes.putChange(document.getPath(), RemoteChange.deletion(document.getPath(), document));
                            }
                        } else {
                            changes.putChange(childMetadata.id, RemoteChange.modification(childMetadata.id, document));
                        }
                        if (null != listener) {
                            listener.onProgress(0, changes.getChanges().size());
                            listener.pauseIfNeeded();
                            if (listener.isCanceled()) {
                                throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                            }
                        }
                    }
                }
                if (null!=dropboxFolderResult.cursor) {
                    changes.setLastChangeId(dropboxFolderResult.cursor);
                }
                if (dropboxFolderResult.has_more) {
                    response = apiService.listFolderContinue(account.getAuthHeader(),
                            new ListFolderContinueArg(dropboxFolderResult.cursor)).execute();

                    if (response.isSuccess()) {
                        dropboxFolderResult = response.body();
                    } else {
                        throw remoteException(account, response, "Failed to get changes");
                    }
                } else {
                    dropboxFolderResult = null;
                }
            } while (null!=dropboxFolderResult);
            return changes;
        } catch (IOException e) {
            throw new RemoteException("Failed to get changes", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public void deleteFolder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        deleteDocument(accountName, id);
    }

    @Override
    public void deleteFile(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        deleteDocument(accountName, id);
    }

    /**
     * Sends a request to delete a remote document.
     *
     * @param accountName the account user name
     * @param id          the id of the document to delete
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void deleteDocument(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<DropboxMetadata> response = apiService.getMetadata(account.getAuthHeader(), new GetMetadataArg(id)).execute();
            if (response.isSuccess()) {
                response = apiService.delete(account.getAuthHeader(),
                        new PathArg(response.body().path_lower)).execute();
                if (response.isSuccess()) {
                    return;
                }
            }
            RemoteException remoteException = remoteException(account, response, "Failed to delete document");
            if (remoteException.getReason() != RemoteException.Reason.NotFound) {
                throw remoteException;
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to delete document", RemoteException.Reason.NetworkError);
        }
    }
}