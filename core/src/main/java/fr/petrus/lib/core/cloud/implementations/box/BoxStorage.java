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
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.RemoteChange;
import fr.petrus.lib.core.cloud.RemoteChanges;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.rest.models.box.BoxItem;
import fr.petrus.lib.core.rest.models.box.BoxUser;
import fr.petrus.lib.core.rest.models.OauthTokenResponse;
import fr.petrus.lib.core.rest.services.box.BoxApiService;
import fr.petrus.lib.core.rest.services.box.BoxRestClient;
import fr.petrus.lib.core.rest.services.box.BoxUploadApiService;
import fr.petrus.lib.core.result.ProcessProgressListener;

/**
 * The {@link RemoteStorage} implementation for Box.com.
 *
 * @author Pierre Sagne
 * @since 10.02.2015
 */
public class BoxStorage extends AbstractRemoteStorage<BoxStorage, BoxDocument> {
    private static Logger LOG = LoggerFactory.getLogger(BoxStorage.class);

    private BoxApiService apiService;
    private BoxUploadApiService uploadApiService;

    /**
     * Creates a new BoxStorage, providing its dependencies.
     *
     * @param crypto       a {@code Crypto} instance
     * @param cloudAppKeys a {@code CloudAppKeys} instance
     * @param accounts     a {@code Accounts} instance
     */
    public BoxStorage(Crypto crypto, CloudAppKeys cloudAppKeys, Accounts accounts) {
        super(crypto, cloudAppKeys, accounts);
        BoxRestClient client = new BoxRestClient();
        apiService = client.getApiService();
        uploadApiService = client.getUploadApiService();
    }

    /**
     * Returns the {@code BoxApiService}, used to call the underlying API.
     *
     * @return the {@code BoxApiService}
     */
    BoxApiService getApiService() {
        return apiService;
    }

    /**
     * Returns the {@code BoxUploadApiService}, used to call the underlying API.
     *
     * @return the {@code BoxUploadApiService}
     */
    BoxUploadApiService getUploadApiService() {
        return uploadApiService;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.Box;
    }

    @Override
    public String oauthAuthorizeUrl(boolean mobileVersion) throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getBoxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        String url = Constants.BOX.OAUTH_URL
                + "?client_id=" + appKeys.getClientId()
                + "&redirect_uri=" + appKeys.getRedirectUri()
                + "&response_type=" + Constants.BOX.RESPONSE_TYPE;

        try {
            url += "&state=" + requestCSRFToken();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't generate a CSRF token", e);
        }

