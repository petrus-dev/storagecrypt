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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.petrus.lib.core.cloud.AbstractRemoteStorage;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteStorage;
import fr.petrus.lib.core.cloud.appkeys.AppKeys;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import okhttp3.ResponseBody;
import retrofit2.Response;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.RemoteChange;
import fr.petrus.lib.core.cloud.RemoteChanges;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.rest.models.OauthTokenResponse;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveAbout;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveDelta;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveItem;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveRoot;
import fr.petrus.lib.core.rest.services.onedrive.OneDriveApiService;
import fr.petrus.lib.core.rest.services.onedrive.OneDriveLiveApiService;
import fr.petrus.lib.core.rest.services.onedrive.OneDriveOauthApiService;
import fr.petrus.lib.core.rest.services.onedrive.OneDriveRestClient;
import fr.petrus.lib.core.result.ProcessProgressListener;

/**
 * The {@link RemoteStorage} implementation for OneDrive.
 *
 * @author Pierre Sagne
 * @since 10.02.2015
 */
public class OneDriveStorage extends AbstractRemoteStorage<OneDriveStorage, OneDriveDocument> {
    private static Logger LOG = LoggerFactory.getLogger(OneDriveStorage.class);

    private OneDriveOauthApiService oauthApiService;
    private OneDriveLiveApiService liveApiService;
    private OneDriveApiService apiService;

    /**
     * Creates a new OneDriveStorage, providing its dependencies.
     *
     * @param crypto       a {@code Crypto} instance
     * @param cloudAppKeys a {@code CloudAppKeys} instance
     * @param accounts     a {@code Accounts} instance
     */
    public OneDriveStorage(Crypto crypto, CloudAppKeys cloudAppKeys, Accounts accounts) {
        super(crypto, cloudAppKeys, accounts);
        oauthApiService = new OneDriveRestClient().getOauthApiService();
        liveApiService = new OneDriveRestClient().getLiveApiService();
        apiService = new OneDriveRestClient().getApiService();
    }

    /**
     * Returns the {@code OneDriveApiService}, used to call the underlying API.
     *
     * @return the {@code OneDriveApiService}
     */
    OneDriveApiService getApiService() {
        return apiService;
    }

