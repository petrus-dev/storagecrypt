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

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.result.ProcessProgressListener;
import retrofit2.Response;

/**
 * This interface is used to access a remote storage in the cloud.
 *
 * @param <S> the {@link RemoteStorage} implementation
 * @param <D> the {@link RemoteDocument} implementation
 * @author Pierre Sagne
 * @since 05.05.2016
 */
public interface RemoteStorage<S extends RemoteStorage<S, D>, D extends RemoteDocument<S, D>> {
    /**
     * Returns the {@link RemoteException.Reason} based on a Retrofit error response.
     *
     * @param response the Retrofit error response
     * @return the exception reason
     */
    RemoteException.Reason retrofitErrorReason(Response<?> response);

    /**
     * Creates a {@link RemoteException} based on a Retrofit error response, and a text.
     *
     * @param account       the account used to call the API
     * @param response      the Retrofit error response
     * @param exceptionText the exception text
     * @return the created exception
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    RemoteException cloudException(Account account, Response<?> response, String exceptionText)
            throws DatabaseConnectionClosedException;

    /**
     * Creates an empty Account.
     *
     * @return the newly created account
     */
    Account createAccount();

    /**
     * Returns a list of all account user names using this type of RemoteStorage.
     *
     * @return the list of accounts
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    List<String> accountNames() throws DatabaseConnectionClosedException;

    /**
     * Returns an account using this type of RemoteStorage, with the provided user name.
     *
     * @param accountName the account user name
     * @return the account, or null if none was found with this user name and this RemoteStorage type
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    Account account(String accountName) throws DatabaseConnectionClosedException;

    /**
     * Removes an account.
     *
     * @param accountName the account user name
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void removeAccount(String accountName) throws DatabaseConnectionClosedException;

    /**
     * Requests a CSRF token, and stores it for checking later.
     *
     * @return the CSRF token
     * @throws NoSuchAlgorithmException if an error occured when generating the token
     */
    String requestCSRFToken() throws NoSuchAlgorithmException;

    /**
     * Refreshes the OAuth2 tokens of this account if needed, and returns the updated account.
     *
     * @param accountName the account user name
     * @return the account updated with refreshed OAuth2 tokens if needed
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    Account refreshedAccount(String accountName) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns the app folder for this storage, on the account matching the provided user name.
     *
     * <p>The app folder is a folder at the root of the remote storage, with the name of the app,
     * where all the documents managed by the app are stored.
     *
     * <p> The app will not access or modify documents outside this folder.
     *
     * <p>If the app folder does not exist, it is created.
     *
     * @param accountName the account user name
     * @return the app folder
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D appFolder(String accountName) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns the storage type of this RemoteStorage.
     *
     * @return the storage type
     */
    StorageType getStorageType();

    /**
     * Builds and returns the OAuth2 "authorize" URL.
     *
     * @param mobileVersion if true, requests the mobile version of the credentials page, if it exists
     *
     * @return the OAuth2 "authorize" URL
     * @throws RemoteException if any error occurs when calling the underlying API
     */
    String oauthAuthorizeUrl(boolean mobileVersion) throws RemoteException;

    /**
     * Returns the OAuth2 OAuth2 redirect URI for the "authorize" call.
     *
     * @return the OAuth2 redirect URI for the "authorize" call
     * @throws RemoteException if any error occurs when calling the underlying API
     */
    String oauthAuthorizeRedirectUri() throws RemoteException;

    /**
     * Creates an account from the response of the OAuth2 "authorize" API call.
     *
     * @param responseParameters the response parameters of the OAuth2 "authorize" API call
     * @return the newly created account
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    Account connectWithAccessCode(Map<String, String> responseParameters) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Finds an account with the given user name, and return it after refreshing its OAuth2 token.
     *
     * @param accountName the account user name
     * @return the refreshed account
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    Account refreshToken(String accountName) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Refreshes the account quota (querying the underlying API), and returns the updated account.
     *
     * @param account the account to refresh
     * @return the updated account
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    Account refreshQuota(Account account) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Revokes the OAuth2 token of an account (logout), so it can no longer be used.
     *
     * @param accountName the account user name
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void revokeToken(String accountName) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Returns the account user name, from the OAuth2 access token.
     *
     * @param accessToken the OAuth2 access token
     * @return the user name of the account
     * @throws RemoteException if any error occurs when calling the underlying API
     */
    String accountNameFromAccessToken(String accessToken) throws RemoteException;

    /**
     * Returns the remote root folder, where all the remote files of the account are stored
     * (even those not created by the app) : it is only used to create and find the app folder.
     *
     * @param accountName the account user name
     * @return the root folder
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D rootFolder(String accountName) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Accesses the account matching the user name, and returns the file with the provided remote id.
     *
     * @param accountName the account user name
     * @param id          the file remote id
     * @return the file
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D file(String accountName, String id) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Accesses the account matching the user name, and returns the folder with the provided remote id.
     *
     * @param accountName the account user name
     * @param id          the folder remote id
     * @return the folder
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D folder(String accountName, String id) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Accesses the account matching the user name, and returns the document with the provided remote id.
     *
     * @param accountName the account user name
     * @param id          the document remote id
     * @return the document
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    D document(String accountName, String id) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Accesses the account matching the user name, and returns the changes since the last query,
     * identified by the {@code lastChangeId}.
     *
     * @param accountName the account user name
     * @param lastChangeId the last change query id
     * @param listener  a listener which allows to track the progress, and cancel/pause it
     * @return the remote changes
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    RemoteChanges changes(String accountName, String lastChangeId, ProcessProgressListener listener) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Sends a request to delete a remote file.
     *
     * @param accountName the account user name
     * @param id          the file remote id
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void deleteFile(String accountName, String id) throws RemoteException, DatabaseConnectionClosedException;

    /**
     * Sends a request to delete a remote folder.
     *
     * @param accountName the account user name
     * @param id          the folder remote id
     * @throws RemoteException                    if any error occurs when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    void deleteFolder(String accountName, String id) throws RemoteException, DatabaseConnectionClosedException;
}
