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

package fr.petrus.tools.storagecrypt.android;

import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.exceptions.NetworkException;
import fr.petrus.lib.core.crypto.keystore.KeyStore;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.AlertDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ConfirmationDialogFragment;
import fr.petrus.tools.storagecrypt.android.platform.AndroidPlatformFactory;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;

/**
 * The Android {@link android.app.Application}, which manages the application state.
 *
 * @author Pierre Sagne
 * @since 04.10.2015
 */
public class Application extends android.app.Application implements DocumentsSelection {
    private static final String TAG = "Application";

    private static Application instance = null;

    /**
     * Returns the unique instance of the {@code Application}.
     *
     * @return the unique instance of the {@code Application}
     */
    public static Application getInstance(){
        return instance;
    }

    private AppContext appContext = null;

    private List<Uri> encryptQueue = new ArrayList<>();

    private long currentFolderId = Constants.STORAGE.ROOT_PARENT_ID;
    private EncryptedDocument currentFolder = null;
    private OrderBy orderBy = OrderBy.NameAsc;

    private List<EncryptedDocument> documentsRef = new ArrayList<>();
    private KeyStore exportedKeyStore = null;

    private boolean selectionMode = false;
    private HashSet<EncryptedDocument> selectedDocuments = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    /**
     * Returns the {@code AppContext} which provides dependencies.
     *
     * @return the {@code AppContext} which provides dependencies
     */
    public AppContext getAppContext() {
        if (null==appContext) {
            synchronized (this) {
                if (null==appContext) {
                    appContext = new AppContext(new AndroidPlatformFactory(this));
                }
            }
        }
        return appContext;
    }

    /**
     * Returns the list of {@code Uri}s this application was asked to open.
     *
     * @return the list of {@code Uri}s this application was asked to open
     */
    public List<Uri> getEncryptQueue() {
        return encryptQueue;
    }

    /**
     * Sets the {@code EncryptedDocument} with the given {@code currentFolderId} as the current folder.
     *
     * @param currentFolderId the database ID of the {@code EncryptedDocument} to set as the current                        folder.
     */
    public void setCurrentFolderId(long currentFolderId) {
        try {
            this.currentFolder = appContext.getEncryptedDocuments()
                    .encryptedDocumentWithId(currentFolderId);
            this.currentFolderId = currentFolderId;
            DocumentListChangeEvent.post();
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }
    }

    /**
     * Sets the given {@code currentFolder} as the current folder.
     *
     * @param currentFolder the {@code EncryptedDocument} to set as the current folder.
     */
    public void setCurrentFolder(EncryptedDocument currentFolder) {
        this.currentFolder = currentFolder;
        if (null==currentFolder) {
            currentFolderId = Constants.STORAGE.ROOT_PARENT_ID;
        } else {
            currentFolderId = currentFolder.getId();
        }
        DocumentListChangeEvent.post();
    }

    /**
     * Makes the parent of the current folder the new current folder, if the parent exists.
     *
     * @return the boolean
     */
    public boolean backToParentFolder() {
        if (isCurrentFolderRoot()) {
            // if we are already at the top level, do nothing, and return false.
            // if this action was done by pressing the back key, the app will exit.
            return false;
        }
        checkCurrentFolder();
        if (!isCurrentFolderRoot()) {
            setCurrentFolderId(currentFolder.getParentId());
        }
        return true;
    }