    @Override
    public RemoteException.Reason retrofitErrorReason(Response<?> response) {
        if (!response.isSuccessful()) {
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
                    return RemoteException.Reason.Conflict;
                case 412:
                    return RemoteException.Reason.PreconditionFailed;
                case 429:
                    return RemoteException.Reason.TooManyRequests;
                case 500:
                    return RemoteException.Reason.InternalServerError;
                case 503:
                    return RemoteException.Reason.Unavailable;
                case 509:
                    return RemoteException.Reason.TooManyRequests;
                default:
                    return RemoteException.Reason.UnknownError;
            }
        }
        return RemoteException.Reason.NotAnError;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.OneDrive;
    }

    @Override
    public String oauthAuthorizeUrl(boolean mobileVersion) throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getOneDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        String url = Constants.ONE_DRIVE.OAUTH_URL
                + "?client_id=" + appKeys.getClientId()
                + "&redirect_uri=" + appKeys.getRedirectUri()
                + "&response_type=" + Constants.ONE_DRIVE.RESPONSE_TYPE
                + "&scope=" + Constants.ONE_DRIVE.SCOPE;

        if (mobileVersion) {
            url += "&display=touch";
        }

        try {
            url += "&state=" + requestCSRFToken();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't generate a CSRF token", e);
        }

        return url;
    }

    @Override
    public String oauthAuthorizeRedirectUri() throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getOneDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }
        return appKeys.getRedirectUri();
    }

    @Override
    public Account connectWithAccessCode(Map<String, String> responseParameters)
            throws DatabaseConnectionClosedException, RemoteException {

        AppKeys appKeys = cloudAppKeys.getOneDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", responseParameters.get("code"));
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("redirect_uri", appKeys.getRedirectUri());
        params.put("grant_type", Constants.ONE_DRIVE.AUTHORIZATION_CODE_GRANT_TYPE);

        try {
            Response<OauthTokenResponse> response = oauthApiService.getOauthToken(params).execute();
            if (response.isSuccessful()) {
                OauthTokenResponse oauthTokenResponse = response.body();

                String accountName = accountNameFromAccessToken(oauthTokenResponse.access_token);

                Account account = createAccount();
                account.setAccountName(accountName);
                account.setAccessToken(oauthTokenResponse.access_token);
                account.setExpiresInSeconds(oauthTokenResponse.expires_in);
                account.setRefreshToken(oauthTokenResponse.refresh_token);
                accounts.add(account);
                return account;
            } else {
                throw new RemoteException("Failed to get oauth token", retrofitErrorReason(response));
            }
        } catch (IOException | RuntimeException e) {
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
            Response<OneDriveAbout> response = liveApiService.getAccountInfo(accessToken).execute();
            if (response.isSuccessful()) {
                return response.body().emails.account;
            } else {
                throw new RemoteException("Failed to get account name", retrofitErrorReason(response));
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to get account name", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public Account refreshToken(String accountName)
            throws DatabaseConnectionClosedException, RemoteException {
        if (null==accountName) {
            throw new RemoteException("Failed to refresh access token : account name is null",
                    RemoteException.Reason.AccountNameIsNull);
        }

        Account account = account(accountName);
        if (null==account) {
            throw new RemoteException("Failed to refresh access token : account is null",
                    RemoteException.Reason.AccountNotFound);
        }

        AppKeys appKeys = cloudAppKeys.getOneDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("refresh_token", account.getRefreshToken());
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("redirect_uri", appKeys.getRedirectUri());
        params.put("grant_type", Constants.ONE_DRIVE.REFRESH_TOKEN_GRANT_TYPE);

        try {
            Response<OauthTokenResponse> response = oauthApiService.getOauthToken(params).execute();
            if (response.isSuccessful()) {
                OauthTokenResponse oauthTokenResponse = response.body();
                if (null!=oauthTokenResponse.access_token) {
                    account.setAccessToken(oauthTokenResponse.access_token);
                }
                if (null!=oauthTokenResponse.expires_in) {
                    account.setExpiresInSeconds(oauthTokenResponse.expires_in);
                }
                if (null!=oauthTokenResponse.refresh_token) {
                    account.setRefreshToken(oauthTokenResponse.refresh_token);
                }
                account.update();

                return account;
            } else {
                throw remoteException(account, response, "Failed to refresh access token");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to refresh access token", RemoteException.Reason.NetworkError, e);
        }

    }

    @Override
    public Account refreshQuota(Account account)
            throws DatabaseConnectionClosedException, RemoteException {
        try {
            Response<OneDriveRoot> response = apiService.getDriveRoot(account.getAuthHeader()).execute();
            if (response.isSuccessful()) {
                OneDriveRoot root = response.body();
                if (null != root && null != root.quota) {
                    if (null != root.quota.total) {
                        account.setQuotaAmount(root.quota.total);
                    }
                    if (null != root.quota.used) {
                        account.setQuotaUsed(root.quota.used);
                    }
                    account.update();
                }
                return account;
            } else {
                throw remoteException(account, response, "Failed to get quota");
            }
        } catch (IOException | RuntimeException e) {
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

        AppKeys appKeys = cloudAppKeys.getOneDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found",
                    RemoteException.Reason.AppKeysNotFound);
        }

        Account account = account(accountName);
        if (null==account) {
            throw new RemoteException("Failed to revoke access token : account is null",
                    RemoteException.Reason.AccountNotFound);
        }

        try {
            Response<ResponseBody> response = oauthApiService.revokeOauthToken(account.getAuthHeader(),
                    appKeys.getClientId(), appKeys.getRedirectUri()).execute();
            if (!response.isSuccessful()) {
                if (302!=response.code() || !response.headers().get("Location").startsWith(appKeys.getRedirectUri())) {
                    RemoteException remoteException = remoteException(account, response, "Failed to revoke access token");
                    if (remoteException.getReason() != RemoteException.Reason.NotAnError) {
                        throw remoteException;
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to revoke access token", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument rootFolder(String accountName)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<OneDriveRoot> response = apiService.getDriveRoot(account.getAuthHeader()).execute();
            if (response.isSuccessful()) {
                Response<OneDriveItem> itemResponse = apiService.getDocumentById(account.getAuthHeader(),
                        Constants.ONE_DRIVE.ROOT_FOLDER_ID).execute();
                if (itemResponse.isSuccessful()) {
                    return new OneDriveDocument(this, accountName, itemResponse.body());
                } else {
                    throw remoteException(account, response, "Failed to get root folder");
                }
            } else {
                throw remoteException(account, response, "Failed to get root folder");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to get root folder", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public OneDriveDocument file(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        OneDriveDocument document;
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
    public OneDriveDocument folder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        OneDriveDocument document;
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
    public OneDriveDocument document(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<OneDriveItem> response = apiService.getDocumentById(account.getAuthHeader(), id).execute();
            if (response.isSuccessful()) {
                return new OneDriveDocument(this, accountName, response.body());
            } else {
                throw remoteException(account, response, "Failed to get document");
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to get document", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public RemoteChanges changes(String accountName, String lastChangeId, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {

        Account account = refreshedAccount(accountName);

        RemoteChanges changes = new RemoteChanges();

        String token = lastChangeId;
        Map<String, String> params = new HashMap<>();
        OneDriveDelta delta;
        do {
            if (null!=listener) {
                listener.onSetMax(0, changes.getChanges().size());
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                }
            }
            try {
                if (null != token) {
                    params.put("token", token);
                }
                Response<OneDriveDelta> response = apiService.getDeltaByPath(account.getAuthHeader(),
                        "/" + Constants.FILE.APP_DIR_NAME, params).execute();
                if (response.isSuccessful()) {
                    delta = response.body();

                    if (null!=delta.value) {
                        for (OneDriveItem item : delta.value) {
                            OneDriveDocument document = new OneDriveDocument(this, accountName, item);
                            RemoteChange change;
                            if (null!=item.deleted) {
                                change = RemoteChange.deletion(item.id);
                            } else {
                                change = RemoteChange.modification(document);
                            }
                            changes.putChange(item.id, change);
                            if (null!=listener) {
                                listener.onProgress(0, changes.getChanges().size());
                                listener.pauseIfNeeded();
                                if (listener.isCanceled()) {
                                    throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                                }
                            }
                        }
                    }
                    token = delta.token;
                } else {
                    throw remoteException(account, response, "Failed to get changes");
                }
            } catch (IOException | RuntimeException e) {
                throw new RemoteException("Failed to get changes", RemoteException.Reason.NetworkError, e);
            }
        } while (null!=delta.nextLink);

        changes.setLastChangeId(token);

        return changes;
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
            Response<ResponseBody> response = apiService.deleteDocumentById(account.getAuthHeader(), id).execute();
            if (!response.isSuccessful()) {
                RemoteException remoteException = remoteException(account, response, "Failed to delete document");
                if (remoteException.getReason()!= RemoteException.Reason.NotFound) {
                    throw remoteException;
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new RemoteException("Failed to delete document", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public void deleteFile(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        deleteDocument(accountName, id);
    }

    @Override
    public void deleteFolder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        deleteDocument(accountName, id);
    }
}