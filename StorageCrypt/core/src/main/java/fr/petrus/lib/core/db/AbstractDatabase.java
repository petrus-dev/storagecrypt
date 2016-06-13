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

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.UpdateBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The abstract implementation of the {@link Database} interface.
 *
 * <p>It implements all the methods which do not depend on the database engine used.
 *
 * <p>The real implementations should extend this abstract class.
 *
 * @author Pierre Sagne
 * @since 18.07.2015.
 */
public abstract class AbstractDatabase implements Database {
    private Logger LOG = LoggerFactory.getLogger(AbstractDatabase.class);

    /**
     * Returns the DAO, used to access the {@code Account}s.
     *
     * <p>Implementations must implement this method
     *
     * @return the DAO, used to access the {@code Account}s
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    protected abstract Dao<Account, Long> getAccountDao() throws DatabaseConnectionClosedException;

    /**
     * Returns the DAO, used to access the {@code EncryptedDocument}s.
     *
     * <p>Implementations must implement this method
     *
     * @return the DAO, used to access the {@code EncryptedDocument}s
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    protected abstract Dao<EncryptedDocument, Long> getEncryptedDocumentDao() throws DatabaseConnectionClosedException;

    /**
     * The TextI18n instance, used to get localized messages.
     */
    protected TextI18n textI18n;

    /**
     * Creates a new {@code AbstractDatabase}, providing its dependencies.
     *
     * @param textI18n a {@code TextI18n} instance
     */
    protected AbstractDatabase(TextI18n textI18n) {
        this.textI18n = textI18n;
    }

    @Override
    public String getEncryptedDocumentOrderColumnName(OrderBy orderBy) {
        switch (orderBy) {
            case IdAsc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID;
            case IdDesc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID;
            case NameAsc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME;
            case NameDesc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME;
            case MimeTypeAsc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE;
            case MimeTypeDesc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE;
            case SizeAsc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_SIZE;
            case SizeDesc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_SIZE;
            case LocalModificationDateAsc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_LOCAL_MODIFICATION_TIME;
            case LocalModificationDateDesc:
                return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_LOCAL_MODIFICATION_TIME;
        }
        return DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME;

    }

    @Override
    public boolean isEncryptedDocumentOrderAscending(OrderBy orderBy) {
        switch (orderBy) {
            case IdAsc:
            case NameAsc:
            case MimeTypeAsc:
            case SizeAsc:
            case LocalModificationDateAsc:
                return true;
            case IdDesc:
            case NameDesc:
            case MimeTypeDesc:
            case SizeDesc:
            case LocalModificationDateDesc:
                return false;
        }
        return true;
    }

