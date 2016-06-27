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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.db.DatabaseConstants;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * This class holds information about Cloud accounts.
 *
 * <p>It also performs various tasks related to account management.
 *
 * @author Pierre Sagne
 * @since 17.07.2015
 */
@DatabaseTable(tableName = DatabaseConstants.ACCOUNTS_TABLE)
public class Account {
    /** The logger used by this class */
    private static Logger LOG = LoggerFactory.getLogger(Account.class);

    /* This class dependencies */
    private AppContext appContext = null;
    private Accounts accounts = null;
    private Crypto crypto = null;
    private CloudAppKeys cloudAppKeys = null;
    private TextI18n textI18n = null;
    private Database database = null;

    /** The id of this account in the database */
    @DatabaseField(generatedId=true, columnName = DatabaseConstants.ACCOUNT_COLUMN_ID)
    private long id;

    /** The StorageType of this account (Google Drive, Dropbox...) */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_STORAGE_TYPE)
    private StorageType storageType;

    /** The user name (usually e-mail address) associated to this account */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_NAME)
    private String accountName;

    /** The OAuth2 access token, used for each API call */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_ACCESS_TOKEN, width=2048)
    private String accessToken;

    /** The expiration date (in ms since the epoch) of the current OAuth2 access token */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_EXPIRATION_TIME)
    private long accessTokenExpirationTime;

    /** The OAuth2 refresh token, used to obtain a new access token, when the current one expires */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_REFRESH_TOKEN, width=2048)
    private String refreshToken;

    /**
     * The date (in ms since the epoch) of the last time a "Too Many Requests" error was returned
     * for this account.
     */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_LAST_TOO_MANY_REQUESTS_ERROR_TIME)
    private long lastTooManyRequestErrorTime;

    /**
     * The delay (relative to {@code lastTooManyRequestErrorTime} before retrying any call
     * on this account, if we encountered a "Too Many Request" error.
     */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_NEXT_RETRY_DELAY)
    private long nextRetryDelay;

    /** The remote "root" folder id of this account, where all the remote files are stored */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_ROOT_FOLDER_ID)
    private String rootFolderId;

    /**
     * The alias of the default key used to encrypt the documents in this account
     */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_DEFAULT_KEY_ALIAS)
    private String defaultKeyAlias;

    /** The id of the last remote changes query. */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_LAST_REMOTE_CHANGE_ID)
    private String lastRemoteChangeId;

    /** The total size (in bytes) of the remote storage allocated to this account */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_QUOTA_AMOUNT)
    private long quotaAmount;

    /** The size (in bytes) taken by the files stored on this account */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_QUOTA_USED)
    private long quotaUsed;

    /** The estimated size (in bytes) taken by the files stored on this account */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_ESTIMATED_QUOTA_USED)
    private long estimatedQuotaUsed;

    /**
     * The "changes synchronization state" of this account :
     * whether it is planned, running, failed or done
     */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_CHANGES_SYNC_STATE)
    private State changesSyncState;

    /** If this account uses OpenStack (like HubiC accounts), the OpenStack access token */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_OPENSTACK_ACCESS_TOKEN, width=2048)
    private String openStackAccessToken;

    /**
     * If this account uses OpenStack (like HubiC accounts),
     * the expiration date (in ms since the epoch) of the current OpenStack access token
     */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_OPENSTACK_ACCESS_TOKEN_EXPIRATION_TIME)
    private long openStackAccessTokenExpirationTime;

    /**
     * If this account uses OpenStack (like HubiC accounts),
     * the current OpenStack API endpoint
     */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_OPENSTACK_ENDPOINT)
    private String openStackEndPoint;

    /**
     * If this account uses OpenStack (like HubiC accounts),
     * the current OpenStack account user name
     */
    @DatabaseField(columnName = DatabaseConstants.ACCOUNT_COLUMN_OPENSTACK_ACCOUNT)
    private String openStackAccount;

    /**
     * Creates a new instance with default values.
     *
     * <p>Dependencies have to be set later, with the {@link Account#setDependencies} method.
     */
    Account() {
        id = 0;
        storageType = null;
        accountName = null;
        accessToken = null;
        accessTokenExpirationTime = 0;
        refreshToken = null;
        lastTooManyRequestErrorTime = 0;
        nextRetryDelay = 0;
        rootFolderId = null;
        defaultKeyAlias = null;
        lastRemoteChangeId = null;
        quotaAmount = -1;
        quotaUsed = -1;
        estimatedQuotaUsed = -1;
        changesSyncState = State.Done;
        openStackAccessToken = null;
        openStackAccessTokenExpirationTime = -1;
        openStackEndPoint = null;
        openStackAccount = null;
    }

    /**
     * Creates a new instance, specifying its dependencies.
     *
     * @param appContext   the AppContext instance
     * @param accounts     the Accounts instance
     * @param crypto       the Crypto instance
     * @param cloudAppKeys the CloudAppKeys instance
     * @param textI18n     the TextI18N instance
     * @param database     the Database instance
     */
    Account(AppContext appContext, Accounts accounts, Crypto crypto, CloudAppKeys cloudAppKeys,
            TextI18n textI18n, Database database) {
        this();
        setDependencies(appContext, accounts, crypto, cloudAppKeys, textI18n, database);
    }

    /**
     * Sets the dependencies needed by this instance to perform its tasks.
     *
     * @param appContext   the AppContext instance
     * @param accounts     the Accounts instance
     * @param crypto       the Crypto instance
     * @param cloudAppKeys the CloudAppKeys instance
     * @param textI18n     the TextI18N instance
     * @param database     the Database instance
     */
    public void setDependencies(AppContext appContext, Accounts accounts, Crypto crypto, CloudAppKeys cloudAppKeys,
                                TextI18n textI18n, Database database) {
        this.appContext = appContext;
        this.accounts = accounts;
        this.crypto = crypto;
        this.cloudAppKeys = cloudAppKeys;
        this.textI18n = textI18n;
        this.database = database;
    }

    /**
     * Sets the dependencies needed by this account to perform its tasks,
     * from another existing instance.
     *
     * @param account the other account to get dependencies from
     */
    public void setDependenciesFrom(Account account) {
        setDependencies(account.appContext, account.accounts, account.crypto,
                account.cloudAppKeys, account.textI18n, account.database);
    }

    /**
     * {@inheritDoc}
     * This implementation returns true if the other object is and an Account instance too,
     * and if the ids of both accounts are equal,
     * or if they have the same storageType and accountName.
     *
     * @param obj the object this account is compared to
     * @return true if this account is considered equal to the object parameter, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Account other = (Account) obj;
        if (id == other.id || storageText().equals(other.storageText())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * This implementation simply returns a hash of the result of {@link Account#storageText}
     */
    @Override
    public int hashCode() {
        return storageText().hashCode();
    }

    /**
     * Returns the database id of this account (-1 if it has not been inserted in the database).
     *
     * @return the database id of this account
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the storage type of this account.
     *
     * @param storageType the storage type of this account
     */
    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    /**
     * Returns the storage type of this account.
     *
     * @return the storage type of this account
     */
    public StorageType getStorageType() {
        return storageType;
    }

    /**
     * Returns this account description (Storage type translation and storage name if not null).
     *
     * @return the description
     */
    public String storageText() {
        return textI18n.getStorageText(getStorageType(), this);
    }

    /**
     * Sets the user name associated with this account.
     *
     * @param accountName the user name associated with this account
     */
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    /**
     * Returns the user name associated with this account.
     *
     * @return the user name associated with this account
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Sets the OAuth2 access token.
     *
     * @param accessToken the access token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Returns the OAuth2 access token.
     *
     * @return the OAuth2 access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Returns the HTTP auth header based on the access token.
     *
     * @return the HTTP auth header based on the access token
     */
    public String getAuthHeader() {
        return "Bearer "+accessToken;
    }

    /**
     * Sets the OAuth2 access token expiration time (in ms from the epoch).
     *
     * @param accessTokenExpirationTime the OAuth2 access token expiration time (in ms from the epoch)
     */
    public void setAccessTokenExpirationTime(long accessTokenExpirationTime) {
        this.accessTokenExpirationTime = accessTokenExpirationTime;
    }

    /**
     * Sets the OAuth2 access token expiration time (in s from the current time).
     *
     * @param seconds the OAuth2 access token expiration time (in s from the current time)
     */
    public void setExpiresInSeconds(int seconds) {
        accessTokenExpirationTime = System.currentTimeMillis() + seconds * 1000;
    }

    /**
     * Sets the OAuth2 access token expiration time (in ms from the current time).
     *
     * @param millis the OAuth2 access token expiration time (in ms from the current time)
     */
    public void setExpiresInMillis(int millis) {
        accessTokenExpirationTime = System.currentTimeMillis() + millis;
    }

    /**
     * Returns the OAuth2 access token expiration time (in ms from the epoch).
     *
     * @return the OAuth2 access token expiration time (in ms from the epoch)
     */
    public long getAccessTokenExpirationTime() {
        return accessTokenExpirationTime;
    }

    /**
     * Returns whether the OAuth2 access token is expired.
     *
     * @return true if the OAuth2 access token is expired, false otherwise
     */
    public boolean isAccessTokenExpired() {
        return System.currentTimeMillis() >= accessTokenExpirationTime;
    }

    /**
     * Sets the OAuth2 refresh token.
     *
     * @param refreshToken the OAuth2 refresh token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Returns the OAuth2 refresh token.
     *
     * @return the OAuth2 refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the last time a "Too many requests" error (in ms from the epoch) was received.
     *
     * @param lastTooManyRequestErrorTime the last time a "Too many requests" error (in ms from the epoch) was received
     */
    public void setLastTooManyRequestErrorTime(long lastTooManyRequestErrorTime) {
        this.lastTooManyRequestErrorTime = lastTooManyRequestErrorTime;
    }

    /**
     * Returns the last time a "Too many requests" error (in ms from the epoch) was received.
     *
     * @return the last time a "Too many requests" error (in ms from the epoch) was received
     */
    public long getLastTooManyRequestErrorTime() {
        return lastTooManyRequestErrorTime;
    }

    /**
     * Returns whether a "Too many requests" error is registered.
     *
     * @return true if a "Too many requests" error is registered
     */
    public boolean tooManyRequestError() {
        return lastTooManyRequestErrorTime > 0;
    }

    /**
     * Sets the delay (in ms from {@code lastTooManyRequestErrorTime} before retrying any call
     * on this account, if we encountered a "Too Many Request" error.
     *
     * @param nextRetryDelay the next retry delay
     */
    public void setNextRetryDelay(long nextRetryDelay) {
        this.nextRetryDelay = nextRetryDelay;
    }

    /**
     * Returns the delay (in ms from {@code lastTooManyRequestErrorTime} before retrying any call
     * on this account, if we encountered a "Too Many Request" error.
     *
     * @return the next retry delay
     */
    public long getNextRetryDelay() {
        return nextRetryDelay;
    }

    /**
     * Sets the root folder id.
     *
     * @param rootFolderId the root folder id
     */
    public void setRootFolderId(String rootFolderId) {
        this.rootFolderId = rootFolderId;
    }

    /**
     * Returns the root folder id.
     *
     * @return the root folder id
     */
    public String getRootFolderId() {
        return rootFolderId;
    }

    /**
     * Sets the alias of the default key used to encrypt the documents stored in this account.
     *
     * @param defaultKeyAlias the alias of the default key used to encrypt the documents stored in this account
     */
    public void setDefaultKeyAlias(String defaultKeyAlias) {
        this.defaultKeyAlias = defaultKeyAlias;
    }

    /**
     * Returns the alias of the the default key used to encrypt the documents stored in this account.
     *
     * @return the alias of the the default key used to encrypt the documents stored in this account
     */
    public String getDefaultKeyAlias() {
        return defaultKeyAlias;
    }

    /**
     * Sets the last remote change id.
     *
     * @param lastRemoteChangeId the last remote change id
     */
    public void setLastRemoteChangeId(String lastRemoteChangeId) {
        this.lastRemoteChangeId = lastRemoteChangeId;
    }

    /**
     * Returns the last remote change id.
     *
     * @return the last remote change id
     */
    public String getLastRemoteChangeId() {
        return lastRemoteChangeId;
    }

    /**
     * Sets the total size (in bytes) of the remote storage allocated to this account
     *
     * @param quotaAmount the total size (in bytes) of the remote storage allocated to this account
     */
    public void setQuotaAmount(long quotaAmount) {
        this.quotaAmount = quotaAmount;
    }

    /**
     * Returns the total size (in bytes) of the remote storage allocated to this account
     *
     * @return the total size (in bytes) of the remote storage allocated to this account
     */
    public long getQuotaAmount() {
        return quotaAmount;
    }

    /**
     * Sets the size (in bytes) taken by the files stored on this account.
     *
     * @param quotaUsed the size (in bytes) taken by the files stored on this account
     */
    public void setQuotaUsed(long quotaUsed) {
        this.quotaUsed = quotaUsed;
        this.estimatedQuotaUsed = quotaUsed;
    }

    /**
     * Returns the size (in bytes) taken by the files stored on this account.
     *
     * @return the size (in bytes) taken by the files stored on this account
     */
    public long getQuotaUsed() {
        return quotaUsed;
    }

    /**
     * Sets the estimated size (in bytes) taken by the files stored on this account.
     *
     * @param estimatedQuotaUsed the estimated size (in bytes) taken by the files stored on this account
     */
    public void setEstimatedQuotaUsed(long estimatedQuotaUsed) {
        this.estimatedQuotaUsed = estimatedQuotaUsed;
    }

    /**
     * Returns the estimated size (in bytes) taken by the files stored on this account.
     *
     * @return the estimated size (in bytes) taken by the files stored on this account
     */
    public long getEstimatedQuotaUsed() {
        return estimatedQuotaUsed;
    }

    /**
     * Returns the quota text (used / total).
     *
     * @return the quota text
     */
    public String getQuotaText() {
        if (quotaUsed<0 || quotaAmount<0) {
            return "";
        }
        return String.format("%s / %s",
                textI18n.getSizeText(estimatedQuotaUsed),
                textI18n.getSizeText(quotaAmount));
    }

    /**
     * Sets the changes synchronization state.
     *
     * @param changesSyncState the changes synchronization state
     */
    public void setChangesSyncState(State changesSyncState) {
        this.changesSyncState = changesSyncState;
    }

    /**
     * Returns the changes synchronization state.
     *
     * @return the changes synchronization state
     */
    public State getChangesSyncState() {
        return changesSyncState;
    }

    /**
     * Sets the OpenStack access token of this account.
     *
     * @param openStackAccessToken the OpenStack access token of this account
     */
    public void setOpenStackAccessToken(String openStackAccessToken) {
        this.openStackAccessToken = openStackAccessToken;
    }

    /**
     * Returns the OpenStack access token of this account.
     *
     * @return the OpenStack access token of this account
     */
    public String getOpenStackAccessToken() {
        return openStackAccessToken;
    }

    /**
     * Sets the OpenStack endpoint of this account.
     *
     * @param openStackEndPoint the OpenStack endpoint of this account
     */
    public void setOpenStackEndPoint(String openStackEndPoint) {
        this.openStackEndPoint = openStackEndPoint;
    }

    /**
     * Returns the OpenStack endpoint of this account.
     *
     * @return the OpenStack endpoint of this account
     */
    public String getOpenStackEndPoint() {
        return openStackEndPoint;
    }

    /**
     * Sets the OpenStack user name of this account.
     *
     * @param openStackAccount the OpenStack user name of this account
     */
    public void setOpenStackAccount(String openStackAccount) {
        this.openStackAccount = openStackAccount;
    }

    /**
     * Returns the OpenStack user name of this account.
     *
     * @return the OpenStack user name of this account
     */
    public String getOpenStackAccount() {
        return openStackAccount;
    }

    /**
     * Sets the OpenStack access token expiration time (in ms from the epoch) of this account.
     *
     * @param openStackAccessTokenExpirationTime the OpenStack access token expiration time (in ms from the epoch) of this account
     */
    public void setOpenStackAccessTokenExpirationTime(long openStackAccessTokenExpirationTime) {
        this.openStackAccessTokenExpirationTime = openStackAccessTokenExpirationTime;
    }

    /**
     * Returns the OpenStack access token expiration time (in ms from the epoch) of this account.
     *
     * @return the OpenStack access token expiration time (in ms from the epoch) of this account
     */
    public long getOpenStackAccessTokenExpirationTime() {
        return openStackAccessTokenExpirationTime;
    }

    /**
     * Returns whether the OpenStack access token is expired.
     *
     * @return true if the OpenStack access token is expired, false otherwise
     */
    public boolean isOpenStackAccessTokenExpired() {
        return System.currentTimeMillis() >= openStackAccessTokenExpirationTime;
    }

    /**
     * Refreshes the quota data of this account, by calling the underlying API, and save
     * the result to the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     * @throws RemoteException                    if any error occured when calling the API
     */
    public void refreshQuota() throws DatabaseConnectionClosedException, RemoteException {
        RemoteStorage storage = appContext.getRemoteStorage(getStorageType());
        if (null == storage) {
            LOG.error("RemoteStorage instance not found for storage type {}", getStorageType().name());
        } else {
            storage.refreshQuota(this);
        }
    }

    /**
     * Refreshes the quota of this account, if the estimated quota differs too much from the
     * last real quota read.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void refreshQuotaIfNeeded() throws DatabaseConnectionClosedException {
        long quotaUsedDifference = Math.abs(getEstimatedQuotaUsed() - getQuotaUsed());
        if (quotaUsedDifference > Constants.FILE.QUOTA_USED_ESTIMATION_BEFORE_REFRESH) {
            try {
                refreshQuota();
            } catch (RemoteException e) {
                LOG.error("Error while refreshing account quota", e);
            }
        }
    }

    /**
     * Notifies a "Too many requests" error.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void notifyTooManyRequests() throws DatabaseConnectionClosedException {
        /** set the current time as the last "Too Many Requests" error time */
        setLastTooManyRequestErrorTime(System.currentTimeMillis());

        /** set the next delay accordingly, incrementing it each time this method is called */
        if (0==getNextRetryDelay()) {
            setNextRetryDelay(1 + new Random().nextInt(1000));
        } else {
            setNextRetryDelay(2 * getNextRetryDelay() + new Random().nextInt(1000));
        }

        /** persist this account to the Database */
        update();
    }

    /**
     * Computes the next time (in ms from the epoch) we are allowed to retry,
     * after a "Too Many Requests" error, and returns it.
     *
     * @return the next time (in ms from the epoch) we can retry to call the API
     */
    private long nextRequestTime() {
        return getLastTooManyRequestErrorTime() + getNextRetryDelay();
    }

    /**
     * Resets the "Too Many Requests" state, and persist this state to database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void resetTooManyRequests() throws DatabaseConnectionClosedException {
        setLastTooManyRequestErrorTime(0);
        setNextRetryDelay(0);
        update();
    }

    /**
     * Returns whether we still have to wait because of a previous "Too many requests" error.
     *
     * @return if we still have to wait because of a previous "Too many requests" error, false otherwise
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public boolean hasTooManyRequests() throws DatabaseConnectionClosedException {
        if (nextRequestTime() > 0) {
            if (System.currentTimeMillis() < nextRequestTime()) {
                return true;
            } else {
                resetTooManyRequests();
            }
        }
        return false;
    }

    /**
     * Adds this account to the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void add() throws DatabaseConnectionClosedException {
        Account existingAccount = accounts.accountWithTypeAndName(getStorageType(), getAccountName());
        if (null==existingAccount) {
            database.addAccount(this);
        } else {
            id = existingAccount.getId();
            update();
        }
    }

    /**
     * Sets the "changes synchronization state", and persists it to database.
     *
     * @param state the changes synchronization state
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateChangesSyncState(State state) throws DatabaseConnectionClosedException {
        setChangesSyncState(state);
        database.updateAccountChangesSyncState(getId(), getChangesSyncState());
    }

    /**
     * Persists this account to the database (it must already exist).
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void update() throws DatabaseConnectionClosedException {
        database.updateAccount(this);
    }

    /**
     * Refreshes this account with the data of the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void refresh() throws DatabaseConnectionClosedException {
        database.refreshAccount(this);
    }

    /**
     * Deletes this account from the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void delete() throws DatabaseConnectionClosedException {
        database.deleteAccount(this);
    }

    /**
     * Returns the cloud storage type (Google Drive, Dropbox...).
     *
     * @return the cloud storage type
     */
    public RemoteStorage getCloudStorage() {
        return appContext.getRemoteStorage(storageType);
    }
}
