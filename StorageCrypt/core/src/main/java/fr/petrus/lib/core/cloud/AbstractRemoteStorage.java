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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import retrofit2.Response;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import fr.petrus.lib.core.Constants;

/**
 * This abstract class implements the methods which are the same for all implementations of the
 * {@code RemoteStorage} interface.
 *
 * @param <S> the {@link RemoteStorage} implementation
 * @param <D> the {@link RemoteDocument} implementation
 * @author Pierre Sagne
 * @since 17.02.2015
 */
public abstract class AbstractRemoteStorage
        <S extends RemoteStorage<S, D>, D extends RemoteDocument<S, D>>
        implements RemoteStorage<S, D> {

    private static Logger LOG = LoggerFactory.getLogger(AbstractRemoteStorage.class);

    /**
     * The Crypto instance, used to perform cryptographic tasks.
     */
    protected Crypto crypto = null;

    /**
     * The CloudAppKeys instance, which stores the underlying API keys
     */
    protected CloudAppKeys cloudAppKeys = null;

    /**
     * The Accounts instance, used to access accounts.
     */
    protected Accounts accounts = null;

    /** The last generated CSRF token. */
    private String csrfToken;

    /**
     * Creates a new RemoteStorage, providing its dependencies.
     *
     * @param crypto       the crypto
     * @param cloudAppKeys the cloud app keys
     * @param accounts     the accounts
     */
    protected AbstractRemoteStorage(Crypto crypto, CloudAppKeys cloudAppKeys, Accounts accounts) {
        this.crypto = crypto;
        this.cloudAppKeys = cloudAppKeys;
        this.accounts = accounts;
        csrfToken = null;
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
    public RemoteException cloudException(Account account, Response<?> response, String exceptionText) throws DatabaseConnectionClosedException {
        RemoteException remoteException = new RemoteException(exceptionText, retrofitErrorReason(response));
        if (remoteException.getReason() == RemoteException.Reason.TooManyRequests) {
            account.notifyTooManyRequests();
        }
        return remoteException;
    }

    @Override
    public Account createAccount() {
        return accounts.createAccount(getStorageType());
    }

    @Override
    public List<String> accountNames() throws DatabaseConnectionClosedException {
        return accounts.accountNames(getStorageType());
    }

    @Override
    public Account account(String accountName) throws DatabaseConnectionClosedException {
        return accounts.accountWithTypeAndName(getStorageType(), accountName);
    }

    @Override
    public void removeAccount(String accountName) throws DatabaseConnectionClosedException {
        Account account = account(accountName);
        if (null!=account) {
            account.delete();
        }
    }

    @Override
    public String requestCSRFToken() throws NoSuchAlgorithmException {
        csrfToken = crypto.generateCSRFToken(32);
        return csrfToken;
    }

    @Override
    public Account refreshedAccount(String accountName) throws RemoteException, DatabaseConnectionClosedException {
        if (null==accountName) {
            throw new RemoteException("Failed to get refreshed token : account name is null",
                    RemoteException.Reason.AccountNameIsNull);
        }
        Account account = account(accountName);
        if (null == account) {
            throw new RemoteException("Failed to get refreshed token : account not found",
                    RemoteException.Reason.AccountNotFound);
        }
        if (!account.isAccessTokenExpired()) {
            return account;
        }
        if (account.hasTooManyRequests()) {
            throw new RemoteException("Too many requests, retry delay not expired yet",
                    RemoteException.Reason.TooManyRequestsDelayNotExpired);
        }
        try {
            account = refreshToken(accountName);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to refresh token", e.getReason(), e);
        }
        return account;
    }

    @Override
    public D appFolder(String accountName) throws RemoteException, DatabaseConnectionClosedException {
        if (null==accountName) {
            throw new RemoteException("Failed to get refreshed token : account name is null",
                    RemoteException.Reason.AccountNameIsNull);
        }
        Account account = refreshedAccount(accountName);
        if (null == account) {
            throw new RemoteException("Failed to get refreshed token : account not found",
                    RemoteException.Reason.AccountNotFound);
        }
        D rootDocument;
        try {
            rootDocument = rootFolder(accountName);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to get root folder",
                    RemoteException.Reason.GetRootFolderError, e);
        }
        D appFolder;
        try {
            appFolder = rootDocument.childFolder(Constants.FILE.APP_DIR_NAME);
        } catch (RemoteException e) {
            if (e.getReason() == RemoteException.Reason.NotFound) {
                try {
                    appFolder = rootDocument.createChildFolder(Constants.FILE.APP_DIR_NAME);
                } catch (RemoteException ex) {
                    throw new RemoteException("Failed to create app folder", ex.getReason(), ex);
                }
            } else {
                throw new RemoteException("Failed to get app folder", e.getReason(), e);
            }
        }
        return appFolder;
    }
}
