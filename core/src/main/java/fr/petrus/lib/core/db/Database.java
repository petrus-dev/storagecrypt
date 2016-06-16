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

package fr.petrus.lib.core.db;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionException;

/**
 * This interface provides methods to manipulate a database.
 *
 * @author Pierre Sagne
 * @since 26.03.2016
 */
public interface Database {

    /**
     * Opens a connection to this non encrypted database.
     *
     * @throws DatabaseConnectionException if the connection fails
     */
    void open() throws DatabaseConnectionException;

    /**
     * Opens a connection to this encrypted database, using the given {@code password} to unlock it.
     *
     * @param password this database encryption password
     * @throws DatabaseConnectionException if the connection fails
     */
    void open(String password) throws DatabaseConnectionException;

    /**
     * Returns whether the connection to this database is open.
     *
     * @return true if the connection to this database is open, false otherwise
     */
    boolean isOpen();

    /**
     * Closes the connection to this database.
     */
    void close();

    /**
     * Executes the given {@code callable}, wrapping it in a database transaction.
     *
     * @param <T>      the type of the result returned by the callable
     * @param callable the callable to wrap in the transaction
     * @return the result returned by the callable
     * @throws SQLException                      if any sql statement inside the {@code callable}
     *                                           throws an exception
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    <T> T callInTransaction(Callable<T> callable) throws SQLException, DatabaseConnectionClosedException;

    /**
     * Deletes all the contents of this database.
     *
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void resetDatabase() throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocuments} table column related to the given {@code orderBy} value.
     *
     * @param orderBy the value which we want to know which column is related to
     * @return the {@code EncryptedDocuments} table column related to the given {@code orderBy} value
     */
    String getEncryptedDocumentOrderColumnName(OrderBy orderBy);

    /**
     * Returns whether the given {@code orderBy} value order defines an ascending order.
     *
     * @param orderBy the value which we want to know if it defines an ascending order
     * @return true if the given {@code orderBy} value order defines an ascending order, false otherwise
     */
    boolean isEncryptedDocumentOrderAscending(OrderBy orderBy);