        return url;
    }

    @Override
    public String oauthAuthorizeRedirectUri() throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getBoxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }
        return appKeys.getRedirectUri();
    }

    @Override
    public Account connectWithAccessCode(Map<String, String> responseParameters)
            throws DatabaseConnectionClosedException, RemoteException {

        AppKeys appKeys = cloudAppKeys.getBoxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", responseParameters.get("code"));
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("redirect_uri", appKeys.getRedirectUri());
        params.put("grant_type", Constants.BOX.AUTHORIZATION_CODE_GRANT_TYPE);


        try {
            Response<OauthTokenResponse> response = apiService.getOauthToken(params).execute();
            if (response.isSuccess()) {
                OauthTokenResponse oauthTokenResponse = response.body();
                try {
                    String accountName = accountNameFromAccessToken(oauthTokenResponse.access_token);

                    Account account = createAccount();
                    account.setAccountName(accountName);
                    account.setAccessToken(oauthTokenResponse.access_token);
                    account.setExpiresInSeconds(oauthTokenResponse.expires_in);
                    account.setRefreshToken(oauthTokenResponse.refresh_token);
                    accounts.add(account);
                    return account;
                } catch (RemoteException e) {
                    throw new RemoteException("Failed to get account name", e.getReason(), e);
                }
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
            Response<BoxUser> response = apiService.getAccountInfo("Bearer " + accessToken).execute();
            if (response.isSuccess()) {
                return response.body().login;
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
        if (null==accountName) {
            throw new RemoteException("Failed to refresh access token : account name is null",
                    RemoteException.Reason.AccountNameIsNull);
        }

        Account account = account(accountName);
        if (null==account) {
            throw new RemoteException("Failed to refresh access token : account is null",
                    RemoteException.Reason.AccountNotFound);
        }

        AppKeys appKeys = cloudAppKeys.getBoxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("refresh_token", account.getRefreshToken());
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("redirect_uri", appKeys.getRedirectUri());
        params.put("grant_type", Constants.BOX.REFRESH_TOKEN_GRANT_TYPE);

        try {
            Response<OauthTokenResponse> response = apiService.getOauthToken(params).execute();
            if (response.isSuccess()) {
                OauthTokenResponse oauthTokenResponse = response.body();
                if (null != oauthTokenResponse.access_token) {
                    account.setAccessToken(oauthTokenResponse.access_token);
                }
                if (null != oauthTokenResponse.expires_in) {
                    account.setExpiresInSeconds(oauthTokenResponse.expires_in);
                }
                if (null != oauthTokenResponse.refresh_token) {
                    account.setRefreshToken(oauthTokenResponse.refresh_token);
                }
                account.update();

                return account;
            } else {
                throw remoteException(account, response, "Failed to refresh access token");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to refresh access token", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public Account refreshQuota(Account account)
            throws DatabaseConnectionClosedException, RemoteException {
        try {
            Response<BoxUser> response = apiService.getAccountInfo(account.getAuthHeader()).execute();
            if (response.isSuccess()) {
                BoxUser user = response.body();
                if (null != user.space_amount) {
                    account.setQuotaAmount(user.space_amount);
                }
                if (null != user.space_used) {
                    account.setQuotaUsed(user.space_used);
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

        AppKeys appKeys = cloudAppKeys.getBoxAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("token", account.getRefreshToken());

        try {
            Response<ResponseBody> response = apiService.revokeOauthToken(params).execute();
            if (!response.isSuccess()) {
                throw remoteException(account, response, "Failed to revoke access token");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to revoke access token", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public BoxDocument rootFolder(String accountName)
            throws DatabaseConnectionClosedException, RemoteException {
        return folder(accountName, Constants.BOX.ROOT_FOLDER_ID);
    }

    @Override
    public BoxDocument document(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        BoxDocument boxDocument;
        try {
            boxDocument = file(accountName, id);
        } catch (RemoteException e) {
            if (e.getReason()== RemoteException.Reason.NotFound) {
                boxDocument = folder(accountName, id);
            } else {
                throw e;
            }
        }

        return boxDocument;
    }

    @Override
    public BoxDocument folder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<BoxItem> response = apiService.getFolder(account.getAuthHeader(), id).execute();
            if (response.isSuccess()) {
                return new BoxDocument(this, accountName, response.body());
            } else {
                throw remoteException(account, response, "Failed to get folder");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get folder", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public BoxDocument file(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<BoxItem> response = apiService.getFile(account.getAuthHeader(), id).execute();
            if (response.isSuccess()) {
                return new BoxDocument(this, accountName, response.body());
            } else {
                throw remoteException(account, response, "Failed to get file");
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to get file", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public RemoteChanges changes(String accountName, String lastChangeId, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {

        Account account = refreshedAccount(accountName);
        BoxDocument appFolder = appFolder(account.getAccountName());

        long lastChangeTime = -1L;
        if (null!=lastChangeId) {
            lastChangeTime = Long.parseLong(lastChangeId);
        }
        long lastChangeFoundTime = lastChangeTime;

        LOG.debug("Box changes : lastChangeId = {}, lastChangeTime = {}", lastChangeId, lastChangeTime);

        List<BoxDocument> documents = new ArrayList<>();
        documents.add(appFolder);
        appFolder.getRecursiveChildren(documents, listener);

        RemoteChanges changes = new RemoteChanges();
        for (BoxDocument document : documents) {
            LOG.debug("   - document {} modification time = {}", document.getName(), document.getModificationTime());
            if (document.getModificationTime() > lastChangeFoundTime) {
                LOG.debug("     - document newer than lastChangeFoundTime : update lastChangeFoundTime");
                lastChangeFoundTime = document.getModificationTime();
            }
            if (document.getModificationTime() > lastChangeTime) {
                LOG.debug("     - document newer than lastChangeTime ({}) : add it", document.getModificationTime() - lastChangeTime);
                changes.putChange(document.getId(), RemoteChange.modification(document));
            } else {
                LOG.debug("     - document older than lastChangeTime ({}) : ignore it", document.getModificationTime() - lastChangeTime);
            }
        }

        LOG.debug("lastChangeFoundTime = {}", lastChangeFoundTime);

        changes.setLastChangeId(String.valueOf(lastChangeFoundTime));
        return changes;
    }

    // At this time, this implementation is broken.
    // TODO: It would be more efficient, so try to fix it.
    /*@Override
    public RemoteChanges changes(String accountName, String lastChangeId, ProcessProgressListener listener)
        throws DatabaseConnectionClosedException, RemoteException {

        Account account = refreshedAccount(accountName);

        RemoteChanges changes = new RemoteChanges();
        changes.setLastChangeId(lastChangeId);

        long lastChangeTime = -1L;
        String startChangeTime = null;
        if (null!=lastChangeId) {
            lastChangeTime = Long.parseLong(lastChangeId);
            DateTime dateTime = new DateTime(lastChangeTime);
            DateTimeFormatter fmt = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ssZZ");//.withZone(DateTimeZone.forOffsetHours(-8));
            startChangeTime = fmt.print(dateTime);
            //startChangeTime = dateTime.toDateTimeISO().toString();
        }
        long latestChangeTime = -1L;

        int total_count;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("query", Constants.BOX.DESCRIPTION_STRING);
        //TODO : use this again when search is fixed
        if (null!=lastChangeId) {
            //params.put("created_at_range", startChangeTime + ",");
            params.put("updated_at_range", startChangeTime + ",");
        }

        LOG.debug("Box changes : lastChangeId = {}, lastChangeTime = {}", lastChangeId, lastChangeTime);

        int offset = 0;
        do {
            if (0!=offset) {
                params.put("offset", String.valueOf(offset));
            }

            try {
                Response<BoxItems> response = apiService.searchItems(account.getAuthHeader(), params).execute();
                if (response.isSuccess()) {
                    BoxItems boxItems = response.body();
                    if (null != boxItems.entries) {
                        LOG.debug("Found {} change entries", boxItems.entries.size());
                        for (BoxItem entry : boxItems.entries) {
                            BoxDocument document = new BoxDocument(accountName, entry);
                            LOG.debug(" - document {} : folder = {},  modification time = {}",
                                    entry.id, document.isFolder(), document.getModificationTime());
                            if (document.getModificationTime()>latestChangeTime) {
                                LOG.debug("   - document newer than latestChangeTime : update latestChangeTime");
                                latestChangeTime = document.getModificationTime();
                                changes.setLastChangeId(String.valueOf(latestChangeTime));
                            }
                            if (document.getModificationTime()>=lastChangeTime) {
                                LOG.debug("   - document newer than lastChangeTime ({}) : add change", document.getModificationTime() - lastChangeTime);
                                changes.putChange(entry.id,
                                        new RemoteChange(false, entry.id, document));
                            } else {
                                LOG.debug("   - document older than lastChangeTime ({}) : ignore", document.getModificationTime() - lastChangeTime);
                            }
                        }
                        if (null!=listener) {
                            listener.onSetMax(0, changes.changes().size());
                            listener.pauseIfNeeded();
                            if (listener.isCanceled()) {
                                throw new RemoteException("Canceled", RemoteException.Reason.UserCanceled);
                            }
                        }
                    }
                    total_count = boxItems.total_count;
                    offset = boxItems.offset + boxItems.limit;
                } else {
                    throw remoteException(account, response, "Failed to get changes");
                }
            } catch (IOException e) {
                throw new RemoteException("Failed to get changes", RemoteException.Reason.NetworkError, e);
            }

        } while (offset < total_count);
        return changes;
    }*/

    @Override
    public void deleteFolder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<ResponseBody> response = apiService.deleteFolder(account.getAuthHeader(), id).execute();
            if (!response.isSuccess()) {
                RemoteException remoteException = remoteException(account, response, "Failed to delete folder");
                if (remoteException.getReason() != RemoteException.Reason.NotFound) {
                    throw remoteException;
                }
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to delete folder", RemoteException.Reason.NetworkError, e);
        }
    }

    @Override
    public void deleteFile(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException {
        Account account = refreshedAccount(accountName);
        try {
            Response<ResponseBody> response = apiService.deleteFile(account.getAuthHeader(), id).execute();
            if (!response.isSuccess()) {
                RemoteException remoteException = remoteException(account, response, "Failed to delete file");
                if (remoteException.getReason() != RemoteException.Reason.NotFound) {
                    throw remoteException;
                }
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to delete file", RemoteException.Reason.NetworkError, e);
        }
    }
}