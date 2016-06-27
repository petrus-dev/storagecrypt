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

import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * This class is used to retrieve accounts.
 *
 * @author Pierre Sagne
 * @since 19.03.2016
 */
public class Accounts {

    /** This class dependencies */
    private AppContext appContext;
    private Database database = null;
    private Crypto crypto = null;
    private CloudAppKeys cloudAppKeys = null;
    private TextI18n textI18n = null;
    private EncryptedDocuments encryptedDocuments = null;

    /**
     * Creates a new Accounts instance with default values.
     *
     * <p>Dependencies have to be set later, with the {@link Accounts#setDependencies} method.
     */
    public Accounts() {}

    /**
     * Sets the dependencies needed by this instance to perform its tasks.
     *
     * @param appContext the AppContext instance
     */
    public void setDependencies(AppContext appContext) {
        this.appContext = appContext;
        this.database = appContext.getDatabase();
        this.crypto = appContext.getCrypto();
        this.cloudAppKeys = appContext.getCloudAppKeys();
        this.textI18n = appContext.getTextI18n();
        this.encryptedDocuments = appContext.getEncryptedDocuments();
    }

    /**
     * Creates an account of the specified type StorageType.
     *
     * @param storageType the StorageType of this account
     * @return the created account
     */
    public Account createAccount(StorageType storageType) {
        Account account = new Account(appContext, this, crypto, cloudAppKeys, textI18n, database);
        account.setStorageType(storageType);
        return account;
    }

    /** This method is just there to suppress the "unchecked cast" warning */
    @SuppressWarnings("unchecked")
    private Account connectWithAccessCode(RemoteStorage storage,
                                          Map<String, String> responseParameters)
            throws RemoteException, DatabaseConnectionClosedException {
        return storage.connectWithAccessCode(responseParameters);
    }

    /**
     * Connects to an account from a received OAuth2 access code.
     *
     * @param storageType        the StorageType of the account
     * @param keyAlias           the alias of the key which will be used by default for this account
     * @param responseParameters the response parameters received from the OAuth2 "authorize" call
     * @return the newly created account, or an updated existing one if one was found with the same
     * StorageType and name
     * @throws RemoteException                    if an error occured when calling the underlying API
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public Account connectWithAccessCode(StorageType storageType, String keyAlias,
                                         Map<String, String> responseParameters)
            throws RemoteException, DatabaseConnectionClosedException {

        /* Get a CloudStorage correspoding to the StorageType, to call the API */
        RemoteStorage storage = appContext.getRemoteStorage(storageType);

        /* Call {@link Account#connectWithAccessCode} to do the job */
        Account account;
        try {
            account = connectWithAccessCode(storage, responseParameters);
            account.setDependencies(appContext, this, crypto, cloudAppKeys, textI18n, database);
            account.setDefaultKeyAlias(keyAlias);
            account.update();
        } catch (RemoteException e) {
            throw new RemoteException("Could not add account", e.getReason(), e);
        }

        /* If it worked, update documents root, so that the new storage shows up */
        encryptedDocuments.updateRoots();

        /* Retrieve the EncryptedDocument representing the "root folder" of this account */
        EncryptedDocument root = encryptedDocuments.root(storageType, account);

        /* Get the RemoteDocument for the root folder */
        RemoteDocument appFolder;
        try {
            appFolder = storage.appFolder(account.getAccountName());
        } catch (RemoteException e) {
            throw new RemoteException("Could not get app folder", RemoteException.Reason.GetAppFolderError, e);
        }
        /* store the remote document id in the EncryptedDocument */
        root.updateBackEntryId(appFolder.getId());

        /* Initialize the account quota */
        account = storage.refreshQuota(account);

        return account;
    }

    /**
     * Adds an account : persist it to the database.
     *
     * @param account the account to be added
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void add(Account account) throws DatabaseConnectionClosedException {
        account.add();
    }

    /**
     * Returns the number of accounts.
     *
     * @return the number of accounts
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public long size() throws DatabaseConnectionClosedException {
        return database.getNumAccounts();
    }

    /**
     * Returns all the accounts.
     *
     * @return a list containing all accounts
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<Account> allAccounts() throws DatabaseConnectionClosedException {
        /* Get the accounts */
        List<Account> accounts = database.getAllAccounts();
        /* Set their dependencies before returning them */
        for (Account account : accounts) {
            account.setDependencies(appContext, this, crypto, cloudAppKeys, textI18n, database);
        }
        return accounts;
    }

    /**
     * Returns accounts which have a given changes synchronization state.
     *
     * @param state the changes synchronization state
     * @return a list containing the accounts in this state
     * @throws DatabaseConnectionClosedException if the database connection closed exception
     */
    public List<Account> accountsWithChangesSyncState(State state) throws DatabaseConnectionClosedException {
        /* Get the accounts */
        List<Account> accounts = database.getAccountsByChangesSyncState(state);
        /* Set their dependencies before returning them */
        for (Account account : accounts) {
            account.setDependencies(appContext, this, crypto, cloudAppKeys, textI18n, database);
        }
        return accounts;
    }

    /**
     * Returns the name of all accounts of a certain StorageType.
     *
     * @param type the StorageType
     * @return a list containing the names of all accounts of the specified StorageType
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<String> accountNames(StorageType type) throws DatabaseConnectionClosedException {
        return database.getAccountNamesByType(type);
    }

    /**
     * Returns an account with specified StorageType and user name.
     *
     * @param storageType the StorageType
     * @param accountName the account user name
     * @return the account, or null if not found
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public Account accountWithTypeAndName(StorageType storageType, String accountName) throws DatabaseConnectionClosedException {
        /* Get the accounts */
        Account account = database.getAccountByTypeAndName(storageType, accountName);
        /* Set its dependencies before returning it */
        if (null!=account) {
            account.setDependencies(appContext, this, crypto, cloudAppKeys, textI18n, database);
        }
        return account;
    }

    /**
     * Returns an account, from its database table id.
     *
     * @param id the database table id
     * @return the account, or null if not found
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public Account accountWithId(long id) throws DatabaseConnectionClosedException {
        /* Get the account */
        Account account = database.getAccountById(id);
        /* Set its dependencies before returning it */
        if (null!=account) {
            account.setDependencies(appContext, this, crypto, cloudAppKeys, textI18n, database);
        }
        return account;
    }
}