    /**
     * Adds the given {@code encryptedDocument} to this database.
     *
     * @param encryptedDocument the encrypted document to add to this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void addEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException;

    /**
     * Updates the given {@code encryptedDocument}.
     *
     * <p>The fields of the given {@code encryptedDocument} are persisted into this database
     *
     * @param encryptedDocument the encrypted document to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException;

    /**
     * Refreshes the given {@code encryptedDocument}.
     *
     * <p>The fields of the given {@code encryptedDocument} are refreshed from the persisted data of
     * this database
     *
     * @param encryptedDocument the encrypted document to refresh from this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void refreshEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException;

    /**
     * Updates the size of the {@code EncryptedDocument} which has the given {@code id} into
     * this database with the given {@code size}.
     *
     * @param id   the id of the {@code EncryptedDocument} which size will be updated
     * @param size the size to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentSize(long id, long size) throws DatabaseConnectionClosedException;

    /**
     * Updates the key alias of the {@code EncryptedDocument} which has the given {@code id} into
     * this database with the given {@code keyAlias}.
     *
     * @param id       the id of the {@code EncryptedDocument} which key alias will be updated
     * @param keyAlias the key alias to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentKeyAlias(long id, String keyAlias) throws DatabaseConnectionClosedException;

    /**
     * Updates the local modification time of the {@code EncryptedDocument} which has the given
     * {@code id} into this database with the given {@code time}.
     *
     * @param id   the id of the {@code EncryptedDocument} which local modification time will be updated
     * @param time the local modification time to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentLocalModificationTime(long id, long time) throws DatabaseConnectionClosedException;

    /**
     * Updates the back entry id of the {@code EncryptedDocument} which has the given {@code id}
     * into this database with the given {@code backEntryId}.
     *
     * @param id          the id of the {@code EncryptedDocument} which back entry id will be updated
     * @param backEntryId the back entry id to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentBackEntryId(long id, String backEntryId) throws DatabaseConnectionClosedException;

    /**
     * Updates the back entry version of the {@code EncryptedDocument} which has the given {@code id}
     * into this database with the given {@code version}.
     *
     * @param id      the id of the {@code EncryptedDocument} which back entry version will be updated
     * @param version the back entry version to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentBackEntryVersion(long id, long version) throws DatabaseConnectionClosedException;

    /**
     * Updates the remote modification time of the {@code EncryptedDocument} which has the given
     * {@code id} into this database with the given {@code time}.
     *
     * @param id   the id of the {@code EncryptedDocument} which remote modification time will be updated
     * @param time the remote modification time to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentRemoteModificationTime(long id, long time) throws DatabaseConnectionClosedException;

    /**
     * Updates the back entry folder id of the {@code EncryptedDocument} which has the given {@code id}
     * into this database with the given {@code backEntryId}.
     *
     * @param id                the id of the {@code EncryptedDocument} which back entry folder id will be updated
     * @param backEntryFolderId the back entry folder id to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentBackEntryFolderId(long id, long backEntryFolderId) throws DatabaseConnectionClosedException;

    /**
     * Updates the sync state of the given {@code syncAction} of the {@code EncryptedDocument} which
     * has the given {@code id} into this database with the given {@code state}.
     *
     * @param id         the id of the {@code EncryptedDocument} which sync state will be updated
     * @param syncAction the type of the sync state to persist into this database
     * @param state      the state to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentSyncState(long id, SyncAction syncAction, State state) throws DatabaseConnectionClosedException;

    /**
     * Updates the back entry folder last subfolder id of the {@code EncryptedDocument} which has
     * the given {@code id} into this database with the given {@code folderLastSubfolderId}.
     *
     * @param id                    the id of the {@code EncryptedDocument} which back entry folder last
     *                              subfolder id will be updated
     * @param folderLastSubfolderId the back entry folder last subfolder id to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentBackEntryFolderLastSubfolderId(long id, long folderLastSubfolderId) throws DatabaseConnectionClosedException;

    /**
     * Updates the number of synchronization failures of the {@code EncryptedDocument} which has
     * the given {@code id} into this database with the given {@code numSyncFailures}.
     *
     * @param id              the id of the {@code EncryptedDocument} which number of synchronization
     *                        failures will be updated
     * @param numSyncFailures the number of synchronization failures to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentBackEntryNumSyncFailures(long id, int numSyncFailures) throws DatabaseConnectionClosedException;

    /**
     * Updates the time of the last synchronization failure of the {@code EncryptedDocument} which
     * has the given {@code id} into this database with the given {@code lastFailureTime}.
     *
     * @param id              the id of the {@code EncryptedDocument} which number of synchronization
     *                        failures will be updated
     * @param lastFailureTime the time of the last synchronization failure to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateEncryptedDocumentBackEntryLastFailureTime(long id, long lastFailureTime) throws DatabaseConnectionClosedException;

    /**
     * Deletes the given {@code encryptedDocument} from this database.
     *
     * @param encryptedDocument the encrypted document to delete from this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void deleteEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument} which has the given {@code id} in this database.
     *
     * @param id the id of the {@code EncryptedDocument} to return
     * @return the {@code EncryptedDocument} which has the given {@code id} in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    EncryptedDocument getEncryptedDocumentById(long id) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument} which has the given {@code displayName} and
     * {@code parentId} in this database.
     *
     * @param displayName the display name of the {@code EncryptedDocument} to return
     * @param parentId    the id of the parent of the {@code EncryptedDocument} to return
     * @return the {@code EncryptedDocument} which has the given {@code displayName} and
     *         {@code parentId} in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    EncryptedDocument getEncryptedDocumentByNameAndParentId(String displayName, long parentId) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument} which has the given {@code account} and
     * {@code backEntryId} in this database.
     *
     * @param account     the account of the {@code EncryptedDocument} to return
     * @param backEntryId the backEntryId of the {@code EncryptedDocument} to return
     * @return the {@code EncryptedDocument} which has the given {@code account} and
     *         {@code backEntryId} in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    EncryptedDocument getEncryptedDocumentByAccountAndEntryId(Account account, String backEntryId) throws DatabaseConnectionClosedException;

    /**
     * Returns all the {@code EncryptedDocument}s present in this database.
     * 
     * @param foldersFirst if true, the {@code EncryptedDocument}s representing folders will be before
     *                     the ones representing files in the list, otherwise they will be mixed with
     *                     the files.
     * @param orderBy      the criterion used to sort the {@code EncryptedDocument}s 
     * @return a list containing all the {@code EncryptedDocument}s present in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<EncryptedDocument> getAllEncryptedDocuments(boolean foldersFirst, OrderBy orderBy) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument}s which have the given {@code parentId} in this database.
     *
     * @param parentId     the parent id of the {@code EncryptedDocument}s to return 
     * @param foldersFirst if true, the {@code EncryptedDocument}s representing folders will be before
     *                     the ones representing files in the list, otherwise they will be mixed with
     *                     the files.
     * @return a list containing the {@code EncryptedDocument}s which have the given {@code parentId}
     *         in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<EncryptedDocument> getEncryptedDocumentsByParentId(long parentId, boolean foldersFirst) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument}s which have the given {@code parentId} in this database.
     *
     * @param parentId     the parent id of the {@code EncryptedDocument}s to return 
     * @param foldersFirst if true, the {@code EncryptedDocument}s representing folders will be before
     *                     the ones representing files in the list, otherwise they will be mixed with
     *                     the files.
     * @param orderBy      the criterion used to sort the {@code EncryptedDocument}s 
     * @return a list containing the {@code EncryptedDocument}s which have the given {@code parentId}
     *         in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<EncryptedDocument> getEncryptedDocumentsByParentId(long parentId, boolean foldersFirst,
                                                            OrderBy orderBy) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument}s which have the given {@code keyAlias} in this database.
     *
     * @param keyAlias the key alias of the {@code EncryptedDocument}s to return
     * @return a list containing the {@code EncryptedDocument}s which have the given {@code keyAlias}
     *         in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<EncryptedDocument> getEncryptedDocumentsByKeyAlias(String keyAlias) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument}s which have the given {@code state} for the given
     * {@code syncAction} in this database.
     *
     * @param syncAction the synchronization action
     * @param state      the state of the {@code syncAction}
     * @return a list containing the {@code EncryptedDocument}s for which the {@code syncAction}
     *         has the given {@code state} in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<EncryptedDocument> getEncryptedDocumentsBySyncState(SyncAction syncAction, State state) throws DatabaseConnectionClosedException;

    /**
     * Removes the {@code EncryptedDocument}s which parent has the given {@code id}, and all of
     * their children from this database.
     *
     * @param id the id of the {@code EncryptedDocument} which children (and their children) will be
     *           removed from this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void removeEncryptedDocumentChildrenReferences(long id) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument} which has the given {@code storageType} and
     * {@code account}, and no parent in this database.
     *
     * @param storageType the storage type of the root {@code EncryptedDocument} to return
     * @param account     the account of the root {@code EncryptedDocument} to return
     * @return the {@code EncryptedDocument} which has the given {@code storageType} and
     *         {@code account}, and no parent in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    EncryptedDocument getRootEncryptedDocument(StorageType storageType, Account account) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code EncryptedDocument}s which have no parent in this database.
     *
     * @return a list of the {@code EncryptedDocument}s which have no parent in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<EncryptedDocument> getRootEncryptedDocuments() throws DatabaseConnectionClosedException;

    /**
     * Adds the given {@code account} to this database.
     *
     * @param account the account to add to this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void addAccount(Account account) throws DatabaseConnectionClosedException;

    /**
     * Updates the given {@code account}.
     *
     * <p>The fields of the given {@code account} are persisted into this database
     *
     * @param account the account to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateAccount(Account account) throws DatabaseConnectionClosedException;

    /**
     * Updates the changes sync state of the {@code Account} which has the given {@code id}
     * into this database with the given {@code state}.
     *
     * @param id         the id of the {@code Account} which changes sync state will be updated
     * @param state      the state to persist into this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void updateAccountChangesSyncState(long id, State state) throws DatabaseConnectionClosedException;

    /**
     * Refreshes the given {@code account}.
     *
     * <p>The fields of the given {@code acount} are refreshed from the persisted data of
     * this database
     *
     * @param account the account to refresh from this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void refreshAccount(Account account) throws DatabaseConnectionClosedException;

    /**
     * Deletes the given {@code account} from this database.
     *
     * @param account the account to delete from this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    void deleteAccount(Account account) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code Account} which has the given {@code id} in this database.
     *
     * @param id the id of the {@code Account} to return
     * @return the {@code Account} which has the given {@code id} in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    Account getAccountById(long id) throws DatabaseConnectionClosedException;

    /**
     * Returns the number of {@code Account}s present in this database.
     *
     * @return the number of {@code Account}s present in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    long getNumAccounts() throws DatabaseConnectionClosedException;

    /**
     * Returns all the {@code Account}s present in this database.
     *
     * @return a list containing all the {@code Account}s present in this database
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<Account> getAllAccounts() throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code Account}s which synchronization state equals the given {@code state}.
     *
     * @param state      the state of the synchronization state
     * @return a list containing the {@code Account}s for which the synchronization state equals the
     *         given {@code state}
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<Account> getAccountsByChangesSyncState(State state) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code Account} with the given {@code storageType} and {@code name}.
     *
     * @param storageType the storage type of the {@code Account} to return
     * @param name        the name of the {@code Account} to return
     * @return the {@code Account} with the given {@code storageType} and {@code name}
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    Account getAccountByTypeAndName(StorageType storageType, String name) throws DatabaseConnectionClosedException;

    /**
     * Returns the {@code Account}s with the given {@code storageType}.
     *
     * @param storageType the storage type of the {@code Account}s to return
     * @return the list of the {@code Account}s with the given {@code storageType}
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<Account> getAccountsByType(StorageType storageType) throws DatabaseConnectionClosedException;

    /**
     * Returns the names of the {@code Account}s with the given {@code storageType}.
     *
     * @param storageType the storage type of the {@code Account}s
     * @return the list of the names of the {@code Account}s which have the given {@code storageType}
     * @throws DatabaseConnectionClosedException if this database connection is closed
     */
    List<String> getAccountNamesByType(StorageType storageType) throws DatabaseConnectionClosedException;
}
