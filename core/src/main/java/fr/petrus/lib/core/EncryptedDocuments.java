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

package fr.petrus.lib.core;

import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * This class is used to retrieve encrypted documents.
 *
 * @author Pierre Sagne
 * @since 19.03.2016
 */
public class EncryptedDocuments {
    private AppContext appContext = null;
    private Crypto crypto = null;
    private FileSystem fileSystem = null;
    private KeyManager keyManager = null;
    private TextI18n textI18n = null;
    private Database database = null;
    private CloudAppKeys cloudAppKeys = null;
    private Accounts accounts = null;

    /**
     * Creates a new {@code EncryptedDocuments} instance with default values.
     *
     * <p>Dependencies have to be set later, with the {@link EncryptedDocuments#setDependencies} method.
     */
    public EncryptedDocuments() {}

    /**
     * Sets the dependencies needed by this instance to perform its tasks.
     *
     * @param appContext the AppContext instance
     */
    public void setDependencies(AppContext appContext) {
        this.appContext = appContext;
        this.crypto = appContext.getCrypto();
        this.fileSystem = appContext.getFileSystem();
        this.keyManager = appContext.getKeyManager();
        this.textI18n = appContext.getTextI18n();
        this.database = appContext.getDatabase();
        this.cloudAppKeys = appContext.getCloudAppKeys();
        this.accounts = appContext.getAccounts();
    }

    /**
     * Adds the given {@code encryptedDocument} : persist it to the database.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to be added
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void add(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException {
        encryptedDocument.add();
    }

    /**
     * Sets the dependencies of the given {@code encryptedDocument}
     *
     * @param encryptedDocument the {@code EncryptedDocument} which dependencies will we set
     */
    private void setAccountDependenciesFor(EncryptedDocument encryptedDocument) {
        if (null!=encryptedDocument && null!=encryptedDocument.getBackStorageAccount()) {
            encryptedDocument.getBackStorageAccount()
                    .setDependencies(appContext, accounts, crypto, cloudAppKeys, textI18n, database);
        }
    }

