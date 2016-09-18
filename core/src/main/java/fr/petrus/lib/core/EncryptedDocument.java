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

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.RemoteStorage;
import fr.petrus.lib.core.cloud.exceptions.NetworkException;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.cloud.exceptions.UserCanceledException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.db.DatabaseConstants;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * This class holds information about encrypted documents.
 * <p/>
 * <p>It also performs various tasks related to document management.
 *
 * @author Pierre Sagne
 * @since 17.07.2015
 */
@DatabaseTable(tableName = DatabaseConstants.ENCRYPTED_DOCUMENTS_TABLE)
public class EncryptedDocument {
    private static Logger LOG = LoggerFactory.getLogger(EncryptedDocument.class);

    /* This class dependencies */
    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private FileSystem fileSystem = null;
    private TextI18n textI18n = null;
    private Database database = null;

    /** The id of this encrypted document in the database */
    @DatabaseField(generatedId = true, columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_ID)
    private long id;

    /** The clear text name of this document */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME)
    private String displayName;

    /** The MIME type of this document */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE)
    private String mimeType;

    /** The database id of the parent folder of this document */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID)
    private long parentId;

    /** The "encrypted" name of the physical file representing this encrypted document on disk */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_FILE_NAME)
    private String fileName;

    /** The size of the physical file representing this encrypted document on disk */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_SIZE)
    private long size;

    /**
     * The alias of the key used to encrypt this document (and the default key used to encrypt its
     * children, if it is a folder or a "root" folder
     */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_KEY_ALIAS)
    private String keyAlias;

    /** The last modification time of the "local" copy of the file associated to this document */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_LOCAL_MODIFICATION_TIME)
    private long localModificationTime;

    /** The last modification time of the "remote" copy of the file associated to this document, if any */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_REMOTE_MODIFICATION_TIME)
    private long remoteModificationTime;

    /** The "Storage Type" of this document : Unsynchronized if it is not synchronized, otherwise Dropbox, ... */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_TYPE)
    private StorageType backStorageType;

    /** The account where this document is synchronized, or null if StorageType is "Unsynchronized" */
    @DatabaseField(foreign = true, foreignAutoRefresh=true, columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_ACCOUNT)
    private Account backStorageAccount;

    /**
     * The remote document id on the remote storage where this document is stored or null if
     * StorageType is "Unsynchronized"
     */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_ID)
    private String backEntryId;

    /**
     * The remote version of this document on the remote storage where this document is stored
     * or null if StorageType is "Unsynchronized"
     */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_VERSION)
    private long backEntryVersion;

    /** Remote folder are stored with a simple integer as name. This field stores this identifier */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_FOLDER_ID)
    private long backEntryFolderId;

    /**
     * Remote folder are stored with a simple integer as name. This field stores the last
     * identifier assigned to a child folder of this folder
     */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_FOLDER_LAST_SUBFOLDER_ID)
    private long backEntryFolderLastSubfolderId;

    /** The state of the upload action of this document */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_UPLOAD_STATE)
    private State backEntryUploadState;

    /** The state of the download action of this document */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DOWNLOAD_STATE)
    private State backEntryDownloadState;

    /** The state of the deletion action of the remote document */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DELETION_STATE)
    private State backEntryDeletionState;

    /** The number of failed synchronization actions for this document, reset on success */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_NUM_SYNC_FAILURES)
    private int backEntryNumSyncFailures;

    /** The time of the last failed synchronization action for this document, reset on success */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_LAST_SYNC_FAILURE_TIME)
    private long backEntryLastFailureTime;

    /** If the back entry creation was started but the remote document is not in a stable state,
     *  this field is true, meaning we should try to complete it (metadata file of a folder missing,
     *  for example) */
    @DatabaseField(columnName = DatabaseConstants.ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_CREATION_INCOMPLETE)
    private boolean backEntryCreationIncomplete;

    /**
     * Creates a new empty {@code EncryptedDocument} instance, with default values.
     *
     * <p>Dependencies have to be set later, with the {@link EncryptedDocument#setDependencies} method.
     */
    EncryptedDocument() {
        id = -1;
        displayName = null;
        mimeType = null;
        parentId = -1;
        fileName = null;
        size = 0;
        keyAlias = null;
        localModificationTime = -1;
        remoteModificationTime = -1;
        backStorageType = null;
        backStorageAccount = null;
        backEntryId = null;
        backEntryVersion = -1;
        backEntryFolderId = -1;
        backEntryFolderLastSubfolderId = -1;
        backEntryUploadState = State.Done;
        backEntryDownloadState = State.Done;
        backEntryDeletionState = State.Done;
        backEntryNumSyncFailures = 0;
        backEntryLastFailureTime = -1;
        backEntryCreationIncomplete = false;
    }

    /**
     * Creates a new empty {@code EncryptedDocument} instance, with default values, setting its dependencies.
     *
     * @param crypto     a {@code Crypto} instance
     * @param keyManager a {@code KeyManager} instance
     * @param fileSystem a {@code FileSystem} instance
     * @param textI18n   a {@code TextI18n} instance
     * @param database   a {@code Database} instance
     */
    EncryptedDocument(Crypto crypto, KeyManager keyManager, FileSystem fileSystem,
                      TextI18n textI18n, Database database) {
        this();
        setDependencies(crypto, keyManager, fileSystem, textI18n, database);
    }

    /**
     * Sets the dependencies needed by this instance to perform its tasks.
     *
     * @param crypto     a {@code Crypto} instance
     * @param keyManager a {@code KeyManager} instance
     * @param fileSystem a {@code FileSystem} instance
     * @param textI18n   a {@code TextI18n} instance
     * @param database   a {@code Database} instance
     */
    public void setDependencies(Crypto crypto, KeyManager keyManager, FileSystem fileSystem,
                                TextI18n textI18n, Database database) {
        this.crypto = crypto;
        this.keyManager = keyManager;
        this.fileSystem = fileSystem;
        this.textI18n = textI18n;
        this.database = database;
    }

    /**
     * Sets the dependencies needed by this instance to perform its tasks, from the dependencies of
     * the given {@code srcEncryptedDocument}.
     *
     * @param srcEncryptedDocument the {@code EncryptedDocument} to get the dependencies from
     */
    public void setDependenciesFrom(EncryptedDocument srcEncryptedDocument) {
        setDependencies(srcEncryptedDocument.crypto, srcEncryptedDocument.keyManager,
                srcEncryptedDocument.fileSystem, srcEncryptedDocument.textI18n,
                srcEncryptedDocument.database);
        if (null!=backStorageAccount && null!=srcEncryptedDocument.backStorageAccount) {
            this.backStorageAccount.setDependenciesFrom(srcEncryptedDocument.backStorageAccount);
        }
    }

    /**
     * {@inheritDoc}
     * This implementation returns true if the other object is and an EncryptedDocument instance too,
     * and if the ids of both are equal, or if they have the same parent and name.
     *
     * @param obj the object this encrypted document is compared to
     * @return true if this encrypted document is considered equal to the object parameter, false otherwise
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
        EncryptedDocument other = (EncryptedDocument) obj;
        if (id != other.id) {
            if (parentId != other.parentId) {
                return false;
            }
            if (null == displayName || !displayName.equals(other.displayName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (int)(id % Integer.MAX_VALUE);
    }

    /**
     * Returns the database id of this encrypted document (-1 if it has not been inserted in
     * the database).
     *
     * @return the database id of this encrypted document
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the name of this encrypted document.
     *
     * @param displayName the name of this encrypted document
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the name of this encrypted document.
     *
     * @return the name of this encrypted document
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the MIME type of this encrypted document.
     *
     * @param mimeType the MIME type of this encrypted document
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the MIME type of this encrypted document.
     *
     * @return the MIME type of this encrypted document
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns whether this encrypted document represents a folder.
     *
     * @return true if this encrypted document represents a folder, false if it represents a file
     */
    public boolean isFolder() {
        return Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE.equals(getMimeType());
    }

    /**
     * Sets the database id of the encrypted document representing the parent folder
     * of this encrypted document.
     *
     * @param parentId the database id of the encrypted document representing the parent folder
     *                 of this encrypted document
     */
    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    /**
     * Returns the database id of the encrypted document representing the parent folder
     * of this encrypted document.
     *
     * @return the database id of the encrypted document representing the parent folder
     *         of this encrypted document
     */
    public long getParentId() {
        return parentId;
    }

    /**
     * Returns whether this encrypted document represents a "top level" folder.
     *
     * @return true if this encrypted document represents a "top level" folder, false if it
     *         represents a file
     */
    public boolean isRoot() {
        return getParentId()== Constants.STORAGE.ROOT_PARENT_ID;
    }

    /**
     * Returns whether this encrypted document represents the "top level" folder for unsynchronized
     * files.
     *
     * @return true if this encrypted document represents the "top level" folder for unsynchronized
     * files, false otherwise
     */
    public boolean isUnsynchronizedRoot() {
        return isRoot() && isUnsynchronized();
    }

    /**
     * Sets the file name of the physical document stored on disk.
     *
     * @param fileName the file name of the physical document stored on disk
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the file name of the physical document stored on disk.
     *
     * @return the file name of the physical document stored on disk
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the size of the contents of this file (in bytes).
     *
     * @param size the size of the contents of this file (in bytes)
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Returns the size of the contents of this file (in bytes).
     *
     * @return the size of the contents of this file (in bytes)
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the size of the contents of this file as a text with units.
     *
     * @return the size of the contents of this file as a text with units
     */
    public String getSizeText() {
        return textI18n.getSizeText(size);
    }

    /**
     * Sets the alias of the key used to encrypt this document.
     *
     * @param keyAlias the alias of the key used to encrypt this document
     */
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
        if (isRoot() && null!=backStorageAccount) {
            backStorageAccount.setDefaultKeyAlias(keyAlias);
        }
    }

    /**
     * Returns the alias of the key used to encrypt this document.
     *
     * @return the alias of the key used to encrypt this document
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Sets the last modification time of the "local" copy of the file associated to this document
     * (in ms from the epoch).
     *
     * @param time the last modification time of the "local" copy of the file associated to this
     *             document (in ms from the epoch)
     */
    public void setLocalModificationTime(long time) {
        this.localModificationTime = time;
    }

    /**
     * Returns the last modification time of the "local" copy of the file associated to this document
     * (in ms from the epoch).
     *
     * @return the last modification time of the "local" copy of the file associated to this
     *         document (in ms from the epoch)
     */
    public long getLocalModificationTime() {
        return localModificationTime;
    }

    /**
     * Sets the last modification time of the remote copy of the file associated to this document
     * (in ms from the epoch).
     *
     * @param time the last modification time of the remote copy of the file associated to this
     *             document (in ms from the epoch)
     */
    public void setRemoteModificationTime(long time) {
        this.remoteModificationTime = time;
    }

    /**
     * Returns the last modification time of the remote copy of the file associated to this document
     * (in ms from the epoch).
     *
     * @return the last modification time of the remote copy of the file associated to this
     *         document (in ms from the epoch)
     */
    public long getRemoteModificationTime() {
        return remoteModificationTime;
    }

    /**
     * Sets the "Storage Type" of this document : Unsynchronized if it is not synchronized, otherwise Dropbox, etc.
     *
     * @param backStorageType the "Storage Type" of this document
     */
    public void setBackStorageType(StorageType backStorageType) {
        this.backStorageType = backStorageType;
    }

    /**
     * Returns the "Storage Type" of this document : Unsynchronized if it is not synchronized, otherwise Dropbox, etc.
     *
     * @return the "Storage Type" of this document
     */
    public StorageType getBackStorageType() {
        return backStorageType;
    }

    /**
     * Returns whether this document is unsynchronized.
     *
     * @return true if this document is unsynchronized
     */
    public boolean isUnsynchronized() {
        return StorageType.Unsynchronized == backStorageType;
    }

    /**
     * Sets the account where this document is synchronized.
     *
     * @param backStorageAccount the account where this document is synchronized
     */
    public void setBackStorageAccount(Account backStorageAccount) {
        this.backStorageAccount = backStorageAccount;
    }

    /**
     * Returns the account where this document is synchronized.
     *
     * @return the account where this document is synchronized
     */
    public Account getBackStorageAccount() {
        return backStorageAccount;
    }

    /**
     * Sets the remote document id on the remote storage where this document is stored
     *
     * @param backEntryId the remote document id on the remote storage where this document is stored
     */
    public void setBackEntryId(String backEntryId) {
        this.backEntryId = backEntryId;
    }

    /**
     * Returns the remote document id on the remote storage where this document is stored
     *
     * @return the remote document id on the remote storage where this document is stored
     */
    public String getBackEntryId() {
        return backEntryId;
    }

    /**
     * Sets the version of the remote document on the remote storage.
     *
     * @param backEntryVersion the version of the remote document on the remote storage
     */
    public void setBackEntryVersion(long backEntryVersion) {
        this.backEntryVersion = backEntryVersion;
    }

    /**
     * Returns the version of the remote document on the remote storage.
     *
     * @return the version of the remote document on the remote storage
     */
    public long getBackEntryVersion() {
        return backEntryVersion;
    }

    /**
     * Returns a text describing the used size and the total size of the storage where this document
     * is stored (the local storage for local only documents, and the remote storage for synchronized
     * documents).
     *
     * @return a text describing the used size and the total size of the storage where this document
     *         is stored
     */
    public String getBackStorageQuotaText() {
        if (null!=backStorageAccount) {
            return backStorageAccount.getQuotaText();
        } else {
            File localAppFolder = fileSystem.getAppDir();
            long totalSize = localAppFolder.getTotalSpace();
            long freeSpace = localAppFolder.getFreeSpace();
            long sizeUsed = totalSize - freeSpace;
            return String.format("%s / %s",
                    textI18n.getSizeText(sizeUsed),
                    textI18n.getSizeText(totalSize));
        }
    }

    /**
     * Returns a text describing the type of storage where this document is stored.
     *
     * @return a text describing the type of storage where this document is stored
     */
    public String storageTypeText() {
        return textI18n.getStorageTypeText(getBackStorageType());
    }

    /**
     * Returns a text describing the storage where this document is stored (with the account name
     * for synchronized documents).
     *
     * @return a text describing the storage where this document is stored
     */
    public String storageText() {
        return textI18n.getStorageText(getBackStorageType(), getBackStorageAccount());
    }

    /**
     * Sets the folder identifier for the remote folder associated to this document.
     *
     * <p>Remote folder are stored with a simple integer as name. This method sets this identifier
     *
     * @param backEntryFolderId the folder identifier for the remote folder associated to this document
     */
    public void setBackEntryFolderId(long backEntryFolderId) {
        this.backEntryFolderId = backEntryFolderId;
    }

    /**
     * Returns the folder identifier for the remote folder associated to this document.
     *
     * <p>Remote folder are stored with a simple integer as name. This method sets this identifier
     *
     * @return the folder identifier for the remote folder associated to this document
     */
    public long getBackEntryFolderId() {
        return backEntryFolderId;
    }

    /**
     * Sets the last identifier assigned to a child folder of this folder.
     *
     * <p>Remote folder are stored with a simple integer as name. This method sets the last
     * identifier assigned to a child folder of this folder
     *
     * @param backEntryFolderLastSubfolderId the last identifier assigned to a child folder of this folder
     */
    public void setBackEntryFolderLastSubfolderId(long backEntryFolderLastSubfolderId) {
        this.backEntryFolderLastSubfolderId = backEntryFolderLastSubfolderId;
    }

    /**
     * Returns the last identifier assigned to a child folder of this folder.
     *
     * <p>Remote folder are stored with a simple integer as name. This method returns the last
     * identifier assigned to a child folder of this folder
     *
     * @return the last identifier assigned to a child folder of this folder
     */
    public long getBackEntryFolderLastSubfolderId() {
        return backEntryFolderLastSubfolderId;
    }

    /**
     * Sets the given {@code state} for the given {@code syncAction} of this document.
     *
     * @param syncAction the synchronization action to set
     * @param state      the state of the synchronization action to set
     */
    private void setSyncState(SyncAction syncAction, State state) {
        switch (syncAction) {
            case Upload:
                backEntryUploadState = state;
                break;
            case Download:
                backEntryDownloadState = state;
                break;
            case Deletion:
                backEntryDeletionState = state;
                break;
        }
    }

    /**
     * Returns the state for the given {@code syncAction} of this document.
     *
     * @param syncAction the synchronization action which state to return
     * @return           the state of the given {@code syncAction} of this document
     */
    public State getSyncState(SyncAction syncAction) {
        switch (syncAction) {
            case Upload:
                return backEntryUploadState;
            case Download:
                return backEntryDownloadState;
            case Deletion:
                return backEntryDeletionState;
            default:
                return null;
        }
    }

    /**
     * Sets the last time a synchronization action failed for this document (in ms from the epoch).
     *
     * <p>This method also increments the number of failed synchronization actions.
     *
     * @param lastFailureTime the last time a synchronization action failed for this document
     *                        (in ms from the epoch)
     */
    public void setBackEntryLastFailureTime(long lastFailureTime) {
        backEntryNumSyncFailures++;
        backEntryLastFailureTime = lastFailureTime;
    }

    /**
     * Resets the failed synchronization actions count and time.
     */
    public void resetBackEntryLastFailure() {
        backEntryNumSyncFailures = 0;
        backEntryLastFailureTime = -1;
    }

    /**
     * Returns the last time a synchronization action failed for this document (in ms from the epoch).
     *
     * @return the last time a synchronization action failed for this document (in ms from the epoch)
     */
    public long getBackEntryLastFailureTime() {
        return backEntryLastFailureTime;
    }

    /**
     * Returns the number of failed synchronization actions (since the last reset).
     *
     * @return the number of failed synchronization actions (since the last reset)
     */
    public int getBackEntryNumSyncFailures() {
        return backEntryNumSyncFailures;
    }

    /**
     * Sets whether the remote document creation was incomplete.
     *
     * @param backEntryCreationIncomplete true if the remote document creation was incomplete
     */
    public void setBackEntryCreationIncomplete(boolean backEntryCreationIncomplete) {
        this.backEntryCreationIncomplete = backEntryCreationIncomplete;
    }

    /**
     * Returns whether the remote document creation was incomplete.
     *
     * @return true if the remote document creation was incomplete
     */
    public boolean isBackEntryCreationIncomplete() {
        return backEntryCreationIncomplete;
    }

    /**
     * {@inheritDoc}
     * This implementation simply returns the name of this document.
     */
    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Persists this document to the database (it must already exist).
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void update() throws DatabaseConnectionClosedException {
        database.updateEncryptedDocument(this);
    }

    /**
     * Refreshes this document with the data of the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void refresh() throws DatabaseConnectionClosedException {
        database.refreshEncryptedDocument(this);
    }

    /**
     * Returns the parent folder of this encrypted document.
     *
     * @return the parent folder of this encrypted document
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument parent() throws DatabaseConnectionClosedException {
        EncryptedDocument parent = database.getEncryptedDocumentById(parentId);
        if (null!=parent) {
            parent.setDependenciesFrom(this);
        }
        return parent;
    }

    /**
     * Returns the parent folders of this encrypted document, starting from the root.
     *
     * @return the parent folders of this encrypted document, starting from the root
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> parents() throws DatabaseConnectionClosedException {
        LinkedList<EncryptedDocument> parents = new LinkedList<>();
        EncryptedDocument parent = parent();
        while (null!=parent) {
            parents.addFirst(parent);
            parent = parent.parent();
        }
        return parents;
    }

    /**
     * Returns the next identifier to assign to a child folder of this folder.
     *
     * <p>Remote folder are stored with a simple integer as name. This method increments the last
     * identifier assigned to a child folder of this folder, persists it into the database and
     * returns it
     *
     * @return the next identifier to assign to a child folder of this folder
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public long nextBackEntryFolderId() throws DatabaseConnectionClosedException {
        long backEntryFolderId = getBackEntryFolderLastSubfolderId() + 1;
        setBackEntryFolderLastSubfolderId(backEntryFolderId);
        database.updateEncryptedDocumentBackEntryFolderLastSubfolderId(getId(), backEntryFolderId);
        return backEntryFolderId;
    }

    /**
     * Returns the logical path of this document, built from the names of the hierarchy of its parents.
     *
     * @return the logical path of this document, built from the names of the hierarchy of its parents
     * @throws ParentNotFoundException if one of the parents of this document is missing from the database
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public String logicalPath() throws ParentNotFoundException, DatabaseConnectionClosedException {
        if (isRoot()) {
            return storageText();
        } else {
            EncryptedDocument parent = parent();
            if (null==parent) {
                throw new ParentNotFoundException(
                        "Error when building the logical path : of \""+getDisplayName()+"\" missing");
            }
            return parent.logicalPath() + "/" + getDisplayName();
        }
    }

    /**
     * Returns the logical path of this document, built from the names of the hierarchy of its parents.
     *
     * <p>This version doesn't throw any exception, but does its best to return something if an error
     * occurs.
     *
     * @return the logical path of this document, built from the names of the hierarchy of its parents
     */
    public String failSafeLogicalPath() {
        if (isRoot()) {
            return storageText();
        } else {
            try {
                return logicalPath();
            } catch (ParentNotFoundException e) {
                LOG.error("Parent not found for \"{}\"", storageText()+"/???/"+getDisplayName(), e);
            } catch (DatabaseConnectionClosedException e) {
                LOG.error("Database is closed");
            }
            return storageText()+"/???/"+getDisplayName();
        }
    }

    /**
     * Returns the physical file where the contents of this document is stored on the local storage.
     *
     * @return the physical file where the contents of this document is stored on the local storage
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public File file() throws DatabaseConnectionClosedException {
        if (!isRoot()) {
            EncryptedDocument parentEncryptedDocument = parent();
            if (null!=parentEncryptedDocument) {
                return new File(parentEncryptedDocument.file(), getFileName());
            }
        }
        switch (getBackStorageType()) {
            case Unsynchronized:
                return fileSystem.getLocalFilesDir();
            case GoogleDrive:
                return fileSystem.getGDriveFilesDir(getBackStorageAccount().getAccountName());
            case Dropbox:
                return fileSystem.getDropboxFilesDir(getBackStorageAccount().getAccountName());
            case Box:
                return fileSystem.getBoxFilesDir(getBackStorageAccount().getAccountName());
            case HubiC:
                return fileSystem.getHubicFilesDir(getBackStorageAccount().getAccountName());
            case OneDrive:
                return fileSystem.getOneDriveFilesDir(getBackStorageAccount().getAccountName());
        }
        return null;
    }

    /**
     * Returns the size of the physical file where the contents of this document is stored on the
     * local storage.
     *
     * @return the size of the physical file where the contents of this document is stored on the
     *         local storage
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public long fileSize() throws DatabaseConnectionClosedException {
        File file = file();
        if (null==file) {
            return 0L;
        }
        return file.length();
    }

    /**
     * Creates a new encrypted document as a child of this folder.
     *
     * @param displayName the name of the document to create
     * @param mimeType    the MIME type of the document to create
     * @param keyAlias    the alias of the key to encrypt this new document with
     * @return the newly created encrypted document
     * @throws StorageCryptException             if an error occurs when creating the document
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument createChild(String displayName, String mimeType, String keyAlias)
            throws StorageCryptException, DatabaseConnectionClosedException {

        EncryptedDocument encryptedDocument = child(displayName);
        if (null!= encryptedDocument) {
            throw new StorageCryptException("Creation failed : a document named "+displayName+" already exists",
                    StorageCryptException.Reason.DocumentExists);
        }

        String fileName;
        try {
            EncryptedDocumentMetadata metadata = new EncryptedDocumentMetadata(crypto, keyManager);
            metadata.setMetadata(mimeType, displayName, keyAlias);
            fileName = metadata.encryptToBase64();
        } catch (StorageCryptException e) {
            if (StorageCryptException.Reason.KeyNotFound == e.getReason()) {
                throw new StorageCryptException("Error while encrypting file name",
                        StorageCryptException.Reason.KeyNotFound, e);
            } else {
                throw new StorageCryptException("Error while encrypting file name",
                        StorageCryptException.Reason.EncryptionError, e);
            }
        }

        boolean isDir = Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE.equals(mimeType);

        File newFile = new File(file(), fileName);
        try {
            boolean result;
            if (isDir) {
                result = newFile.mkdirs();
            } else {
                result = newFile.createNewFile();
            }
            if (!result) {
                throw new StorageCryptException("Error while creating document " + newFile.getPath(),
                        StorageCryptException.Reason.CreationError);
            }

            long now = System.currentTimeMillis();
            encryptedDocument = new EncryptedDocument(crypto, keyManager, fileSystem, textI18n, database);
            encryptedDocument.setDisplayName(displayName);
            encryptedDocument.setMimeType(mimeType);
            encryptedDocument.setParentId(getId());
            encryptedDocument.setFileName(fileName);
            encryptedDocument.setKeyAlias(keyAlias);
            encryptedDocument.setLocalModificationTime(now);
            encryptedDocument.setBackStorageType(backStorageType);
            if (encryptedDocument.isUnsynchronized()) {
                encryptedDocument.setSyncState(SyncAction.Upload, State.Done);
            } else {
                encryptedDocument.setBackStorageAccount(getBackStorageAccount());
                encryptedDocument.setSyncState(SyncAction.Upload, State.Planned);
            }
            encryptedDocument.add();
            return encryptedDocument;
        } catch (IOException e) {
            throw new StorageCryptException("Error while creating document " + newFile.getPath(),
                    StorageCryptException.Reason.CreationError, e);
        }
    }

    /**
     * Creates an encrypted document as a child of this folder from the data of an existing encrypted
     * remote document.
     *
     * @param encryptedDocumentMetadata the encrypted document metadata obtained by decrypting the
     *                                  metadata of the remote document.
     * @param encryptedName             the encrypted name of the remote document
     * @param remoteDocument            the remote document
     * @return the newly created encrypted document
     * @throws StorageCryptException             if an error occurs when creating the document
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument createChild(EncryptedDocumentMetadata encryptedDocumentMetadata,
                                         String encryptedName, RemoteDocument remoteDocument)
            throws StorageCryptException, DatabaseConnectionClosedException {

        EncryptedDocument encryptedDocument = child(encryptedDocumentMetadata.getDisplayName());
        if (null!= encryptedDocument) {
            throw new StorageCryptException(
                    "Creation failed : a document named "+encryptedDocumentMetadata.getDisplayName()+" already exists",
                    StorageCryptException.Reason.DocumentExists);
        }

        encryptedDocument = new EncryptedDocument(crypto, keyManager, fileSystem, textI18n, database);
        encryptedDocument.setDisplayName(encryptedDocumentMetadata.getDisplayName());
        encryptedDocument.setMimeType(encryptedDocumentMetadata.getMimeType());
        encryptedDocument.setParentId(getId());
        encryptedDocument.setFileName(encryptedName);
        encryptedDocument.setKeyAlias(encryptedDocumentMetadata.getKeyAlias());
        encryptedDocument.setRemoteModificationTime(remoteDocument.getModificationTime());
        encryptedDocument.setBackStorageType(getBackStorageType());
        encryptedDocument.setBackStorageAccount(getBackStorageAccount());
        encryptedDocument.setSize(remoteDocument.getSize());
        encryptedDocument.setBackEntryVersion(remoteDocument.getVersion());
        if (remoteDocument.isFolder()) {
            encryptedDocument.setSyncState(SyncAction.Download, State.Done);
            long backEntryFolderId = Long.parseLong(remoteDocument.getName());
            if (getBackEntryFolderLastSubfolderId() < backEntryFolderId) {
                updateBackEntryFolderLastSubfolderId(backEntryFolderId);
            }
            encryptedDocument.setBackEntryFolderId(backEntryFolderId);
            File folder = encryptedDocument.file();
            folder.mkdirs();
            encryptedDocument.setLocalModificationTime(folder.lastModified());
        } else {
            encryptedDocument.setSyncState(SyncAction.Download, State.Planned);
        }
        encryptedDocument.setBackEntryId(remoteDocument.getId());
        encryptedDocument.add();
        return encryptedDocument;
    }

    /**
     * Creates an encrypted document as a child of this folder from the data of an existing encrypted
     * local file.
     *
     * @param encryptedDocumentMetadata the encrypted document metadata obtained by decrypting the
     *                                  name of the local document.
     * @param document                  the encrypted local file
     * @return the newly created encrypted document
     * @throws StorageCryptException             if an error occurs when creating the document
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument createChild(EncryptedDocumentMetadata encryptedDocumentMetadata, File document)
        throws StorageCryptException, DatabaseConnectionClosedException {

        EncryptedDocument encryptedDocument = child(encryptedDocumentMetadata.getDisplayName());
        if (null != encryptedDocument) {
            throw new StorageCryptException(
                    "Creation failed : a document named "+encryptedDocumentMetadata.getDisplayName()+" already exists",
                    StorageCryptException.Reason.DocumentExists);
        }

        encryptedDocument = new EncryptedDocument(crypto, keyManager, fileSystem, textI18n, database);
        encryptedDocument.setDisplayName(encryptedDocumentMetadata.getDisplayName());
        encryptedDocument.setMimeType(encryptedDocumentMetadata.getMimeType());
        encryptedDocument.setParentId(getId());
        encryptedDocument.setFileName(document.getName());
        encryptedDocument.setKeyAlias(encryptedDocumentMetadata.getKeyAlias());
        encryptedDocument.setSize(document.length());
        encryptedDocument.setLocalModificationTime(document.lastModified());
        encryptedDocument.setBackStorageType(getBackStorageType());
        if (encryptedDocument.isUnsynchronized()) {
            encryptedDocument.setSyncState(SyncAction.Upload, State.Done);
        } else {
            encryptedDocument.setBackStorageAccount(getBackStorageAccount());
            encryptedDocument.setSyncState(SyncAction.Upload, State.Planned);
        }
        encryptedDocument.add();
        return encryptedDocument;
    }

    /**
     * Adds this encrypted document to the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void add() throws DatabaseConnectionClosedException {
        database.addEncryptedDocument(this);
    }

    /**
     * Returns the child of this folder, which has the given {@code name}.
     *
     * @param name the name of the child document to search
     * @return the child of this folder, which has the given {@code name}, or null if not found
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public EncryptedDocument child(String name) throws DatabaseConnectionClosedException {
        EncryptedDocument child = database.getEncryptedDocumentByNameAndParentId(name, getId());
        if (null!=child) {
            child.setDependenciesFrom(this);
        }
        return child;
    }

    /**
     * Returns the list of all the children encrypted documents of this folder.
     *
     * @param foldersFirst if true, the folders are listed before the files, otherwise they are mixed
     * @return the list of all the children encrypted documents of this folder
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> children(boolean foldersFirst) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> children = database.getEncryptedDocumentsByParentId(getId(), foldersFirst);
        for (EncryptedDocument child : children) {
            child.setDependenciesFrom(this);
        }
        return children;
    }

    /**
     * Returns the list of all the children encrypted documents of this folder, sorted by the given
     * {@code orderBy} criterion.
     *
     * @param foldersFirst if true, the folders are listed before the files, otherwise they are mixed
     * @param orderBy      the criterion used to sort the documents
     * @return the list of all the children encrypted documents of this folder, sorted by the given
     *         {@code orderBy} criterion
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> children(boolean foldersFirst, OrderBy orderBy)
            throws DatabaseConnectionClosedException {
        List<EncryptedDocument> children =
                database.getEncryptedDocumentsByParentId(getId(), foldersFirst, orderBy);
        for (EncryptedDocument child : children) {
            child.setDependenciesFrom(this);
        }
        return children;
    }

    /**
     * Removes the children of this folder from the database, without removing the physical and remote
     * documents themselves.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void removeChildrenReferences() throws DatabaseConnectionClosedException {
        database.removeEncryptedDocumentChildrenReferences(getId());
    }

    /**
     * Sets the size of this document content by checking the physical file, then persists the
     * result into the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateFileSize() throws DatabaseConnectionClosedException {
        setSize(fileSize());
        database.updateEncryptedDocumentSize(getId(), getSize());
    }

    /**
     * Sets the alias of the key used to encrypt this document, then persists it into the database.
     *
     * @param keyAlias the alias of the key used to encrypt this document
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateKeyAlias(String keyAlias) throws DatabaseConnectionClosedException {
        setKeyAlias(keyAlias);
        database.updateEncryptedDocumentKeyAlias(getId(), getKeyAlias());
        if (isRoot() && null!=backStorageAccount) {
            backStorageAccount.refresh();
            backStorageAccount.setDefaultKeyAlias(keyAlias);
            backStorageAccount.update();
        }
    }

    /**
     * Sets the last modification time of the "local" copy of the file associated to this document
     * (in ms from the epoch), then persists it into the database.
     *
     * @param time the last modification time of the "local" copy of the file associated to this
     *             document (in ms from the epoch)
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateLocalModificationTime(long time) throws DatabaseConnectionClosedException {
        setLocalModificationTime(time);
        database.updateEncryptedDocumentLocalModificationTime(getId(), getLocalModificationTime());
    }

    /**
     * Sets the last modification time of the "remote" copy of the file associated to this document
     * (in ms from the epoch), then persists it into the database.
     *
     * @param time the last modification time of the "remote" copy of the file associated to this
     *             document (in ms from the epoch)
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateRemoteModificationTime(long time) throws DatabaseConnectionClosedException {
        setRemoteModificationTime(time);
        database.updateEncryptedDocumentRemoteModificationTime(getId(), getRemoteModificationTime());
    }

    /**
     * Sets the remote document id on the remote storage where this document is stored, then
     * persists it into the database
     *
     * @param backEntryId the remote document id on the remote storage where this document is stored
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateBackEntryId(String backEntryId) throws DatabaseConnectionClosedException {
        setBackEntryId(backEntryId);
        database.updateEncryptedDocumentBackEntryId(getId(), getBackEntryId());
    }

    /**
     * Sets the folder identifier for the remote folder associated to this document, then
     * persists it into the database.
     *
     * <p>Remote folder are stored with a simple integer as name. This method sets this identifier
     *
     * @param backEntryFolderId the folder identifier for the remote folder associated to this document
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateBackEntryFolderId(long backEntryFolderId) throws DatabaseConnectionClosedException {
        setBackEntryFolderId(backEntryFolderId);
        database.updateEncryptedDocumentBackEntryFolderId(getId(), getBackEntryFolderId());
    }

    /**
     * Sets the last identifier assigned to a child folder of this folder, then persists it into
     * the database.
     *
     * <p>Remote folder are stored with a simple integer as name. This method sets the last
     * identifier assigned to a child folder of this folder
     *
     * @param backEntryFolderLastSubfolderId the last identifier assigned to a child folder of this folder
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateBackEntryFolderLastSubfolderId(long backEntryFolderLastSubfolderId) throws DatabaseConnectionClosedException {
        setBackEntryFolderLastSubfolderId(backEntryFolderLastSubfolderId);
        database.updateEncryptedDocumentBackEntryFolderLastSubfolderId(getId(), getBackEntryFolderLastSubfolderId());
    }

    /**
     * Sets the version of the remote document on the remote storage, then persists it into
     * the database.
     *
     * @param backEntryVersion the version of the remote document on the remote storage
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateBackEntryVersion(long backEntryVersion) throws DatabaseConnectionClosedException {
        setBackEntryVersion(backEntryVersion);
        database.updateEncryptedDocumentBackEntryVersion(getId(), getBackEntryVersion());
    }

    /**
     * Sets whether the remote document creation was incomplete, then persists it into the database.
     *
     * @param backEntryCreationIncomplete true if the remote document creation was incomplete
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateBackEntryCreationIncomplete(boolean backEntryCreationIncomplete)
            throws DatabaseConnectionClosedException {
        setBackEntryCreationIncomplete(backEntryCreationIncomplete);
        database.updateEncryptedDocumentBackEntryCreationIncomplete(getId(), isBackEntryCreationIncomplete());
    }

    /**
     * Sets the given {@code state} for the given {@code syncAction} of this document, then persists
     * it into the database.
     *
     * @param syncAction the synchronization action to set
     * @param state      the state of the synchronization action to set
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void updateSyncState(SyncAction syncAction, State state) throws DatabaseConnectionClosedException {
        setSyncState(syncAction, state);
        database.updateEncryptedDocumentSyncState(getId(), syncAction, state);
        if (State.Planned == state) {
            resetFailuresCount();
        }
    }

    /**
     * Increments the failed synchronization actions count and sets the last failure time now, then
     * persists this information into the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void incrementFailuresCount() throws DatabaseConnectionClosedException {
        setBackEntryLastFailureTime(System.currentTimeMillis());
        database.updateEncryptedDocumentBackEntryNumSyncFailures(getId(), getBackEntryNumSyncFailures());
        database.updateEncryptedDocumentBackEntryLastFailureTime(getId(), getBackEntryLastFailureTime());
    }

    /**
     * Resets the failed synchronization actions count and time now, then persists this information
     * into the database.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void resetFailuresCount() throws DatabaseConnectionClosedException {
        resetBackEntryLastFailure();
        database.updateEncryptedDocumentBackEntryNumSyncFailures(getId(), getBackEntryNumSyncFailures());
        database.updateEncryptedDocumentBackEntryLastFailureTime(getId(), getBackEntryLastFailureTime());
    }

    /**
     * Requests the remote document associated with this encrypted document.
     *
     * @return the remote document associated with this encrypted document, or null if this document
     *         is not synchronized
     * @throws StorageCryptException             if an error occurs when requesting the remote
     *                                           document
     * @throws NetworkException                  if a network connectivity error occurs
     * @throws NotFoundException                 if the remote document does not exist
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public RemoteDocument remoteDocument() throws StorageCryptException,
            DatabaseConnectionClosedException, NetworkException, NotFoundException {
        if (isUnsynchronized()) {
            return null;
        }
        RemoteStorage storage = getBackStorageAccount().getRemoteStorage();
        if (isRoot()) {
            try {
                return storage.appFolder(getBackStorageAccount().getAccountName());
            } catch (RemoteException e) {
                if (e.isNotFoundError()) {
                    throw new NotFoundException("Remote document not found", e);
                } else {
                    throw new StorageCryptException("Failed to get remote app folder",
                            StorageCryptException.Reason.GetRemoteAppFolderError, e);
                }
            }
        } else if (isFolder()) {
            try {
                return storage.folder(getBackStorageAccount().getAccountName(), getBackEntryId());
            } catch (RemoteException e) {
                if (e.isNotFoundError()) {
                    throw new NotFoundException("Remote document not found", e);
                } else {
                    throw new StorageCryptException("Failed to get remote folder",
                            StorageCryptException.Reason.GetRemoteFolderError, e);
                }
            }
        } else {
            try {
                return storage.file(getBackStorageAccount().getAccountName(), getBackEntryId());
            } catch (RemoteException e) {
                if (e.isNotFoundError()) {
                    throw new NotFoundException("Remote document not found", e);
                } else {
                    throw new StorageCryptException("Failed to get remote file",
                            StorageCryptException.Reason.GetRemoteFileError, e);
                }
            }
        }
    }

    /**
     * Uploads this document to the associated account.
     *
     * <p>The remote document must not already exist
     *
     * @param listener the listener which the upload progress will be reported to, and which handles
     *                 canceling, pausing and resuming the upload process
     * @throws StorageCryptException             if an error occurs when uploading
     * @throws NetworkException                  if a network connectivity error occurs
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void uploadNew(ProcessProgressListener listener) throws StorageCryptException, DatabaseConnectionClosedException, NetworkException, UserCanceledException {
        if (!isUnsynchronized()) {
            EncryptedDocument parentEncryptedDocument = parent();
            try {
                if (parentEncryptedDocument.isRoot() && !parentEncryptedDocument.isUnsynchronizedRoot()) {
                    parentEncryptedDocument.checkRemoteRoot();
                }
            } catch (StorageCryptException e) {
                LOG.error("Failed to refresh root document", e);
                throw e;
            }

            String accountName = getBackStorageAccount().getAccountName();
            RemoteStorage storage = getBackStorageAccount().getRemoteStorage();
            if (null == storage) {
                LOG.error("RemoteStorage instance not found for storage type {}", getBackStorageType().name());
                return;
            }
            RemoteDocument parent;
            try {
                try {
                    parent = storage.folder(accountName, parentEncryptedDocument.getBackEntryId());
                } catch (RemoteException e) {
                    incrementFailuresCount();
                    throw new StorageCryptException("Failed to upload new document : impossible to get parent",
                            StorageCryptException.Reason.RemoteCreationErrorFailedToGetParent, e);
                }
                try {
                    RemoteDocument document = null;
                    if (isFolder()) {
                        if (null != listener) {
                            listener.onSetMax(0, 2);
                            listener.onProgress(0, 0);
                        }
                        // try to get a free folder id
                        long backEntryFolderId;
                        do {
                            backEntryFolderId = parentEncryptedDocument.nextBackEntryFolderId();
                            try {
                                document = parent.childFolder(String.valueOf(backEntryFolderId));
                            } catch (RemoteException e) {
                                if (!e.isNotFoundError()) {
                                    throw e;
                                }
                            }
                        } while (null != document);
                        if (null != listener) {
                            listener.onProgress(0, 1);
                        }
                        document = parent.createChildFolderWithMetadata(String.valueOf(backEntryFolderId),
                                crypto.decodeUrlSafeBase64(getFileName()));
                        updateBackEntryFolderId(backEntryFolderId);
                        if (null != listener) {
                            listener.onProgress(0, 2);
                        }
                    } else {
                        document = parent.uploadNewChildFile(getFileName(),
                                Constants.STORAGE.DEFAULT_BINARY_MIME_TYPE, file(), listener);
                    }
                    if (null != document) {
                        updateBackEntryId(document.getId());
                        updateBackEntryVersion(document.getVersion());
                        updateRemoteModificationTime(document.getModificationTime());
                        if (document.isCreationIncomplete()) {
                            updateBackEntryCreationIncomplete(document.isCreationIncomplete());
                        } else {
                            updateSyncState(SyncAction.Upload, State.Done);
                        }
                    }
                } catch (RemoteException e) {
                    incrementFailuresCount();
                    throw new StorageCryptException("Failed to create remote document",
                            StorageCryptException.Reason.RemoteCreationError, e);
                }
            } catch (NetworkException e) {
                incrementFailuresCount();
                throw e;
            }
            resetFailuresCount();
        }
    }

    /**
     * Uploads this document to the associated account, if any.
     *
     * <p>The remote document must already exist
     *
     * @param listener the listener which the upload progress will be reported to, and which handles
     *                 canceling, pausing and resuming the upload process
     * @throws StorageCryptException             if an error occurs when uploading
     * @throws NetworkException                  if a network connectivity error occurs
     * @throws NotFoundException                 if the remote document does not exist
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void upload(ProcessProgressListener listener) throws StorageCryptException, DatabaseConnectionClosedException, NetworkException, NotFoundException, UserCanceledException {
        if (!isUnsynchronized()) {
            if (isFolder()) {
                throw new StorageCryptException("Failed to upload document : folders cannot be uploaded",
                        StorageCryptException.Reason.FoldersNotAllowed);
            }
            String accountName = getBackStorageAccount().getAccountName();
            RemoteStorage storage = getBackStorageAccount().getRemoteStorage();
            if (null == storage) {
                LOG.error("RemoteStorage instance not found for storage type {}", getBackStorageType().name());
                return;
            }
            RemoteDocument document;
            try {
                try {
                    document = storage.file(accountName, getBackEntryId());
                } catch (RemoteException e) {
                    if (e.isNotFoundError()) {
                        throw new NotFoundException("The remote file does not exist", e);
                    } else {
                        incrementFailuresCount();
                        throw new StorageCryptException("Failed to upload document : impossible to get document metadata",
                                StorageCryptException.Reason.FailedToGetMetadata, e);
                    }
                }
                try {
                    document = document.uploadFile(Constants.STORAGE.DEFAULT_BINARY_MIME_TYPE, file(), listener);

                    Account account = getBackStorageAccount();
                    account.refresh();
                    account.setEstimatedQuotaUsed(account.getEstimatedQuotaUsed() + getSize());
                    account.update();
                    account.refreshQuotaIfNeeded();
                } catch (RemoteException e) {
                    incrementFailuresCount();
                    throw new StorageCryptException("Failed to upload document",
                            StorageCryptException.Reason.UploadError, e);
                }
            } catch (NetworkException e) {
                incrementFailuresCount();
                throw e;
            }
            updateBackEntryVersion(document.getVersion());
            updateRemoteModificationTime(document.getModificationTime());
            updateSyncState(SyncAction.Upload, State.Done);
            resetFailuresCount();
        }
    }

    /**
     * Downloads this document to the associated account, if any.
     *
     * @param listener the listener which the download progress will be reported to, and which handles
     *                 canceling, pausing and resuming the download process
     * @throws StorageCryptException             if an error occurs when downloading
     * @throws NetworkException                  if a network connectivity error occurs
     * @throws NotFoundException                 if the remote document does not exist
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void download(ProcessProgressListener listener) throws StorageCryptException, DatabaseConnectionClosedException, NetworkException, NotFoundException, UserCanceledException {
        if (!isUnsynchronized()) {
            if (isFolder()) {
                throw new StorageCryptException("Failed to download document : folders cannot be downloaded",
                        StorageCryptException.Reason.FoldersNotAllowed);
            }
            String accountName = getBackStorageAccount().getAccountName();
            RemoteStorage storage = getBackStorageAccount().getRemoteStorage();
            if (null == storage) {
                LOG.error("RemoteStorage instance not found for storage type ", getBackStorageType().name());
                return;
            }
            RemoteDocument document;
            try {
                try {
                    document = storage.file(accountName, getBackEntryId());
                } catch (RemoteException e) {
                    if (e.isNotFoundError()) {
                        throw new NotFoundException("The remote file does not exist", e);
                    } else {
                        incrementFailuresCount();
                        throw new StorageCryptException("Failed to download document : impossible to get document metadata",
                                StorageCryptException.Reason.FailedToGetMetadata, e);
                    }
                }
                try {
                    document.downloadFile(file(), listener);
                } catch (RemoteException e) {
                    incrementFailuresCount();
                    throw new StorageCryptException("Failed to download document",
                            StorageCryptException.Reason.DownloadError, e);
                }
            } catch (NetworkException e) {
                incrementFailuresCount();
                throw e;
            }
            updateLocalModificationTime(System.currentTimeMillis());
            updateRemoteModificationTime(document.getModificationTime());
            updateBackEntryVersion(document.getVersion());
            updateSyncState(SyncAction.Download, State.Done);
            resetFailuresCount();
        }
    }

    /**
     * Tries to fix an incomplete remote document creation
     */
    public void tryToFixIncompleteCreation()
            throws DatabaseConnectionClosedException, StorageCryptException, NetworkException {
        if (isUnsynchronized() || isRoot() || !isBackEntryCreationIncomplete()) {
            return;
        }

        RemoteDocument remoteDocument;
        if (null != getBackEntryId()) {
            try {
                remoteDocument = remoteDocument();
            } catch (NotFoundException e) {
                LOG.error("Impossible to fix incomplete creation for document {} because we could not get the remote file",
                        failSafeLogicalPath(), e);
                return;
            }
        } else {
            // try to retrieve remote document ID if partially created
            try {
                RemoteDocument parentRemoteDocument = parent().remoteDocument();
                remoteDocument = parentRemoteDocument.childDocument(getDisplayName());
                // if the remote document was already created, get its ID
                updateBackEntryId(remoteDocument.getId());
            } catch (NotFoundException | RemoteException e) {
                LOG.error("Impossible to fix incomplete creation for document {} because we could not retrieve its ID",
                        failSafeLogicalPath(), e);
                return;
            }
        }

        try {
            remoteDocument.tryToFixIncompleteDocumentCreation(crypto.decodeUrlSafeBase64(getFileName()));
        } catch (RemoteException e) {
            LOG.error("Impossible to fix incomplete creation for document {}",
                    failSafeLogicalPath(), e);
        }
    }

    /**
     * Deletes the physical file holding the content of this document, then requests the deletion
     * of the associated remote document if any.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void delete() throws DatabaseConnectionClosedException {
        if (isUnsynchronized()) {
            deleteLocal();
        } else {
            //if there is a remote document associated, mark it for deletion on the next sync
            updateSyncState(SyncAction.Deletion, State.Planned);
        }
    }

    /**
     * Deletes the physical file holding the content of this document, then requests the deletion
     * of the associated remote document if any.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void deleteLocal() throws DatabaseConnectionClosedException {
        // physically remove the file
        file().delete();

        //delete the encryptedDocument if it is strictly local
        database.deleteEncryptedDocument(this);
    }

    /**
     * Deletes the remote document associated with this document.
     *
     * @throws StorageCryptException             if an error occurs when deleting the remote document
     * @throws NetworkException                  if a network connectivity error occurs
     * @throws NotFoundException                 if the remote document does not exist
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void deleteRemote() throws StorageCryptException, DatabaseConnectionClosedException, NetworkException, NotFoundException {
        if (!isUnsynchronized() && null!=getBackEntryId()) {
            //remove the remote file
            RemoteStorage storage = getBackStorageAccount().getRemoteStorage();
            if (null == storage) {
                LOG.error("RemoteStorage instance not found for storage type {}", getBackStorageType().name());
                return;
            }
            try {
                if (isFolder()) {
                    storage.deleteFolder(getBackStorageAccount().getAccountName(), getBackEntryId());
                } else {
                    storage.deleteFile(getBackStorageAccount().getAccountName(), getBackEntryId());

                    Account account = getBackStorageAccount();
                    account.refresh();
                    account.setEstimatedQuotaUsed(account.getEstimatedQuotaUsed() - getSize());
                    account.update();
                    account.refreshQuotaIfNeeded();
                }
            } catch (NetworkException e) {
                incrementFailuresCount();
                throw e;
            } catch (RemoteException e) {
                if (e.isNotFoundError()) {
                    throw new NotFoundException("The remote document does not exist", e);
                } else {
                    incrementFailuresCount();
                    throw new StorageCryptException("Error while deleting remote document",
                            StorageCryptException.Reason.DeletionError, e);
                }
            } catch (DatabaseConnectionClosedException e) {
                LOG.error("Database is closed", e);
                throw e;
            }
            database.deleteEncryptedDocument(this);
        }
    }

    /**
     * Removes this "top level" folder from the database, removing all children as well.
     *
     * <p>The associated account is also removed from the database, and the access token is revoked,
     * if the remote storage supports it.
     *
     * <p>The remote documents are not deleted.
     *
     * @throws StorageCryptException             if an error occurs when calling the underlying API
     * @throws NetworkException                  if a network connectivity error occurs
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void deleteRoot() throws StorageCryptException, DatabaseConnectionClosedException, NetworkException {
        if (!isUnsynchronized()) {
            RemoteStorage storage = getBackStorageAccount().getRemoteStorage();
            if (null == storage) {
                LOG.error("RemoteStorage instance not found for storage type {}", getBackStorageType().name());
                return;
            }
            try {
                storage.revokeToken(getBackStorageAccount().getAccountName());
            } catch (RemoteException e) {
                throw new StorageCryptException("Error when revoking token",
                        StorageCryptException.Reason.TokenRevocationError, e);
            }
            storage.removeAccount(getBackStorageAccount().getAccountName());
            database.deleteEncryptedDocument(this);
        }
    }

    /**
     * Sends a request to the remote storage to try to recover the remote entry id of this document,
     * if something went wrong when creating it before.
     *
     * @throws StorageCryptException             if an error occurs when calling the underlying API
     * @throws NetworkException                  if a network connectivity error occurs
     * @throws NotFoundException                 if the remote document does not exist
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void tryToRecoverBackEntryId() throws StorageCryptException, DatabaseConnectionClosedException, NetworkException, NotFoundException {
        if (isUnsynchronized()) {
            return;
        }

        EncryptedDocument parent = parent();
        if (null==parent) {
            LOG.error("Parent not found for document {}",getDisplayName());
            return;
        }

        if (null==parent.getBackEntryId()) {
            parent.tryToRecoverBackEntryId();
        }

        if (null==parent.getBackEntryId()) {
            LOG.error("Impossible to get {} parent remote id", getDisplayName());
            return;
        }

        RemoteDocument remoteParent;
        try {
            remoteParent = parent.remoteDocument();
        } catch (NotFoundException e) {
            throw new StorageCryptException(
                    "Impossible to get document remote parent",
                    StorageCryptException.Reason.ParentNotFound, e);
        }
        try {
            RemoteDocument remoteDocument = remoteParent.childDocument(getFileName());
            updateBackEntryId(remoteDocument.getId());
        } catch (RemoteException e) {
            if (e.isNotFoundError()) {
                throw new NotFoundException("Remote document not found", e);
            } else {
                throw new StorageCryptException(
                        "Impossible to get child documents for " + remoteParent.getName(),
                        StorageCryptException.Reason.FileNotFound, e);
            }
        }
    }

    /**
     * If this document represents the "top level" folder of a synchronized tree, tries to get the
     * remote id of the app folder, stores it in this document remote id, and persists it into the
     * database.
     *
     * @throws StorageCryptException             if an error occurs when calling the underlying API
     * @throws NetworkException                  if a network connectivity error
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void checkRemoteRoot() throws StorageCryptException, DatabaseConnectionClosedException, NetworkException {
        if (isRoot()) {
            if (!isUnsynchronized()) {
                RemoteStorage storage = getBackStorageAccount().getRemoteStorage();
                if (null==storage) {
                    LOG.error("RemoteStorage instance not found for storage type {}", getBackStorageType().name());
                    return;
                }
                try {
                    RemoteDocument document = storage.appFolder(getBackStorageAccount().getAccountName());
                    if (null!=document) {
                        updateBackEntryId(document.getId());
                    }
                } catch (RemoteException e) {
                    throw new StorageCryptException("Error when refreshing remote root",
                            StorageCryptException.Reason.GetRemoteAppFolderError, e);
                }
            }
        }
    }

    /**
     * Returns whether this document encountered too many synchronization failures.
     *
     * <p>If the reset delay is passed, also resets the "too many failures" state.
     *
     * @return true if this document encountered too many synchronization failures
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public boolean hasTooManyFailures() throws DatabaseConnectionClosedException {
        if (getBackEntryNumSyncFailures() >= Constants.STORAGE.CLOUD_SYNC_MAX_FAILURES) {
            long lastFailureTime = getBackEntryLastFailureTime();
            if (lastFailureTime <= 0 || System.currentTimeMillis() - lastFailureTime >
                    1000 * Constants.STORAGE.CLOUD_SYNC_FAILURE_RESET_DELAY_S) {
                resetFailuresCount();
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the account where this document is stored encountered a "too many requests"
     * error which has not been reset yet.
     *
     * @return true if the account where this document is stored encountered a "too many requests"
     *         error which has not been reset yet
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public boolean hasTooManyRequests() throws DatabaseConnectionClosedException {
        return null != getBackStorageAccount() && getBackStorageAccount().hasTooManyRequests();
    }

    /**
     * Logs this document data.
     */
    public void log() {
        LOG.info("EncryptedDocument :\n  * id = {}\n  * logicalPath = {}\n  * parent id = {}\n  * size = {}\n  * remote id = {}\n  * remote version = {}",
                id, failSafeLogicalPath(), parentId, getSizeText(), backEntryId, backEntryVersion);
    }

    /**
     * Resursively lists the documents contained in this folder, or only this document if it is a file.
     *
     * @param parentBefore if true, the recursive iteration lists each folder before its content;
     *                     if false, each folder is listed after its content
     * @return the list of all the documents contained in this folder, or only this document if it
     *         is a file
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public List<EncryptedDocument> unfoldAsList(boolean parentBefore) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> encryptedDocuments = new ArrayList<>();
        if (parentBefore) {
            encryptedDocuments.add(this);
        }
        if (isFolder()) {
            for (EncryptedDocument child : children(false)) {
                encryptedDocuments.addAll(child.unfoldAsList(parentBefore));
            }
        }
        if (!parentBefore) {
            encryptedDocuments.add(this);
        }
        return encryptedDocuments;
    }

    /**
     * Recursively builds a list of the documents, by unfolding the given {@code encryptedDocuments}
     * list.
     *
     * @param encryptedDocuments the encrypted documents list to unfold
     * @param parentBefore       if true, the recursive iteration lists each folder before its content;
     *                           if false, each folder is listed after its content
     * @return the unfolded list
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public static List<EncryptedDocument> unfoldAsList(List<EncryptedDocument> encryptedDocuments, boolean parentBefore) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> result = new ArrayList<>();
        for (EncryptedDocument encryptedDocument : encryptedDocuments) {
            result.addAll(encryptedDocument.unfoldAsList(parentBefore));
        }
        return result;
    }
}