    @Override
    public void addEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException {
        try {
            getEncryptedDocumentDao().create(encryptedDocument);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException {
        try {
            getEncryptedDocumentDao().update(encryptedDocument);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void refreshEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException {
        try {
            getEncryptedDocumentDao().refresh(encryptedDocument);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentSize(long id, long size) throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_SIZE, size)
                    .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentKeyAlias(long id, String keyAlias) throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_KEY_ALIAS, keyAlias)
                    .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentLocalModificationTime(long id, long time) throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_LOCAL_MODIFICATION_TIME, time)
                    .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentBackEntryId(long id, String backEntryId) throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_ID, backEntryId)
                    .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentBackEntryVersion(long id, long version) throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_VERSION, version)
                    .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentRemoteModificationTime(long id, long time) throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_REMOTE_MODIFICATION_TIME, time)
                    .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentBackEntryFolderId(long id, long backEntryFolderId)
            throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_FOLDER_ID, backEntryFolderId)
                    .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentSyncState(long id, SyncAction syncAction, State state)
            throws DatabaseConnectionClosedException {
        try {
            String syncActionColumn;
            switch (syncAction) {
                case Upload:
                    syncActionColumn = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_UPLOAD_STATE;
                    break;
                case Download:
                    syncActionColumn = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DOWNLOAD_STATE;
                    break;
                case Deletion:
                    syncActionColumn = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DELETION_STATE;
                    break;
                default:
                    syncActionColumn = null;
            }
            if (null!=syncActionColumn) {
                UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
                updateBuilder.updateColumnValue(syncActionColumn, state)
                        .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
                updateBuilder.update();
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentBackEntryFolderLastSubfolderId(long id, long folderLastSubfolderId)
            throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_FOLDER_LAST_SUBFOLDER_ID,
                    folderLastSubfolderId);
            updateBuilder.where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentBackEntryNumSyncFailures(long id, int numSyncFailures)
            throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_NUM_SYNC_FAILURES,
                    numSyncFailures);
            updateBuilder.where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateEncryptedDocumentBackEntryLastFailureTime(long id, long lastFailureTime)
            throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<EncryptedDocument, Long> updateBuilder = getEncryptedDocumentDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_LAST_SYNC_FAILURE_TIME,
                    lastFailureTime);
            updateBuilder.where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void deleteEncryptedDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException {
        try {
            getEncryptedDocumentDao().deleteById(encryptedDocument.getId());
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public EncryptedDocument getEncryptedDocumentById(long id) throws DatabaseConnectionClosedException {
        EncryptedDocument encryptedDocument = null;
        try {
            encryptedDocument = getEncryptedDocumentDao().queryForId(id);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocument;
    }

    @Override
    public EncryptedDocument getEncryptedDocumentByNameAndParentId(String displayName, long parentId)
            throws DatabaseConnectionClosedException {
        EncryptedDocument encryptedDocument = null;
        try {
            encryptedDocument = getEncryptedDocumentDao().queryBuilder()
                    .where()
                    .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME, displayName)
                    .and()
                    .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, parentId)
                    .queryForFirst();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocument;
    }

    @Override
    public EncryptedDocument getEncryptedDocumentByAccountAndEntryId(Account account,
                                                                     String backEntryId)
            throws DatabaseConnectionClosedException {
        EncryptedDocument encryptedDocument = null;
        try {
            encryptedDocument = getEncryptedDocumentDao().queryBuilder()
                    .where()
                    .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_ACCOUNT, account)
                    .and()
                    .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_ID, backEntryId)
                    .queryForFirst();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocument;
    }

    @Override
    public List<EncryptedDocument> getAllEncryptedDocuments(boolean foldersFirst, OrderBy orderBy)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = null;
        try {
            if (foldersFirst) {
                encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                        .orderBy(getEncryptedDocumentOrderColumnName(orderBy),
                                isEncryptedDocumentOrderAscending(orderBy))
                        .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE,
                                Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE)
                        .query();
                encryptedDocuments.addAll(getEncryptedDocumentDao().queryBuilder()
                        .orderBy(getEncryptedDocumentOrderColumnName(orderBy),
                                isEncryptedDocumentOrderAscending(orderBy))
                        .where()
                        .ne(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE,
                                Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE)
                        .query());
            } else {
                encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                        .orderBy(getEncryptedDocumentOrderColumnName(orderBy),
                                isEncryptedDocumentOrderAscending(orderBy))
                        .query();
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocuments;
    }

    @Override
    public List<EncryptedDocument> getEncryptedDocumentsByParentId(long parentId, boolean foldersFirst)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = null;
        try {
            if (foldersFirst) {
                encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                        .where()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, parentId)
                        .and()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE,
                                Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE)
                        .query();
                encryptedDocuments.addAll(getEncryptedDocumentDao().queryBuilder()
                        .where()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, parentId)
                        .and()
                        .ne(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE,
                                Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE)
                        .query());
            } else {
                encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                        .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, parentId)
                        .query();
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocuments;
    }

    @Override
    public List<EncryptedDocument> getEncryptedDocumentsByParentId(long parentId, boolean foldersFirst,
                                                                   OrderBy orderBy)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = null;
        try {
            if (foldersFirst) {
                encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                        .orderBy(getEncryptedDocumentOrderColumnName(orderBy),
                                isEncryptedDocumentOrderAscending(orderBy))
                        .where()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, parentId)
                        .and()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE,
                                Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE)
                        .query();
                encryptedDocuments.addAll(getEncryptedDocumentDao().queryBuilder()
                        .orderBy(getEncryptedDocumentOrderColumnName(orderBy),
                                isEncryptedDocumentOrderAscending(orderBy))
                        .where()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, parentId)
                        .and()
                        .ne(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE,
                                Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE)
                        .query());
            } else {
                encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                        .orderBy(getEncryptedDocumentOrderColumnName(orderBy),
                                isEncryptedDocumentOrderAscending(orderBy))
                        .where().eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, parentId)
                        .query();
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocuments;
    }

    @Override
    public List<EncryptedDocument> getEncryptedDocumentsByKeyAlias(String keyAlias)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = null;
        try {
            encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                    .orderBy(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME, true)
                    .where()
                    .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_KEY_ALIAS, keyAlias)
                    .query();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocuments;
    }

    @Override
    public List<EncryptedDocument> getEncryptedDocumentsBySyncState(SyncAction syncAction, State state)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = null;
        try {
            String syncActionColumn;
            switch (syncAction) {
                case Upload:
                    syncActionColumn = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_UPLOAD_STATE;
                    break;
                case Download:
                    syncActionColumn = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DOWNLOAD_STATE;
                    break;
                case Deletion:
                    syncActionColumn = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DELETION_STATE;
                    break;
                default:
                    syncActionColumn = null;
            }
            if (null!=syncActionColumn) {
                encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                        .orderBy(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME, true)
                        .where().eq(syncActionColumn, state)
                        .query();
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocuments;
    }

    @Override
    public void removeEncryptedDocumentChildrenReferences(long id) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> children = getEncryptedDocumentsByParentId(id, false, OrderBy.NameAsc);
        for (EncryptedDocument child : children) {
            removeEncryptedDocumentChildrenReferences(child.getId());
            deleteEncryptedDocument(child);
        }
    }

    @Override
    public EncryptedDocument getRootEncryptedDocument(StorageType storageType, Account account)
            throws DatabaseConnectionClosedException {
        EncryptedDocument encryptedDocument = null;
        try {
            if (StorageType.Unsynchronized ==storageType) {
                encryptedDocument = getEncryptedDocumentDao().queryBuilder()
                        .where()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, Constants.STORAGE.ROOT_PARENT_ID)
                        .and()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_TYPE, storageType)
                        .queryForFirst();
            } else {
                encryptedDocument = getEncryptedDocumentDao().queryBuilder()
                        .where()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, Constants.STORAGE.ROOT_PARENT_ID)
                        .and()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_TYPE, storageType)
                        .and()
                        .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_ACCOUNT, account)
                        .queryForFirst();
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocument;
    }

    @Override
    public List<EncryptedDocument> getRootEncryptedDocuments() throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = null;
        try {
            encryptedDocuments = getEncryptedDocumentDao().queryBuilder()
                    .where()
                    .eq(DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID, Constants.STORAGE.ROOT_PARENT_ID)
                    .query();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return encryptedDocuments;
    }

    @Override
    public void addAccount(Account account) throws DatabaseConnectionClosedException {
        try {
            getAccountDao().create(account);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateAccount(Account account) throws DatabaseConnectionClosedException {
        try {
            getAccountDao().update(account);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void updateAccountChangesSyncState(long id, State state) throws DatabaseConnectionClosedException {
        try {
            UpdateBuilder<Account, Long> updateBuilder = getAccountDao().updateBuilder();
            updateBuilder.updateColumnValue(DatabaseConstants.ACCOUNT_COLUMN_CHANGES_SYNC_STATE, state)
                    .where().eq(DatabaseConstants.ACCOUNT_COLUMN_ID, id);
            updateBuilder.update();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void refreshAccount(Account account) throws DatabaseConnectionClosedException {
        try {
            getAccountDao().refresh(account);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public void deleteAccount(Account account) throws DatabaseConnectionClosedException {
        try {
            getAccountDao().deleteById(account.getId());
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public Account getAccountById(long id) throws DatabaseConnectionClosedException {
        Account account = null;
        try {
            account = getAccountDao().queryBuilder()
                    .where().eq(DatabaseConstants.ACCOUNT_COLUMN_ID, id)
                    .queryForFirst();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return account;
    }

    @Override
    public long getNumAccounts() throws DatabaseConnectionClosedException {
        try {
            return getAccountDao().queryBuilder().countOf();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return -1L;
    }

    @Override
    public List<Account> getAllAccounts() throws DatabaseConnectionClosedException {
        List<Account> accounts = null;
        try {
            accounts = getAccountDao().queryBuilder().query();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return accounts;
    }

    @Override
    public List<Account> getAccountsByChangesSyncState(State state) throws DatabaseConnectionClosedException {
        List<Account> accounts = null;
        try {
            accounts = getAccountDao().queryBuilder()
                    .where()
                    .ne(DatabaseConstants.ACCOUNT_COLUMN_STORAGE_TYPE, StorageType.Unsynchronized)
                    .and()
                    .eq(DatabaseConstants.ACCOUNT_COLUMN_CHANGES_SYNC_STATE, state)
                    .query();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return accounts;
    }

    @Override
    public Account getAccountByTypeAndName(StorageType storageType, String name) throws DatabaseConnectionClosedException {
        Account account = null;
        try {
            account = getAccountDao().queryBuilder()
                    .where()
                    .eq(DatabaseConstants.ACCOUNT_COLUMN_STORAGE_TYPE, storageType)
                    .and()
                    .eq(DatabaseConstants.ACCOUNT_COLUMN_NAME, name)
                    .queryForFirst();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return account;
    }

    @Override
    public List<Account> getAccountsByType(StorageType storageType) throws DatabaseConnectionClosedException {
        List<Account> accounts = null;
        try {
            accounts = getAccountDao().queryBuilder()
                    .where().eq(DatabaseConstants.ACCOUNT_COLUMN_STORAGE_TYPE, storageType)
                    .query();
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
        return accounts;
    }

    @Override
    public List<String> getAccountNamesByType(StorageType storageType) throws DatabaseConnectionClosedException {
        List<Account> accounts = getAccountsByType(storageType);
        if (null==accounts) {
            return null;
        }
        List<String> accountNames = new ArrayList<>();
        for (Account account : accounts) {
            accountNames.add(account.getAccountName());
        }
        return accountNames;
    }
}