    /**
     * Returns the {@code EncryptedDocument} with the given {@code id}.
     *
     * @param id the id of the {@code EncryptedDocument} in the database
     * @return the encrypted document which database id matches the given {@code id}
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument encryptedDocumentWithId(long id)
            throws DatabaseConnectionClosedException {
        EncryptedDocument encryptedDocument = database.getEncryptedDocumentById(id);
        if (null!=encryptedDocument) {
            encryptedDocument.setDependencies(crypto, keyManager, fileSystem, textI18n, database);
            setAccountDependenciesFor(encryptedDocument);
        }
        return encryptedDocument;
    }

    /**
     * Returns the {@code EncryptedDocument} with the given {@code account} and {@code backEntryId}.
     *
     * @param account     the account where the remote document is stored
     * @param backEntryId the remote id of the {@code EncryptedDocument} on the given {@code account}
     * @return the encrypted document which matches the given {@code account} and {@code backEntryId}
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument encryptedDocumentWithAccountAndEntryId(Account account, String backEntryId)
            throws DatabaseConnectionClosedException {
        EncryptedDocument encryptedDocument = database.getEncryptedDocumentByAccountAndEntryId(account, backEntryId);
        if (null!=encryptedDocument) {
            encryptedDocument.setDependencies(crypto, keyManager, fileSystem, textI18n, database);
            setAccountDependenciesFor(encryptedDocument);
        }
        return encryptedDocument;
    }

    /**
     * Returns the list of encrypted documents, which were encrypted with the key matching the given
     * {@code alias}.
     *
     * @param alias the alias of the key used to encrypt the documents
     * @return the list of encrypted documents, which were encrypted with the key matching the given
     *         {@code alias}
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> encryptedDocumentsWithKeyAlias(String alias)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = database.getEncryptedDocumentsByKeyAlias(alias);
        for (EncryptedDocument encryptedDocument : encryptedDocuments) {
            encryptedDocument.setDependencies(crypto, keyManager, fileSystem, textI18n, database);
            setAccountDependenciesFor(encryptedDocument);
        }
        return encryptedDocuments;
    }

    /**
     * Returns the list of encrypted documents, for which the given {@code syncAction} matches the given
     * {@code state}.
     * Encrypted documents with sync state list.
     *
     * @param syncAction the synchronization action to check
     * @param state      the state of the given {@code syncAction}
     * @return the list of encrypted documents, for which the given {@code syncAction} matches the given
     *         {@code state}
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> encryptedDocumentsWithSyncState(SyncAction syncAction, State state)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = database.getEncryptedDocumentsBySyncState(syncAction, state);
        for (EncryptedDocument encryptedDocument : encryptedDocuments) {
            encryptedDocument.setDependencies(crypto, keyManager, fileSystem, textI18n, database);
            setAccountDependenciesFor(encryptedDocument);
        }
        return encryptedDocuments;
    }

    /**
     * Returns the root encrypted document with the given {@code storageType} and {@code account}.
     *
     * @param storageType the {@code StorageType} of the root encrypted document to return
     * @param account     the account of the root encrypted document to return
     * @return the root encrypted document with the given {@code storageType} and {@code account}
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument root(StorageType storageType, Account account)
            throws DatabaseConnectionClosedException {
        EncryptedDocument root = database.getRootEncryptedDocument(storageType, account);
        if (null!=root) {
            root.setDependencies(crypto, keyManager, fileSystem, textI18n, database);
            setAccountDependenciesFor(root);
        }
        return root;
    }

    /**
     * Returns the list of all the roots in the database.
     *
     * @return the list of all the roots in the database
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> roots() throws DatabaseConnectionClosedException {
        List<EncryptedDocument> roots = database.getRootEncryptedDocuments();
        for (EncryptedDocument root : roots) {
            root.setDependencies(crypto, keyManager, fileSystem, textI18n, database);
            setAccountDependenciesFor(root);
        }
        return roots;
    }

    /**
     * Generates all the roots into the database, and returns them as a list.
     *
     * @return the list of all the roots in the database
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> generateRoots() throws DatabaseConnectionClosedException {
        ArrayList<EncryptedDocument> roots = new ArrayList<>();

        EncryptedDocument root;

        for (StorageType storageType : StorageType.values()) {
            if (StorageType.Unsynchronized.equals(storageType)) {
                root = new EncryptedDocument(crypto, keyManager, fileSystem, textI18n, database);
                root.setMimeType(Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE);
                root.setParentId(Constants.STORAGE.ROOT_PARENT_ID);
                root.setKeyAlias(Constants.CRYPTO.KEY_STORE_DEFAULT_KEY_ALIAS);
                root.setBackStorageType(storageType);
                root.setDisplayName(textI18n.getStorageTypeText(storageType));
                roots.add(root);
            } else {
                for (String accountName : accounts.accountNames(storageType)) {
                    root = new EncryptedDocument(crypto, keyManager, fileSystem, textI18n, database);
                    root.setMimeType(Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE);
                    root.setParentId(Constants.STORAGE.ROOT_PARENT_ID);
                    root.setKeyAlias(Constants.CRYPTO.KEY_STORE_DEFAULT_KEY_ALIAS);
                    root.setBackStorageType(storageType);
                    root.setBackStorageAccount(accounts.accountWithTypeAndName(storageType, accountName));
                    setAccountDependenciesFor(root);
                    root.setDisplayName(textI18n.getStorageTypeText(storageType));
                    roots.add(root);
                }
            }
        }

        return roots;
    }

    /**
     * Updates the roots in the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateRoots() throws DatabaseConnectionClosedException {
        for (EncryptedDocument encryptedDocument : generateRoots()) {
            if (null==root(encryptedDocument.getBackStorageType(), encryptedDocument.getBackStorageAccount())) {
                add(encryptedDocument);
            }
        }
    }
}
