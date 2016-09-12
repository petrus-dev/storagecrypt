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

import com.google.gson.Gson;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.cloud.AbstractRemoteStorage;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.RemoteStorage;
import fr.petrus.lib.core.cloud.appkeys.AppKeys;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.NetworkException;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.cloud.exceptions.UserCanceledException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import fr.petrus.lib.core.Constants;

import fr.petrus.lib.core.cloud.RemoteChange;
import fr.petrus.lib.core.cloud.RemoteChanges;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveChange;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveChanges;
import fr.petrus.lib.core.rest.models.gdrive.error.GoogleDriveError;
import fr.petrus.lib.core.rest.services.gdrive.GoogleDriveAccountsApiService;
import fr.petrus.lib.core.rest.models.OauthTokenResponse;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveAbout;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveItem;
import fr.petrus.lib.core.rest.services.gdrive.GoogleDriveApiService;
import fr.petrus.lib.core.rest.services.gdrive.GoogleDriveRestClient;
import fr.petrus.lib.core.result.ProcessProgressListener;

/**
 * The {@link RemoteStorage} implementation for Google Drive.
 *
 * @author Pierre Sagne
 * @since 10.02.2015
 */
public class GoogleDriveStorage
        extends AbstractRemoteStorage<GoogleDriveStorage, GoogleDriveDocument> {
    private static Logger LOG = LoggerFactory.getLogger(GoogleDriveStorage.class);

    private GoogleDriveApiService apiService;
    private GoogleDriveAccountsApiService accountsApiService;
    private EncryptedDocuments encryptedDocuments;

    /**
     * Creates a new GoogleDriveStorage, providing its dependencies.
     *
     * @param crypto             a {@code Crypto} instance
     * @param cloudAppKeys       a {@code CloudAppKeys} instance
     * @param accounts           a {@code Accounts} instance
     * @param encryptedDocuments a {@code} EncryptedDocuments instance
     */
    public GoogleDriveStorage(Crypto crypto, CloudAppKeys cloudAppKeys, Accounts accounts,
                              EncryptedDocuments encryptedDocuments) {
        super(crypto, cloudAppKeys, accounts);
        GoogleDriveRestClient client = new GoogleDriveRestClient();
        apiService = client.getApiService();
        accountsApiService = client.getAccountsApiService();
        this.encryptedDocuments = encryptedDocuments;
    }

    /**
     * Returns the {@code GoogleDriveApiService}, used to call the underlying API.
     *
     * @return the {@code GoogleDriveApiService}
     */
    GoogleDriveApiService getApiService() {
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
                    Gson gson = new Gson();
                    Reader reader = new InputStreamReader(response.errorBody().byteStream());
                    try {
                        GoogleDriveError errorBody = gson.fromJson(reader, GoogleDriveError.class);
                        if (!errorBody.error.errors.isEmpty()) {
                            if (errorBody.error.errors.get(0).reason.equals("rateLimitExceeded") ||
                                    errorBody.error.errors.get(0).reason.equals("userRateLimitExceeded")) {
                                return RemoteException.Reason.TooManyRequests;
                            }
                        }
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            LOG.error("Error when closing reader", e);
                        }
                    }
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
                default:
                    return RemoteException.Reason.UnknownError;
            }
        }
        return RemoteException.Reason.NotAnError;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.GoogleDrive;
    }

    @Override
    public String oauthAuthorizeUrl(boolean mobileVersion) throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getGoogleDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        String url = Constants.GOOGLE_DRIVE.OAUTH_URL
                + "?client_id=" + appKeys.getClientId()
                + "&redirect_uri=" + appKeys.getRedirectUri()
                + "&response_type=" + Constants.GOOGLE_DRIVE.RESPONSE_TYPE
                + "&scope=" + Constants.GOOGLE_DRIVE.SCOPE;

        try {
            url += "&state=" + requestCSRFToken();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't generate a CSRF token", e);
        }

        return url;
    }

    @Override
    public String oauthAuthorizeRedirectUri() throws RemoteException {
        AppKeys appKeys = cloudAppKeys.getGoogleDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }
        return appKeys.getRedirectUri();
    }

    @Override
    public Account connectWithAccessCode(Map<String, String> responseParameters)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {

        AppKeys appKeys = cloudAppKeys.getGoogleDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", responseParameters.get("code"));
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("redirect_uri", appKeys.getRedirectUri());
        params.put("grant_type", Constants.GOOGLE_DRIVE.AUTHORIZATION_CODE_GRANT_TYPE);

        try {
            Response<OauthTokenResponse> response = apiService.getOauthToken(params).execute();
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
            throw new NetworkException("Failed to get oauth token", e);
        }
    }

    @Override
    public String accountNameFromAccessToken(String accessToken) throws RemoteException, NetworkException {
        if (null==accessToken) {
            throw new RemoteException("Failed to get account name : access token is null",
                    RemoteException.Reason.AccessTokenIsNull);
        }

        try {
            Response<GoogleDriveAbout> response = apiService.getAccountInfo("Bearer " + accessToken).execute();
            if (response.isSuccessful()) {
                return response.body().user.emailAddress;
            } else {
                throw new RemoteException("Failed to get account name", retrofitErrorReason(response));
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to get account name", e);
        }
    }

    @Override
    public Account refreshToken(String accountName)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        if (null==accountName) {
            throw new RemoteException("Failed to refresh access token : account name is null",
                    RemoteException.Reason.AccountNameIsNull);
        }

        Account account = account(accountName);
        if (null==account) {
            throw new RemoteException("Failed to refresh access token : account is null",
                    RemoteException.Reason.AppKeysNotFound);
        }

        AppKeys appKeys = cloudAppKeys.getGoogleDriveAppKeys();
        if (null==appKeys) {
            throw new RemoteException("App keys not found", RemoteException.Reason.AppKeysNotFound);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("refresh_token", account.getRefreshToken());
        params.put("client_id", appKeys.getClientId());
        params.put("client_secret", appKeys.getClientSecret());
        params.put("redirect_uri", appKeys.getRedirectUri());
        params.put("grant_type", Constants.GOOGLE_DRIVE.REFRESH_TOKEN_GRANT_TYPE);

        try {
            Response<OauthTokenResponse> response = apiService.getOauthToken(params).execute();
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
            throw new NetworkException("Failed to refresh access token", e);
        }
    }

    @Override
    public Account refreshQuota(Account account)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        try {
            Response<GoogleDriveAbout> response = apiService.getAccountInfo(account.getAuthHeader()).execute();
            if (response.isSuccessful()) {
                GoogleDriveAbout googleDriveAbout = response.body();
                if (null != googleDriveAbout.quotaBytesTotal) {
                    account.setQuotaAmount(googleDriveAbout.quotaBytesTotal);
                }
                if (null != googleDriveAbout.quotaBytesUsed) {
                    if (null != googleDriveAbout.quotaBytesUsedInTrash) {
                        account.setQuotaUsed(googleDriveAbout.quotaBytesUsed + googleDriveAbout.quotaBytesUsedInTrash);
                    } else {
                        account.setQuotaUsed(googleDriveAbout.quotaBytesUsed);
                    }
                }
                account.update();
                return account;
            } else {
                throw remoteException(account, response, "Failed to get quota");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to get quota", e);
        }
    }

    @Override
    public void revokeToken(String accountName)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
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
            Response<ResponseBody> response = accountsApiService.revokeOauthToken(account.getRefreshToken()).execute();
            if (!response.isSuccessful()) {
                throw remoteException(account, response, "Failed to revoke access token");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to revoke access token", e);
        }
    }

    /**
     * Returns the {@code GoogleDriveDocument} which represents the root folder (where all remote
     * files of the account are stored).
     *
     * <p>It is built from the "root folder id" which is stored in the account.
     *
     * @param account the account the root folder is associated to.
     * @return the root folder associated to the specified account.
     */
    private GoogleDriveDocument getRootFolder(Account account) {
        GoogleDriveDocument document = new GoogleDriveDocument(this);
        document.setAccountName(account.getAccountName());
        document.setName("");
        document.setMimeType(Constants.GOOGLE_DRIVE.FOLDER_MIME_TYPE);
        document.setFolder(true);
        document.setId(account.getRootFolderId());
        return document;
    }

    @Override
    public GoogleDriveDocument rootFolder(String accountName)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = refreshedAccount(accountName);
        if (null == account.getRootFolderId()) {
            try {
                Response<GoogleDriveAbout> response = apiService.getAccountInfo(account.getAuthHeader()).execute();
                if (response.isSuccessful()) {
                    account.setRootFolderId(response.body().rootFolderId);
                    account.update();
                } else {
                    throw remoteException(account, response, "Failed to get account information");
                }
            } catch (IOException | RuntimeException e) {
                throw new NetworkException("Failed to get account information", e);
            }
        }

        return getRootFolder(account);
    }

    @Override
    public GoogleDriveDocument folder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        GoogleDriveDocument document = document(accountName, id);
        if (!document.isFolder()) {
            throw new RemoteException("Failed to get folder : the document found is not a folder",
                    RemoteException.Reason.NotAFolder);
        }
        return document;
    }

    @Override
    public GoogleDriveDocument file(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        GoogleDriveDocument document = document(accountName, id);
        if (document.isFolder()) {
            throw new RemoteException("Failed to get file : the document found is not a file",
                    RemoteException.Reason.NotAFile);
        }
        return document;
    }

    @Override
    public GoogleDriveDocument document(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = refreshedAccount(accountName);
        try {
            Response<GoogleDriveItem> response = apiService.getItem(account.getAuthHeader(), id).execute();
            if (response.isSuccessful()) {
                return new GoogleDriveDocument(this, accountName, response.body());
            } else {
                throw remoteException(account, response, "Failed to get document");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to get document", e);
        }
    }

    @Override
    public RemoteChanges changes(String accountName, String lastChangeId, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException, UserCanceledException {

        Account account = refreshedAccount(accountName);

        if (null==lastChangeId) {
            return getRecursiveChanges(account, listener);
        }

        RemoteChanges changes = new RemoteChanges();
        long largestChangeId = -1L;
        String nextPageToken = null;
        do {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("startChangeId", lastChangeId);
            if (null != nextPageToken) {
                params.put("pageToken", nextPageToken);
            }

            try {
                Response<GoogleDriveChanges> response = apiService.getChanges(account.getAuthHeader(), params).execute();
                if (response.isSuccessful()) {
                    GoogleDriveChanges googleDriveChanges = response.body();
                    if (null!=googleDriveChanges.largestChangeId) {
                        LOG.debug("Largest change ID = {}", googleDriveChanges.largestChangeId);
                        if (googleDriveChanges.largestChangeId > largestChangeId) {
                            largestChangeId = googleDriveChanges.largestChangeId;
                            changes.setLastChangeId(String.valueOf(largestChangeId));
                        }
                    }

                    if (null != googleDriveChanges.items) {
                        //suppress the changes which value equals lastChangeId
                        for (Iterator<GoogleDriveChange> it = googleDriveChanges.items.iterator(); it.hasNext(); ) {
                            GoogleDriveChange googleDriveChange = it.next();
                            if (lastChangeId.equals(String.valueOf(googleDriveChange.id))) {
                                it.remove();
                            }
                        }
                        if (null!=listener) {
                            listener.onSetMax(0, changes.getChanges().size() + googleDriveChanges.items.size());
                            listener.pauseIfNeeded();
                            if (listener.isCanceled()) {
                                throw new UserCanceledException("Canceled");
                            }
                        }
                        for (GoogleDriveChange googleDriveChange : googleDriveChanges.items) {
                            String documentId = null;
                            if (null!=googleDriveChange.fileId) {
                                documentId = googleDriveChange.fileId;
                            }
                            if (null!=googleDriveChange.deleted && googleDriveChange.deleted) {
                                if (null!=documentId) {
                                    changes.addChange(RemoteChange.deletion(documentId));
                                }
                            } else {
                                if (null!=googleDriveChange.file) {
                                    documentId = googleDriveChange.file.id;
                                    if (null!=documentId) {
                                        changes.addChange(RemoteChange.modification(
                                                new GoogleDriveDocument(this, accountName,
                                                        googleDriveChange.file)));
                                    }
                                }
                            }

                            if (null!=listener) {
                                listener.onProgress(0, changes.getChanges().size());
                                listener.pauseIfNeeded();
                                if (listener.isCanceled()) {
                                    throw new UserCanceledException("Canceled");
                                }
                            }
                        }
                    }
                    nextPageToken = googleDriveChanges.nextPageToken;
                } else {
                    throw remoteException(account, response, "Failed to get changes");
                }
            } catch (IOException | RuntimeException e) {
                throw new NetworkException("Failed to get changes", e);
            }
        } while (nextPageToken!=null);

        //build the list of folders from the changes list
        Map<String, RemoteDocument> folders = new HashMap<>();
        for (RemoteChange remoteChange : changes.getChanges()) {
            if (!remoteChange.isDeleted()) {
                RemoteDocument remoteDocument = remoteChange.getDocument();
                if (remoteDocument.isFolder()) {
                    folders.put(remoteDocument.getId(), remoteDocument);
                }
            }
        }

        //remove changes which are not in the app folder
        for (Iterator<RemoteChange> it = changes.getChanges().iterator(); it.hasNext(); ) {
            if (null!=listener) {
                listener.onProgress(0, changes.getChanges().size());
                listener.onSetMax(0, changes.getChanges().size());
                listener.pauseIfNeeded();
                if (listener.isCanceled()) {
                    throw new UserCanceledException("Canceled");
                }
            }
            RemoteChange change = it.next();
            if (change.isDeleted()) {
                EncryptedDocument encryptedDocument = encryptedDocuments.encryptedDocumentWithAccountAndEntryId(account,
                        change.getDocumentId());
                // if the change is the deletion of a document which is not present, remove it from the list
                if (null== encryptedDocument) {
                    it.remove();
                }
            } else {
                // if this is not a deletion and we don't have the document object, remove it from the list
                // TODO : get the change?
                if (null==change.getDocument()) {
                    it.remove();
                } else {
                    if (!isInAppFolderTree(account, change.getDocument(), folders)) {
                        it.remove();
                    }
                }
            }
        }
        return changes;
    }

    /**
     * Returns the remote changes for an account.
     *
     * <p>The remote changes list is built recursively from the root folder.
     *
     * @param account the account for which the remote changes are listed
     * @param listener  a listener which allows to track the progress, and cancel/pause it
     * @return the remote changes
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private RemoteChanges getRecursiveChanges(Account account, ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException, UserCanceledException {
        RemoteChanges changes = new RemoteChanges();

        // get latest change
        Map<String, String> params = new LinkedHashMap<>();
        try {
            Response<GoogleDriveChanges> response = apiService.getChanges(account.getAuthHeader(), params).execute();
            if (response.isSuccessful()) {
                GoogleDriveChanges googleDriveChanges = response.body();
                if (null!=googleDriveChanges.largestChangeId) {
                    LOG.debug("Largest change ID = {}", googleDriveChanges.largestChangeId);
                    changes.setLastChangeId(String.valueOf(googleDriveChanges.largestChangeId));
                }

                // then build the changes list recursively for the first run
                RemoteDocument appFolder = appFolder(account.getAccountName());
                changes.addChange(RemoteChange.modification(appFolder));
                appFolder.getRecursiveChanges(changes, listener);

                return changes;
            } else {
                throw remoteException(account, response, "Failed to get changes");
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to get changes", e);
        }
    }

    /**
     * Returns whether a {@code RemoteDocument} is part of the children tree of the app folder
     * of the given account.
     *
     * <p>The provided {@code changes} helps building the children tree without additional queries.
     *
     * @param account  the account where the {@code document} is stored
     * @param document the document to test
     * @param folders  the remote folders from the changes used to build a tree of the {@code document} parents
     * @return true if the document is part of the children tree of the app folder of the given
     *         {@code account}, or false otherwise
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private boolean isInAppFolderTree(Account account, RemoteDocument document, Map<String, RemoteDocument> folders)
        throws DatabaseConnectionClosedException {

        if (null!=encryptedDocuments.encryptedDocumentWithAccountAndEntryId(account, document.getId())) {
            return true;
        }
        for (String parentId : ((GoogleDriveDocument)document).getParentIds()) {
            EncryptedDocument parentEncryptedDocument =
                    encryptedDocuments.encryptedDocumentWithAccountAndEntryId(account, parentId);
            if (null!= parentEncryptedDocument &&
                    !parentEncryptedDocument.getBackEntryId().equals(account.getRootFolderId())) {
                return true;
            }
            RemoteDocument parentRemoteDocument = folders.get(parentId);
            if (null!=parentRemoteDocument && isInAppFolderTree(account, parentRemoteDocument, folders)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteFolder(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        deleteDocument(accountName, id);
    }

    @Override
    public void deleteFile(String accountName, String id)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
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
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        Account account = refreshedAccount(accountName);
        try {
            Response<ResponseBody> response = apiService.deleteItem(account.getAuthHeader(), id).execute();
            if (!response.isSuccessful()) {
                RemoteException remoteException = remoteException(account, response, "Failed to delete document");
                if (!remoteException.isNotFoundError()) {
                    throw remoteException;
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new NetworkException("Failed to delete document", e);
        }
    }
}