    /**
     * Checks that the current folder exists, else go back to the "top level" root.
     *
     * @return true if the current folder exists
     */
    public boolean checkCurrentFolder() {
        if (!isCurrentFolderRoot()) {
            try {
                EncryptedDocument encryptedDocument = appContext.getEncryptedDocuments()
                        .encryptedDocumentWithId(currentFolderId);
                if (null == encryptedDocument) {
                    setCurrentFolderId(Constants.STORAGE.ROOT_PARENT_ID);
                    DocumentListChangeEvent.post();
                    return false;
                }
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the current folder is the "top level" root.
     *
     * @return true if the current folder is the "top level" root
     */
    public boolean isCurrentFolderRoot() {
        return Constants.STORAGE.ROOT_PARENT_ID == currentFolderId;
    }

    /**
     * Returns the database ID of the current folder.
     *
     * @return the database ID of the current folder
     */
    public long getCurrentFolderId() {
        return currentFolderId;
    }

    /**
     * Returns the current folder.
     *
     * @return the current folder
     */
    public EncryptedDocument getCurrentFolder() {
        return currentFolder;
    }

    /**
     * Sets the {@code EncryptedDocument}s list sorting criterion.
     *
     * @param orderBy the {@code EncryptedDocument}s list sorting criterion
     */
    public void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        DocumentListChangeEvent.post();
    }

    /**
     * Returns the {@code EncryptedDocument}s list sorting criterion.
     *
     * @return the {@code EncryptedDocument}s list sorting criterion
     */
    public OrderBy getOrderBy() {
        return orderBy;
    }

    /**
     * Returns the list of {@code EncryptedDocument}s contained in the current folder.
     *
     * @return the list of {@code EncryptedDocument}s contained in the current folder
     */
    public List<EncryptedDocument> getCurrentFolderChildren() {
        if (appContext.getDatabase().isOpen()) {
            checkCurrentFolder();
            try {
                if (isCurrentFolderRoot()) {
                    return appContext.getEncryptedDocuments().roots();
                }
                return currentFolder.children(true, orderBy);
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }


    /**
     * Marks the given {@code encryptedDocument} for deletion, and launches the synchronization task.
     *
     * <p>If the given {@code encryptedDocument} is the local files "top level" folder, this method
     * displays an error message and does nothing.
     *
     * <p>If the given {@code encryptedDocument} is a remote documents root, calls
     * {@link Application#deleteRoot}.
     *
     * <p>If the given {@code encryptedDocument} is a folder, this method also marks the references
     * of the contained documents for deletion.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to delete
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void deleteDocument(EncryptedDocument encryptedDocument) throws DatabaseConnectionClosedException {
        if (null == encryptedDocument) {
            return;
        }

        if (encryptedDocument.isRoot()) {
            if (encryptedDocument.isUnsynchronizedRoot()) {
                new ShowDialogEvent(new AlertDialogFragment.Parameters()
                        .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                        .setMessage(getString(R.string.error_message_you_cannot_delete_the_local_storage_provider)))
                        .postSticky();
                return;
            }

            List<EncryptedDocument> children = encryptedDocument.children(false, orderBy);
            if (null!=children && !children.isEmpty()) {
                new ShowDialogEvent(new ConfirmationDialogFragment.Parameters()
                        .setDialogId(AndroidConstants.MAIN_ACTIVITY.NON_EMPTY_PROVIDER_SUPPRESSION_DIALOG)
                        .setTitle(getString(R.string.confirmation_request_dialog_title))
                        .setMessage(getString(R.string.confirmation_request_message_delete_non_empty_storage_provider))
                        .setPositiveChoiceText(getString(R.string.confirmation_request_yes_button_text))
                        .setNegativeChoiceText(getString(R.string.confirmation_request_no_button_text))
                        .setParameter(encryptedDocument.getId())).postSticky();
                return;
            }
            deleteRoot(encryptedDocument);
        } else {
            if (encryptedDocument.isFolder()) {
                List<EncryptedDocument> children = encryptedDocument.children(false, orderBy);
                if (null != children && !children.isEmpty()) {
                    new ShowDialogEvent(new ConfirmationDialogFragment.Parameters()
                            .setDialogId(AndroidConstants.MAIN_ACTIVITY.NON_EMPTY_FOLDER_SUPPRESSION_DIALOG)
                            .setTitle(getString(R.string.confirmation_request_dialog_title))
                            .setMessage(getString(R.string.confirmation_request_message_delete_non_empty_folder))
                            .setPositiveChoiceText(getString(R.string.confirmation_request_yes_button_text))
                            .setNegativeChoiceText(getString(R.string.confirmation_request_no_button_text))
                            .setParameter(encryptedDocument.getId())).postSticky();
                    return;
                }
            }

            encryptedDocument.delete();
            try {
                DocumentsSyncTask documentsSyncTask = appContext.getTask(DocumentsSyncTask.class);
                documentsSyncTask.restartCurrentSync(encryptedDocument);
                //try to delete the remote file
                documentsSyncTask.syncDocument(encryptedDocument);
            } catch (TaskCreationException e) {
                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
            }
            DocumentListChangeEvent.post();
        }
    }

    /**
     * Tries to delete all the given {@code encryptedDocuments} by calling {@link #deleteDocument}.
     *
     * @param encryptedDocuments the {@code EncryptedDocument}s to delete
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void deleteDocuments(List<EncryptedDocument> encryptedDocuments) throws DatabaseConnectionClosedException {
        for (EncryptedDocument encryptedDocument : encryptedDocuments) {
            deleteDocument(encryptedDocument);
        }
    }

    /**
     * Deletes a root {@code encryptedDocument} from the database, revokes access to the remote
     * storage account and deletes the references of the contained documents from the database.
     *
     * <p>If the given {@code encryptedDocument} is not a remote documents root, this method
     * does nothing.
     *
     * @param encryptedDocument the root to delete
     */
    public void deleteRoot(final EncryptedDocument encryptedDocument) {
        if (null!=encryptedDocument && encryptedDocument.isRoot() && !encryptedDocument.isUnsynchronizedRoot()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        appContext.getTask(DocumentsSyncTask.class).stop();
                    } catch (TaskCreationException e) {
                        Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                    }
                    try {
                        encryptedDocument.deleteRoot();
                        File localRootFolder = encryptedDocument.file();
                        appContext.getFileSystem().deleteFolder(localRootFolder);
                        appContext.getEncryptedDocuments().updateRoots();
                        getContentResolver().notifyChange(DocumentsContract.buildRootsUri(
                                AndroidConstants.CONTENT_PROVIDER.AUTHORITY), null);
                        DocumentListChangeEvent.post();
                    } catch (DatabaseConnectionClosedException e) {
                        Log.e(TAG, "Database is closed", e);
                    } catch (NetworkException | StorageCryptException e) {
                        Log.d(TAG, "Error when deleting account", e);
                        new ShowDialogEvent(new AlertDialogFragment.Parameters()
                                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                .setMessage(appContext.getTextI18n().getExceptionDescription(e))
                        ).postSticky();
                    }
                    try {
                        appContext.getTask(DocumentsSyncTask.class).start();
                    } catch (TaskCreationException e) {
                        Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                    }
                }
            }.start();
        }
    }

    public void deleteFolder(final EncryptedDocument folder) {
        if (null!=folder && !folder.isRoot() && folder.isFolder()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        for (EncryptedDocument encryptedDocument : folder.unfoldAsList(false)) {
                            encryptedDocument.delete();
                            try {
                                DocumentsSyncTask documentsSyncTask =
                                        appContext.getTask(DocumentsSyncTask.class);
                                if (encryptedDocument.getSyncState(SyncAction.Upload) == fr.petrus.lib.core.State.Running) {
                                    documentsSyncTask.restartCurrentSync(encryptedDocument);
                                }
                                //try to delete the remote file
                                documentsSyncTask.syncDocument(encryptedDocument);
                            } catch (TaskCreationException e) {
                                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                            }
                            DocumentListChangeEvent.post();
                        }
                    } catch (DatabaseConnectionClosedException e) {
                        Log.e(TAG, "Database is closed", e);
                    }
                }
            }.start();
        }
    }

    /**
     * Sets the given {@code document} for further use.
     *
     * @param document the encrypted document to reference
     */
    public void setDocumentReference(EncryptedDocument document) {
        documentsRef.clear();
        documentsRef.add(document);
    }

    /**
     * Sets the given {@code documents} for further use.
     *
     * @param documents the encrypted documents to reference
     */
    public void setDocumentsReferences(List<EncryptedDocument> documents) {
        documentsRef.clear();
        documentsRef.addAll(documents);
    }

    /**
     * Clears the documents references.
     */
    public void clearDocumentsReferences() {
        documentsRef.clear();
    }

    /**
     * Returns the documents referenced by {@link #setDocumentReference} or
     * {@link #setDocumentsReferences}.
     *
     * @return the documents referenced by {@link #setDocumentReference} or
     *         {@link #setDocumentsReferences}
     */
    public List<EncryptedDocument> getDocumentsReferences() {
        return documentsRef;
    }

    /**
     * Sets the key store where to save the exported keys.
     *
     * @param exportedKeyStore the key store where to save the exported keys
     */
    public void setExportedKeyStore(KeyStore exportedKeyStore) {
        this.exportedKeyStore = exportedKeyStore;
    }

    /**
     * Returns the key store where to save the exported keys.
     *
     * @return the key store where to save the exported keys
     */
    public KeyStore getExportedKeyStore() {
        return exportedKeyStore;
    }

    @Override
    public void setSelectionMode(boolean selectionMode) {
        if (this.selectionMode != selectionMode) {
            this.selectionMode = selectionMode;
            DocumentListChangeEvent.postSticky();
        }
    }

    @Override
    public boolean isInSelectionMode() {
        return selectionMode;
    }

    @Override
    public void clearSelectedDocuments() {
        selectedDocuments.clear();
    }

    @Override
    public void setDocumentSelected(EncryptedDocument encryptedDocument, boolean selected) {
        if (selected) {
            selectedDocuments.add(encryptedDocument);
        } else {
            selectedDocuments.remove(encryptedDocument);
        }
    }

    @Override
    public boolean isDocumentSelected(EncryptedDocument encryptedDocument) {
        return selectedDocuments.contains(encryptedDocument);
    }

    @Override
    public List<EncryptedDocument> getSelectedDocuments() {
        List<EncryptedDocument> selectedDocumentsList = new ArrayList<>();
        selectedDocumentsList.addAll(selectedDocuments);
        return selectedDocumentsList;
    }
}
