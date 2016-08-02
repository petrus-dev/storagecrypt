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

package fr.petrus.tools.storagecrypt.desktop.windows;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.markdownj.MarkdownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.crypto.KeyStoreUber;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionException;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.tools.storagecrypt.desktop.ProgressWindowCreationException;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.lib.core.processes.ChangesSyncProcess;
import fr.petrus.lib.core.processes.DocumentsDecryptionProcess;
import fr.petrus.lib.core.processes.DocumentsEncryptionProcess;
import fr.petrus.lib.core.processes.DocumentsImportProcess;
import fr.petrus.lib.core.processes.DocumentsUpdatesPushProcess;
import fr.petrus.lib.core.result.OnCompletedListener;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.tools.storagecrypt.desktop.CachedResources;
import fr.petrus.tools.storagecrypt.desktop.Settings;
import fr.petrus.tools.storagecrypt.desktop.platform.DesktopPlatformFactory;
import fr.petrus.tools.storagecrypt.desktop.tasks.ChangesSyncTask;
import fr.petrus.tools.storagecrypt.desktop.tasks.DocumentsDecryptionTask;
import fr.petrus.tools.storagecrypt.desktop.tasks.DocumentsEncryptionTask;
import fr.petrus.tools.storagecrypt.desktop.tasks.DocumentsImportTask;
import fr.petrus.tools.storagecrypt.desktop.tasks.DocumentsUpdatesPushTask;
import fr.petrus.tools.storagecrypt.desktop.tasks.FileDecryptionTask;
import fr.petrus.tools.storagecrypt.desktop.tasks.DocumentsSyncTask;
import fr.petrus.tools.storagecrypt.desktop.platform.DesktopNetwork;
import fr.petrus.tools.storagecrypt.desktop.Resources;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.DocumentAction;
import fr.petrus.tools.storagecrypt.desktop.windows.components.DocumentContextMenuAction;
import fr.petrus.tools.storagecrypt.desktop.windows.components.DocumentsTable;
import fr.petrus.tools.storagecrypt.desktop.windows.components.FolderPathNavigationComposite;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.AuthBrowserDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.CreateFolderDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.CreateRootDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.DocumentChooserDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.DocumentDetailsDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.EncryptDocumentsDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.KeyStoreChangePasswordDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.KeyStoreCreateDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.KeyStoreNoKeyDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.KeyStoreUnlockDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.SelectRootKeyDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.SettingsDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.SelectAndRenameKeysDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.dialog.TextInputDialog;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.ChangesSyncProgressWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.DocumentsSyncProgressWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.progress.ProgressWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The type Application window.
 * <p/>
 * <p>Most of the work is done here.
 *
 * @author Pierre Sagne
 * @since 28.07.2015
 */
public class AppWindow extends ApplicationWindow implements
        DocumentsTable.DocumentsTableListener {

    private static Logger LOG = LoggerFactory.getLogger(AppWindow.class);

    private enum UnlockKeystoreResult {

        /**
         * Successful keystore unlock.
         */
        Success,

        /**
         * Bad password.
         */
        BadPassword,

        /**
         * Keystore unlocked canceled.
         */
        Exit
    }

    private TextBundle textBundle = new TextBundle("res.text.strings");
    private Resources resources = new Resources();

    private HashMap<Class<? extends ProgressWindow>, ProgressWindow> progressWindows = new HashMap<>();

    private AppContext appContext = null;
    private Database database = null;
    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private FileSystem fileSystem = null;
    private CloudAppKeys cloudAppKeys = null;
    private TextI18n textI18n = null;
    private Accounts accounts = null;
    private EncryptedDocuments encryptedDocuments = null;
    private Settings settings = null;

    private Image syncBlackImage = null;
    private Image syncGreenImage = null;
    private Image syncRedImage = null;
    private Image syncVioletImage = null;
    private Image downloadBlackImage = null;
    private Image downloadGreenImage = null;
    private Image downloadRedImage = null;
    private Image downloadVioletImage = null;
    private Image uploadBlackImage = null;
    private Image uploadGreenImage = null;
    private Image uploadRedImage = null;
    private Image uploadVioletImage = null;
    private Image deletionBlackImage = null;
    private Image deletionGreenImage = null;
    private Image deletionRedImage = null;
    private Image deletionVioletImage = null;

    private Image createCloudImage = null;
    private Image createFolderImage = null;
    private Image encryptImage = null;

    private boolean confirmExit = true;

    private Composite windowContent = null;

    private FolderPathNavigationComposite folderPathNavigationComposite = null;

    private Composite syncProcessGroup = null;
    private Button documentsSyncButton = null;
    private Button changesSyncButton = null;

    private MenuManager currentFolderContextMenuManager = null;

    private ChangesSyncProgressWindow changesSyncProgressWindow = null;
    private ChangesSyncTask.SyncServiceState lastChangesSyncState = null;

    private DocumentsSyncProgressWindow documentsSyncProgressWindow = null;
    private DocumentsSyncTask.SyncServiceState lastDocumentsSyncState = null;

    private DocumentsTable documentsTable = null;

    private Action toolBarAddCloudAction = null;
    private Action toolBarCreateFolderAction = null;
    private Action toolBarEncryptAction = null;

    private long currentFolderId = Constants.STORAGE.ROOT_PARENT_ID;
    private EncryptedDocument currentFolder = null;
    private boolean folderChanged = false;

    /**
     * Creates a new {@code AppWindow} instance.
     */
    public AppWindow() {
        super(null);

        appContext = new AppContext(new DesktopPlatformFactory(this));
        database = appContext.getDatabase();
        crypto = appContext.getCrypto();
        keyManager = appContext.getKeyManager();
        fileSystem = appContext.getFileSystem();
        cloudAppKeys = appContext.getCloudAppKeys();
        textI18n = appContext.getTextI18n();
        accounts = appContext.getAccounts();
        encryptedDocuments = appContext.getEncryptedDocuments();

        fileSystem.createAppDir();

        settings = new Settings(fileSystem);

        DesktopNetwork.setupProxy(settings);

        loadImages();

        addMenuBar();
        addToolBar(SWT.BORDER);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(textBundle.getString("app_name"));
    }

    @Override
    protected Control createContents(Composite parent) {
        windowContent = new Composite(parent, SWT.NONE);
        applyGridLayout(windowContent).numColumns(2).horizontalSpacing(4);

        currentFolderContextMenuManager = new MenuManager();
        currentFolderContextMenuManager.setRemoveAllWhenShown(true);
        currentFolderContextMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                if (null!=currentFolder) {
                    currentFolderContextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_details"),
                            DocumentAction.Details,
                            currentFolder, AppWindow.this));

                    if (currentFolder.isUnsynchronizedRoot()) {
                        currentFolderContextMenuManager.add(new DocumentContextMenuAction(
                                textBundle.getString("document_context_menu_select_default_key"),
                                DocumentAction.SelectDefaultKey,
                                currentFolder, AppWindow.this));

                        currentFolderContextMenuManager.add(new DocumentContextMenuAction(
                                textBundle.getString("document_context_menu_import_existing"),
                                DocumentAction.Import,
                                currentFolder, AppWindow.this));
                    } else if (currentFolder.isRoot()) {
                        currentFolderContextMenuManager.add(new DocumentContextMenuAction(
                                textBundle.getString("document_context_menu_select_default_key"),
                                DocumentAction.SelectDefaultKey,
                                currentFolder, AppWindow.this));

                        currentFolderContextMenuManager.add(new DocumentContextMenuAction(
                                textBundle.getString("document_context_menu_push_updates"),
                                DocumentAction.PushUpdates,
                                currentFolder, AppWindow.this));

                        currentFolderContextMenuManager.add(new DocumentContextMenuAction(
                                textBundle.getString("document_context_menu_sync_remote_changes"),
                                DocumentAction.ChangesSync,
                                currentFolder, AppWindow.this));
                    } else if (currentFolder.isFolder()) {
                        currentFolderContextMenuManager.add(new DocumentContextMenuAction(
                                textBundle.getString("document_context_menu_decrypt"),
                                DocumentAction.Decrypt,
                                currentFolder, AppWindow.this));
                    }
                }
            }
        });

        folderPathNavigationComposite = new FolderPathNavigationComposite(windowContent, this,
                currentFolderContextMenuManager, textBundle, resources);
        applyGridData(folderPathNavigationComposite).withHorizontalFill();
        folderPathNavigationComposite.updateLocked();

        syncProcessGroup = new Composite(windowContent, SWT.NONE);
        applyGridLayout(syncProcessGroup).numColumns(2).horizontalSpacing(4);
        applyGridData(syncProcessGroup).horizontalAlignment(SWT.END);

        if (cloudAppKeys.found()) {
            changesSyncButton = new Button(syncProcessGroup, SWT.PUSH);
            applyGridData(changesSyncButton).horizontalAlignment(SWT.FILL);
            changesSyncButton.setImage(syncBlackImage);

            changesSyncButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent selectionEvent) {
                    if (null == changesSyncProgressWindow) {
                        try {
                            appContext.getTask(ChangesSyncTask.class).syncAll(true);
                            changesSyncProgressWindow =
                                    getProgressWindow(ChangesSyncProgressWindow.class);
                            if (null != lastChangesSyncState) {
                                changesSyncProgressWindow.update(lastChangesSyncState);
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Failed to decrypt file", e);
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}",
                                    e.getTaskClass().getCanonicalName(), e);
                        } catch (ProgressWindowCreationException e) {
                            LOG.error("Failed to get progress window {}",
                                    e.getProgressWindowClass().getCanonicalName(), e);
                        }
                    } else {
                        if (!changesSyncProgressWindow.isClosed()) {
                            changesSyncProgressWindow.close();
                        }
                        changesSyncProgressWindow = null;
                    }
                }
            });

            documentsSyncButton = new Button(syncProcessGroup, SWT.PUSH);
            applyGridData(documentsSyncButton).horizontalAlignment(SWT.FILL);
            documentsSyncButton.setText(String.format(Locale.getDefault(), "%d/%d", 0, 0));
            documentsSyncButton.setImage(downloadBlackImage);
            documentsSyncButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent selectionEvent) {
                    if (null == documentsSyncProgressWindow) {
                        try {
                            appContext.getTask(DocumentsSyncTask.class).start();
                            documentsSyncProgressWindow =
                                    getProgressWindow(DocumentsSyncProgressWindow.class);
                            if (null != lastDocumentsSyncState) {
                                documentsSyncProgressWindow.update(lastDocumentsSyncState);
                            }
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}",
                                    e.getTaskClass().getCanonicalName(), e);
                        } catch (ProgressWindowCreationException e) {
                            LOG.error("Failed to get progress window {}",
                                    e.getProgressWindowClass().getCanonicalName(), e);
                        }
                    } else {
                        if (!documentsSyncProgressWindow.isClosed()) {
                            documentsSyncProgressWindow.close();
                        }
                        documentsSyncProgressWindow = null;
                    }
                }
            });
        }

        documentsTable = new DocumentsTable(windowContent, textBundle, resources, this);
        applyGridData(documentsTable.getTable()).horizontalSpan(2).withFill();

        windowContent.layout();

        if (!crypto.isAes256Supported()) {
            showErrorMessage(textBundle.getString("error_message_AES256_not_supported"));
            exit(false);
        }

        update();

        try {
            switch (unlockKeystore()) {
                case Success:
                    if (accounts.size() > 0L) {
                        try {
                            appContext.getTask(ChangesSyncTask.class).syncAll(true);
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}",
                                    e.getTaskClass().getCanonicalName(), e);
                        }
                        // try to sync files
                        try {
                            appContext.getTask(DocumentsSyncTask.class).start();
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}",
                                    e.getTaskClass().getCanonicalName(), e);
                        }
                    }
                    update();
                    break;
                case Exit:
                    exit(false);
                    break;
            }
        } catch (DatabaseConnectionClosedException e) {
            LOG.error("Failed to unlock the database", e);
            showErrorMessage(textBundle.getString("error_message_unable_to_unlock_the_database"));
            exit(false);
        } catch (StorageCryptException e) {
            LOG.error("Failed to unlock the database", e);
            showErrorMessage(textBundle.getString("error_message_unable_to_unlock_the_database"));
            exit(false);
        }

        return windowContent;
    }

    /**
     * Executes the given {@code runnable} on the main thread.
     *
     * @param runnable the runnable to execute on the main thread
     */
    public void asyncExec(Runnable runnable) {
        Display display = getDisplay();
        if (null!=display && !display.isDisposed()) {
            display.asyncExec(runnable);
        }
    }

    /**
     * Executes the given {@code runnable} on the main thread if the given {@code control} is not
     * disposed.
     *
     * @param control  the control to check before executing the given {@code runnable}
     * @param runnable the runnable to execute on the main thread
     */
    public void asyncExec(Control control, Runnable runnable) {
        if (null!=control) {
            Display display = control.getDisplay();
            if (!control.isDisposed()) {
                if (null != display && !display.isDisposed()) {
                    display.asyncExec(runnable);
                }
            }
        }
    }

    /**
     * Exits the application.
     *
     * @param confirmExit if true, the user will have to confirm the application exit
     */
    public void exit(boolean confirmExit) {
        this.confirmExit = confirmExit;
        asyncExec(new Runnable() {
            @Override
            public void run() {
                close();
            }
        });
    }

    @Override
    public boolean close() {
        if (!confirmExit || askForConfirmation(textBundle.getString("confirmation_request_message_exit"))) {
            cancelSyncServices();
            database.close();
            return super.close();
        } else {
            update();
            return false;
        }
    }

    private void cancelSyncServices() {
        try {
            appContext.getTask(ChangesSyncTask.class).cancel();
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
        try {
            appContext.getTask(DocumentsSyncTask.class).cancel();
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
    }

    /**
     * Returns the {@code AppContext} which provides dependencies for many classes.
     *
     * @return the {@code AppContext} which provides dependencies for many classes
     */
    public AppContext getAppContext() {
        return appContext;
    }

    /**
     * Returns the {@code TextBundle} instance of this application window.
     *
     * @return the {@code TextBundle} instance of this application window
     */
    public TextBundle getTextBundle() {
        return textBundle;
    }

    /**
     * Returns the {@code Resources} instance of this application window.
     *
     * @return the {@code Resources} instance of this application window
     */
    public Resources getResources() {
        return resources;
    }

    /**
     * Returns the {@code Settings} instance of this application window.
     *
     * @return the {@code Settings} instance of this application window
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Returns a {@code ProgressWindow} instance of the given {@code progressWindowClass}.
     *
     * @param <P>                 the progress window type
     * @param progressWindowClass the progress window class
     * @return a {@code ProgressWindow} instance of the given {@code progressWindowClass}
     * @throws ProgressWindowCreationException if an error occurs when creating the                                         {@code ProgressWindow}
     */
    @SuppressWarnings("unchecked")
    public <P extends ProgressWindow> P getProgressWindow(Class<P> progressWindowClass)
            throws ProgressWindowCreationException {
        ProgressWindow progressWindow = progressWindows.get(progressWindowClass);
        if (null==progressWindow) {
            try {
                progressWindow = progressWindowClass.getConstructor(AppWindow.class)
                        .newInstance(this);
                progressWindows.put(progressWindowClass, progressWindow);
            } catch (InstantiationException e) {
                throw new ProgressWindowCreationException("Failed to create progress window",
                        progressWindowClass, e);
            } catch (IllegalAccessException e) {
                throw new ProgressWindowCreationException("Failed to create progress window",
                        progressWindowClass, e);
            } catch (InvocationTargetException e) {
                throw new ProgressWindowCreationException("Failed to create progress window",
                        progressWindowClass, e);
            } catch (NoSuchMethodException e) {
                throw new ProgressWindowCreationException("Failed to create progress window",
                        progressWindowClass, e);
            }
        }
        progressWindow.open();
        return (P)progressWindow;
    }

    /**
     * Runs the application and opens the window.
     */
    public void run() {
        // Don't return from open() until window closes
        setBlockOnOpen(true);

        // Open the main window
        open();

        // Dispose the display
        Display.getCurrent().dispose();
    }

    /**
     * Returns whether this window is closed.
     *
     * @return true if this window has been closed
     */
    public boolean isClosed() {
        return null==getShell() || getShell().isDisposed();
    }

    /**
     * Returns the display.
     *
     * @return the display
     */
    public Display getDisplay() {
        Shell shell = getShell();
        if (null!=shell) {
            return shell.getDisplay();
        }
        return null;
    }

    /**
     * Tries to unlock the database.
     *
     * @return true if the database was successfully unlocked
     * @throws StorageCryptException if an error occurs when trying to unlock the database
     */
    public boolean unlockDatabase() throws StorageCryptException {
        if (!keyManager.isKeyStoreUnlocked()) {
            return false;
        }

        String encryptedDatabaseEncryptionPassword = settings.getDatabaseEncryptionPassword();
        if (null == encryptedDatabaseEncryptionPassword) {
            try {
                String databaseEncryptionPassword = crypto.generateRandomPassword(32);
                encryptedDatabaseEncryptionPassword =
                        keyManager.encryptWithDatabaseSecurityKey(databaseEncryptionPassword);
                settings.setDatabaseEncryptionPassword(encryptedDatabaseEncryptionPassword);
                settings.save();
            } catch (NoSuchAlgorithmException e) {
                throw new StorageCryptException("Failed to generate a new database encryption password",
                        StorageCryptException.Reason.DatabaseUnlockError, e);
            } catch (CryptoException e) {
                throw new StorageCryptException("Failed to generate a new database encryption password",
                        StorageCryptException.Reason.DatabaseUnlockError, e);
            }
        }

        if (null == encryptedDatabaseEncryptionPassword) {
            return false;
        }

        try {
            String databaseEncryptionPassword = keyManager.decryptWithDatabaseSecurityKey(encryptedDatabaseEncryptionPassword);
            if (!database.isOpen()) {
                database.open(databaseEncryptionPassword);
            }
            return true;
        } catch (DatabaseConnectionException e) {
            throw new StorageCryptException("Failed to unlock the database",
                    StorageCryptException.Reason.DatabaseUnlockError, e);
        } catch (CryptoException e) {
            throw new StorageCryptException("Failed to unlock the database",
                    StorageCryptException.Reason.DatabaseUnlockError, e);
        }
    }

    /**
     * Sets the current folder to the one with the current {@code id}.
     *
     * @param id the id of the current folder to set
     */
    public void setCurrentFolderId(long id) {
        this.currentFolderId = id;
        folderChanged = true;
        update();
    }

    private void loadImages() {
        createCloudImage = resources.loadImage("/res/drawable/ic_cloud_add.png");
        createFolderImage = resources.loadImage("/res/drawable/ic_folder_add.png");
        encryptImage = resources.loadImage("/res/drawable/ic_encrypt_file.png");
        syncBlackImage = resources.loadImage("/res/drawable/ic_sync_black.png");
        syncGreenImage = resources.loadImage("/res/drawable/ic_sync_green.png");
        syncRedImage = resources.loadImage("/res/drawable/ic_sync_red.png");
        syncVioletImage = resources.loadImage("/res/drawable/ic_sync_violet.png");
        downloadBlackImage = resources.loadImage("/res/drawable/ic_download_black.png");
        downloadGreenImage = resources.loadImage("/res/drawable/ic_download_green.png");
        downloadRedImage = resources.loadImage("/res/drawable/ic_download_red.png");
        downloadVioletImage = resources.loadImage("/res/drawable/ic_download_violet.png");
        uploadBlackImage = resources.loadImage("/res/drawable/ic_upload_black.png");
        uploadGreenImage = resources.loadImage("/res/drawable/ic_upload_green.png");
        uploadRedImage = resources.loadImage("/res/drawable/ic_upload_red.png");
        uploadVioletImage = resources.loadImage("/res/drawable/ic_upload_violet.png");
        deletionBlackImage = resources.loadImage("/res/drawable/ic_delete_black.png");
        deletionGreenImage = resources.loadImage("/res/drawable/ic_delete_green.png");
        deletionRedImage = resources.loadImage("/res/drawable/ic_delete_red.png");
        deletionVioletImage = resources.loadImage("/res/drawable/ic_delete_violet.png");
    }

    /**
     * Updates the {@code ChangesSyncProgressWindow} with the given {@code syncState}.
     *
     * @param syncState the synchronization state to update the {@code ChangesSyncProgressWindow} with
     */
    public void updateChangesSyncProgress(final ChangesSyncTask.SyncServiceState syncState) {
        lastChangesSyncState = syncState;
        asyncExec(new Runnable() {
            @Override
            public void run() {
                changesSyncButton.setImage(syncGreenImage);
                if (null!= changesSyncProgressWindow) {
                    changesSyncProgressWindow.update(syncState);
                }
            }
        });
    }

    /**
     * Resets the {@code ChangesSyncProgressWindow} state.
     */
    public void resetChangesSyncProgress() {
        lastChangesSyncState = null;
        asyncExec(new Runnable() {
            @Override
            public void run() {
                changesSyncButton.setImage(syncBlackImage);
                if (null!= changesSyncProgressWindow) {
                    if (!changesSyncProgressWindow.isClosed()) {
                        changesSyncProgressWindow.close();
                    }
                    changesSyncProgressWindow = null;
                }
            }
        });
    }

    /**
     * Updates the {@code DocumentsSyncProgressWindow} with the given {@code syncState}.
     *
     * @param syncState the synchronization state to update the {@code DocumentsSyncProgressWindow} with
     */
    public void updateDocumentsSyncProgress(final DocumentsSyncTask.SyncServiceState syncState) {
        lastDocumentsSyncState = syncState;
        asyncExec(new Runnable() {
            @Override
            public void run() {
                int progress = syncState.documentsListProgress.getProgress();
                int max = syncState.documentsListProgress.getMax();
                SyncAction currentSyncAction = syncState.currentSyncAction;
                if (progress < max) {
                    documentsSyncButton.setText(String.format(Locale.getDefault(), "%d/%d",
                            progress, max));
                    if (null==currentSyncAction) {
                        documentsSyncButton.setImage(downloadBlackImage);
                    } else {
                        switch (currentSyncAction) {
                            case Download:
                                documentsSyncButton.setImage(downloadGreenImage);
                                break;
                            case Upload:
                                documentsSyncButton.setImage(uploadGreenImage);
                                break;
                            case Deletion:
                                documentsSyncButton.setImage(deletionGreenImage);
                                break;
                        }
                        documentsSyncButton.setVisible(true);
                    }
                    if (null!= documentsSyncProgressWindow) {
                        documentsSyncProgressWindow.update(syncState);
                    }
                } else {
                    documentsSyncButton.setText(String.format(Locale.getDefault(), "%d/%d", 0, 0));
                }
                syncProcessGroup.pack();
                windowContent.layout();
            }
        });
    }

    /**
     * Resets the {@code DocumentsSyncProgressWindow} state.
     */
    public void resetDocumentsSyncProgress() {
        lastDocumentsSyncState = null;
        asyncExec(new Runnable() {
            @Override
            public void run() {
                documentsSyncButton.setText(String.format(Locale.getDefault(),
                        "%d/%d", 0, 0));
                documentsSyncButton.setImage(downloadBlackImage);
                if (null!= documentsSyncProgressWindow) {
                    if (!documentsSyncProgressWindow.isClosed()) {
                        documentsSyncProgressWindow.close();
                    }
                    documentsSyncProgressWindow = null;
                }
                syncProcessGroup.pack();
                windowContent.layout();
            }
        });
    }

    private UnlockKeystoreResult unlockKeystore()
            throws DatabaseConnectionClosedException, StorageCryptException {
        if (keyManager.isKeyStoreExisting()) {
            KeyStoreUnlockDialog keyStoreLockedDialog = new KeyStoreUnlockDialog(this);
            keyStoreLockedDialog.open();
            if (!keyStoreLockedDialog.isResultPositive()) {
                return UnlockKeystoreResult.Exit;
            }
            String password = keyStoreLockedDialog.getPassword();
            if (null == password) {
                return UnlockKeystoreResult.BadPassword;
            }
            if (!keyManager.unlockKeyStore(password)) {
                return UnlockKeystoreResult.BadPassword;
            }
        } else {
            KeyStoreCreateDialog keyStoreCreateDialog = new KeyStoreCreateDialog(this);
            keyStoreCreateDialog.open();
            if (!keyStoreCreateDialog.isResultPositive()) {
                return UnlockKeystoreResult.Exit;
            }
            String password = keyStoreCreateDialog.getPassword();
            if (null == password) {
                return UnlockKeystoreResult.BadPassword;
            }
            if (!keyManager.createKeyStore(password)) {
                return UnlockKeystoreResult.BadPassword;
            }
        }
        if (keyManager.getKeyAliases().isEmpty()) {
            KeyStoreNoKeyDialog keyStoreNoKeyDialog = new KeyStoreNoKeyDialog(this);
            keyStoreNoKeyDialog.open();
            if (!keyStoreNoKeyDialog.isResultPositive()) {
                return UnlockKeystoreResult.Exit;
            }
        }
        if (unlockDatabase()) {
            encryptedDocuments.updateRoots();
        }
        return UnlockKeystoreResult.Success;
    }

    @Override
    public void update() {
        if (!keyManager.isKeyStoreUnlocked() || keyManager.getKeyAliases().isEmpty()) {
            toolBarAddCloudAction.setEnabled(false);
            toolBarCreateFolderAction.setEnabled(false);
            toolBarEncryptAction.setEnabled(false);
            folderPathNavigationComposite.updateLocked();
            documentsTable.updateLocked();
            windowContent.layout();
        } else {
            try {
                checkCurrentFolder();
                if (isCurrentFolderRoot()) {
                    toolBarAddCloudAction.setEnabled(true);
                    toolBarCreateFolderAction.setEnabled(false);
                    toolBarEncryptAction.setEnabled(false);
                    currentFolder = null;
                } else {
                    toolBarAddCloudAction.setEnabled(false);
                    toolBarCreateFolderAction.setEnabled(true);
                    toolBarEncryptAction.setEnabled(true);
                    currentFolder = encryptedDocuments.encryptedDocumentWithId(currentFolderId);
                }
                folderPathNavigationComposite.update(currentFolder);
                documentsTable.update(folderChanged);
                folderChanged = false;
                windowContent.layout();
            } catch (DatabaseConnectionClosedException e) {
                LOG.error("Database is locked", e);
            }
        }
    }

    /**
     * Updates the contents of this application window.
     *
     * @param async if true, the update will be performed asynchronously in the main thread
     */
    public void update(boolean async) {
        if (async) {
            asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
        } else {
            update();
        }
    }

    @Override
    public EncryptedDocument getCurrentFolder() {
        return currentFolder;
    }

    @Override
    public List<EncryptedDocument> getCurrentFolderChildren(OrderBy orderBy) {
        try {
            if (isCurrentFolderRoot()) {
                return encryptedDocuments.roots();
            } else {
                return currentFolder.children(true, orderBy);
            }
        } catch (DatabaseConnectionClosedException e) {
            LOG.error("Database is locked", e);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean isCurrentFolderRoot() {
        return Constants.STORAGE.ROOT_PARENT_ID == currentFolderId;
    }

    /**
     * Checks the current folder and return to the "top level" folder if it does not exist.
     *
     * @return true if the current folder still exists
     */
    public boolean checkCurrentFolder() {
        if (!isCurrentFolderRoot()) {
            EncryptedDocument encryptedDocument = null;
            try {
                encryptedDocument = encryptedDocuments.encryptedDocumentWithId(currentFolderId);
            } catch (DatabaseConnectionClosedException e) {
                LOG.error("Database is locked", e);
            }
            if (null == encryptedDocument) {
                setCurrentFolderId(Constants.STORAGE.ROOT_PARENT_ID);
                return false;
            }
        }
        return true;
    }

    @Override
    public void executeContextMenuAction(DocumentAction documentAction,
                                         EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        encryptedDocument.refresh();
        try {
            switch (documentAction) {
                case Details:
                    DocumentDetailsDialog documentDetailsDialog =
                            new DocumentDetailsDialog(this, encryptedDocument);
                    documentDetailsDialog.open();
                    break;
                case Open:
                    openDocument(encryptedDocument);
                    break;
                case Delete:
                    if (deleteDocument(encryptedDocument)) {
                        update();
                    }
                    break;
                case SelectDefaultKey:
                    if (encryptedDocument.isRoot()) {
                        selectRootDefaultKey(encryptedDocument);
                    }
                    break;
                case Decrypt:
                    if (!encryptedDocument.isRoot()) {
                        if (encryptedDocument.isFolder()) {
                            DocumentChooserDialog documentChooserDialog = new DocumentChooserDialog(this);
                            String folder = documentChooserDialog.chooseFolder();
                            if (null != folder && !folder.isEmpty()) {
                                LOG.debug("folder name = {}", folder);
                                appContext.getTask(DocumentsDecryptionTask.class)
                                        .decrypt(encryptedDocument, folder);
                            }
                        } else {
                            DocumentChooserDialog documentChooserDialog = new DocumentChooserDialog(this);
                            String file = documentChooserDialog.saveFile(
                                    textBundle.getString("document_chooser_save_decrypted_file_title"),
                                    encryptedDocument.getDisplayName());
                            if (null != file && !file.isEmpty()) {
                                appContext.getTask(FileDecryptionTask.class)
                                        .decrypt(encryptedDocument, file);
                            }
                        }
                    }
                    break;
                case Import:
                    if (encryptedDocument.isRoot()) {
                        appContext.getTask(DocumentsImportTask.class)
                                .importDocuments(encryptedDocument);
                    }
                    break;
                case PushUpdates:
                    if (encryptedDocument.isRoot()) {
                        appContext.getTask(DocumentsUpdatesPushTask.class)
                                .pushUpdates(encryptedDocument);
                    }
                    break;
                case ChangesSync:
                    if (encryptedDocument.isRoot() && !encryptedDocument.isUnsynchronizedRoot()) {
                        appContext.getTask(ChangesSyncTask.class)
                                .sync(encryptedDocument.getBackStorageAccount(), true);
                    }
                    break;
            }
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void openDocument(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        encryptedDocument.refresh();
        if (encryptedDocument.isRoot() || encryptedDocument.isFolder()) {
            setCurrentFolderId(encryptedDocument.getId());
        } else {
            openFile(encryptedDocument);
        }
    }

    private void selectRootDefaultKey(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        encryptedDocument.refresh();
        if (encryptedDocument.isRoot()) {
            SelectRootKeyDialog selectRootKeyDialog = new SelectRootKeyDialog(this, encryptedDocument);
            selectRootKeyDialog.open();
            if (selectRootKeyDialog.isResultPositive()) {
                encryptedDocument.updateKeyAlias(selectRootKeyDialog.getKeyAlias());
                update();
            }
        }
    }

    /**
     * The method called when documents decryption is done.
     *
     * @param results the results of the documents decryption task
     */
    public void onDocumentsDecryptionDone(final DocumentsDecryptionProcess.Results results) {
        asyncExec(new Runnable() {
            @Override
            public void run() {
                new ResultsWindow(AppWindow.this, results)
                        .setTitle(textBundle.getString("results_dialog_decryption_results_title"))
                        .setHeaderText(textBundle.getString("results_dialog_decryption_results_header"))
                        .open();
            }
        });
    }

    /**
     * The method called when documents encryption is done.
     *
     * @param results the results of the documents encryption task
     */
    public void onDocumentsEncryptionDone(final DocumentsEncryptionProcess.Results results) {
        try {
            appContext.getTask(DocumentsSyncTask.class)
                    .syncDocuments(results.getSuccessfulyEncryptedDocuments());
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
        asyncExec(new Runnable() {
            @Override
            public void run() {
                update();
                new ResultsWindow(AppWindow.this, results)
                        .setTitle(textBundle.getString("results_dialog_encryption_results_title"))
                        .setHeaderText(textBundle.getString("results_dialog_encryption_results_header"))
                        .open();
            }
        });
    }

    /**
     * The method called when documents import is done.
     *
     * @param results the results of the documents import task
     */
    public void onDocumentsImportDone(final DocumentsImportProcess.Results results) {
        try {
            appContext.getTask(DocumentsSyncTask.class)
                    .syncDocuments(results.getSuccessfulyImportedDocuments());
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
        asyncExec(new Runnable() {
            @Override
            public void run() {
                update();
                new ResultsWindow(AppWindow.this, results)
                        .setTitle(textBundle.getString("results_dialog_import_results_title"))
                        .setHeaderText(textBundle.getString("results_dialog_import_results_header"))
                        .open();
            }
        });
    }

    /**
     * The method called when documents updates push is done.
     *
     * @param results the results of the documents updates push task
     */
    public void onUpdatesPushDone(final DocumentsUpdatesPushProcess.Results results) {
        try {
            appContext.getTask(DocumentsSyncTask.class).syncDocuments(results.getSuccessResultsList());
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
        asyncExec(new Runnable() {
            @Override
            public void run() {
                update();
                new ResultsWindow(AppWindow.this, results)
                        .setTitle(textBundle.getString("results_dialog_push_updates_results_title"))
                        .setHeaderText(textBundle.getString("results_dialog_push_updates_results_header"))
                        .open();
            }
        });
    }

    /**
     * The method called when changes synchronization is done.
     *
     * @param results the results of the changes synchronization task
     */
    public void onChangesSyncDone(final ChangesSyncProcess.Results results) {
        try {
            appContext.getTask(DocumentsSyncTask.class).syncDocuments(results.getSuccessResultsList());
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
        asyncExec(new Runnable() {
            @Override
            public void run() {
                update();
                new ResultsWindow(AppWindow.this, results)
                        .setTitle(textBundle.getString("results_dialog_changes_sync_results_title"))
                        .setHeaderText(textBundle.getString("results_dialog_changes_sync_results_header"))
                        .open();
            }
        });
    }

    @Override
    protected MenuManager createMenuManager() {
        MenuManager menuManager = new MenuManager("menu");

        //File menu
        MenuManager fileMenuManager = new MenuManager(textBundle.getString("menu_file"));
        menuManager.add(fileMenuManager);

        fileMenuManager.add(new Action(textBundle.getString("menu_manage_secret")) {
            @Override
            public void run() {
                if (keyManager.isKeyStoreUnlocked()) {
                    KeyStoreManageKeysWindow keyStoreUnlockedDialog =
                            new KeyStoreManageKeysWindow(AppWindow.this);
                    keyStoreUnlockedDialog.open();
                } else {
                    try {
                        switch (unlockKeystore()) {
                            case Success:
                                // try to sync files
                                appContext.getTask(DocumentsSyncTask.class).start();
                                break;
                            case Exit:
                                exit(false);
                                return;
                        }
                    } catch (TaskCreationException e) {
                        LOG.error("Failed to get task {}",
                                e.getTaskClass().getCanonicalName(), e);
                    } catch (DatabaseConnectionClosedException e) {
                        LOG.error("Failed to unlock the database", e);
                        showErrorMessage(textBundle.getString(
                                "error_message_unable_to_unlock_the_database"));
                        exit(false);
                        return;
                    } catch (StorageCryptException e) {
                        LOG.error("Failed to unlock the database", e);
                        showErrorMessage(textBundle.getString(
                                "error_message_unable_to_unlock_the_database"));
                        exit(false);
                        return;
                    }
                    update();
                }
            }
        });

        fileMenuManager.add(new Action(textBundle.getString("menu_clear_cache")) {
            @Override
            public void run() {
                fileSystem.removeCacheFiles();
            }
        });

        fileMenuManager.add(new Action(textBundle.getString("menu_settings")) {
            @Override
            public void run() {
                SettingsDialog settingsDialog = new SettingsDialog(AppWindow.this);
                settingsDialog.open();
                if (settingsDialog.isResultPositive()) {
                    DesktopNetwork.setupProxy(settings);
                }
            }
        });

        fileMenuManager.add(new Action(textBundle.getString("menu_exit")) {
            @Override
            public void run() {
                exit(true);
            }
        });

        //Help Menu
        MenuManager helpMenuManager = new MenuManager(textBundle.getString("menu_help"));
        menuManager.add(helpMenuManager);

        helpMenuManager.add(new Action(textBundle.getString("menu_help")) {
            @Override
            public void run() {
                CachedResources cachedResources = new CachedResources(resources);
                cachedResources.load("/res/text/help_res.json");
                try {
                    cachedResources.extractResourcesTo(fileSystem.getCacheFilesDir());
                    File helpFile = cachedResources.getIndexFile(fileSystem.getCacheFilesDir(),
                            String.format("help_%s.html", Locale.getDefault().getLanguage()));
                    if (null==helpFile) {
                        helpFile = cachedResources.getIndexFile(fileSystem.getCacheFilesDir(),
                                "help.html");
                    }
                    new HelpBrowserWindow(AppWindow.this, "file://"+helpFile.getAbsolutePath())
                            .open();
                } catch (IOException e) {
                    LOG.error("Failed to open help file", e);
                }
            }
        });

        helpMenuManager.add(new Action(textBundle.getString("menu_about")) {
            @Override
            public void run() {
                String markdownText = resources.loadText(
                        String.format("/res/text/about_%s.md", Locale.getDefault().getLanguage()));
                if (null == markdownText) {
                    markdownText = resources.loadText("/res/text/about.md");
                }

                if (null != markdownText) {
                    MarkdownProcessor m = new MarkdownProcessor();
                    String htmlText = m.markdown(markdownText);
                    new AboutBrowserWindow(AppWindow.this, htmlText).open();
                }
            }
        });

        return menuManager;
    }

    @Override
    protected ToolBarManager createToolBarManager(int style) {
        ToolBarManager toolBarManager = new ToolBarManager(style);

        toolBarAddCloudAction = new Action() {
            @Override
            public void run() {
                try {
                    executeCreation();
                } catch (DatabaseConnectionClosedException e) {
                    LOG.error("Database is closed", e);
                }
            }
        };
        toolBarAddCloudAction.setImageDescriptor(new ImageDescriptor() {
            @Override
            public ImageData getImageData() {
                return createCloudImage.getImageData();
            }
        });
        toolBarManager.add(toolBarAddCloudAction);

        toolBarCreateFolderAction = new Action() {
            @Override
            public void run() {
                try {
                    executeCreation();
                } catch (DatabaseConnectionClosedException e) {
                    LOG.error("Database is closed", e);
                }
            }
        };
        toolBarCreateFolderAction.setImageDescriptor(new ImageDescriptor() {
            @Override
            public ImageData getImageData() {
                return createFolderImage.getImageData();
            }
        });
        toolBarManager.add(toolBarCreateFolderAction);

        toolBarEncryptAction = new Action() {
            @Override
            public void run() {
                executeEncryption();            }
        };
        toolBarEncryptAction.setImageDescriptor(new ImageDescriptor() {
            @Override
            public ImageData getImageData() {
                return encryptImage.getImageData();
            }
        });
        toolBarManager.add(toolBarEncryptAction);

        return toolBarManager;
    }

    /**
     * Creates a new document.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void executeCreation() throws DatabaseConnectionClosedException {
        System.out.println("Create");
        if (Constants.STORAGE.ROOT_PARENT_ID == currentFolderId) {
            if (!cloudAppKeys.found()) {
                showErrorMessage(textBundle.getString("error_message_no_cloud_app_keys"));
            } else {
                CreateRootDialog createRootDialog = new CreateRootDialog(this);
                createRootDialog.open();
                if (createRootDialog.isResultPositive()) {
                    AuthBrowserDialog authBrowserDialog = new AuthBrowserDialog(this,
                            appContext.getRemoteStorage(createRootDialog.getStorageType()));
                    authBrowserDialog.open();
                    if (authBrowserDialog.isSuccess()) {
                        try {
                            Account account = accounts.connectWithAccessCode(
                                    createRootDialog.getStorageType(),
                                    createRootDialog.getKeyAlias(),
                                    authBrowserDialog.getResponseParameters());
                            update();
                            if (null != account) {
                                appContext.getTask(ChangesSyncTask.class).sync(account, true);
                            }
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}",
                                    e.getTaskClass().getCanonicalName(), e);
                        } catch (RemoteException e) {
                            LOG.error("Failed to add account", e);
                            showErrorMessage(textBundle.getString("error_message_failed_to_add_account_check_proxy_settings"));
                        }
                    } else {
                        showErrorMessage(textBundle.getString("error_message_failed_to_add_account"));
                    }
                }
            }
        } else {
            CreateFolderDialog createFolderDialog = new CreateFolderDialog(this, currentFolder);
            createFolderDialog.open();
            if (createFolderDialog.isResultPositive()) {
                try {
                    if (null!=currentFolder) {
                        EncryptedDocument encryptedDocument = currentFolder.createChild(
                                createFolderDialog.getFolderName(),
                                Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE,
                                createFolderDialog.getKeyAlias());
                        update();
                        if (!encryptedDocument.isUnsynchronized()) {
                            appContext.getTask(DocumentsSyncTask.class).syncDocument(encryptedDocument);
                        }
                    }
                } catch (TaskCreationException e) {
                    LOG.error("Failed to get task {}",
                            e.getTaskClass().getCanonicalName(), e);
                } catch (StorageCryptException e) {
                    LOG.error("Error while creating document", e);
                    String message = textBundle.getString("error_message_failed_to_create_document");
                    switch (e.getReason()) {
                        case CreationError:
                            message = textBundle.getString("error_message_failed_to_create_document");
                            break;
                        case DocumentExists:
                            message = textBundle.getString("error_message_document_exists", createFolderDialog.getFolderName());
                            break;
                        case KeyStoreIsLocked:
                            message = textBundle.getString("error_message_failed_to_create_document_keystore_is_locked");
                            break;
                    }
                    showErrorMessage(message);
                }
            }
        }
    }

    /**
     * Encrypts a document.
     */
    public void executeEncryption() {
        if (Constants.STORAGE.ROOT_PARENT_ID == currentFolderId) {
            showErrorMessage(textBundle.getString("error_message_you_cannot_encrypt_a_document_here"));
        } else {
            DocumentChooserDialog documentChooserDialog = new DocumentChooserDialog(this);
            String file = documentChooserDialog.chooseFile(
                    textBundle.getString("document_chooser_select_file_to_encrypt_title"));
            if (null!=file && !file.isEmpty()) {
                showEncryptDocumentsSelectKeyDialog(currentFolder, new String[] { file } );
            }
        }
    }

    @Override
    public void showEncryptDocumentsSelectKeyDialog(EncryptedDocument destinationFolder, String[] documentsToEncrypt) {
        if (null!=destinationFolder) {
            List<String> documents = new ArrayList<>();
            for (String document : documentsToEncrypt) {
                documents.addAll(fileSystem.getRecursiveDocumentsList(document));
            }
            if (!documents.isEmpty()) {
                EncryptDocumentsDialog encryptDocumentsDialog =
                        new EncryptDocumentsDialog(this, destinationFolder, documents);
                encryptDocumentsDialog.open();
                if (encryptDocumentsDialog.isResultPositive()) {
                    String keyAlias = encryptDocumentsDialog.getKeyAlias();
                    if (null != keyAlias) {
                        try {
                            appContext.getTask(DocumentsEncryptionTask.class)
                                    .encrypt(destinationFolder, keyAlias, documents);
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}",
                                    e.getTaskClass().getCanonicalName(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a dialog with the given {@code title} and {@code message}.
     *
     * @param title   the text to display in the title bar
     * @param message the message to display
     */
    public void showMessage(final String title, final String message) {
        MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
        messageBox.setText(title);
        messageBox.setMessage(message);
        messageBox.open();
    }

    /**
     * Shows an error dialog with the given {@code message}.
     *
     * @param message the message to display
     */
    public void showErrorMessage(String message) {
        MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
        messageBox.setText(textBundle.getString("alert_dialog_error_title"));
        messageBox.setMessage(message);
        messageBox.open();
    }

    /**
     * Shows a confirmation dialog with the given {@code message}.
     *
     * @param message the message to display
     * @return true if the user pressed the positive result button
     */
    public boolean askForConfirmation(String message) {
        return askForConfirmation(textBundle.getString("confirmation_request_dialog_title"), message);
    }

    /**
     * Shows a confirmation dialog with the given {@code message}.
     *
     * @param title   the text to display in the title bar
     * @param message the message to display
     * @return true if the user pressed the positive result button
     */
    public boolean askForConfirmation(String title, String message) {
        MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        messageBox.setText(title);
        messageBox.setMessage(message);
        int buttonID = messageBox.open();
        switch (buttonID) {
            case SWT.YES:
                return true;
            case SWT.NO:
                return false;
        }
        return false;
    }

    /**
     * Shows the confirmation dialog for deleting a key from the keystore.
     *
     * @param keyAlias the alias of the key to delete
     * @return true if the user choosed to delete the key
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public boolean deleteKeyStoreKey(String keyAlias) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> documentsList =
                encryptedDocuments.encryptedDocumentsWithKeyAlias(keyAlias);
        String message;
        if (documentsList.isEmpty()) {
            message = textBundle.getString("delete_key_dialog_confirmation_message", keyAlias);
        } else {
            message = textBundle.getString(
                    "delete_key_dialog_confirmation_message_referenced_documents",
                    keyAlias, documentsList.size());
        }
        if (askForConfirmation(textBundle.getString("delete_key_dialog_title"), message)) {
            LOG.debug("Delete key {}", keyAlias);
            keyManager.deleteKeys(keyAlias);
            return true;
        }
        return false;
    }

    /**
     * Shows the dialog to rename a key in the keystore.
     *
     * @param keyAlias the alias of the key to rename
     * @return true if the user renamed the key successfully
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public boolean renameKeyStoreKey(String keyAlias) throws DatabaseConnectionClosedException {
        TextInputDialog newKeyAliasDialog = new TextInputDialog(this)
                .setTitle(textBundle.getString("rename_key_dialog_title"))
                .setPromptText(textBundle.getString("rename_key_dialog_message"))
                .setPositiveButtonText(textBundle.getString("rename_key_dialog_rename_button_text"))
                .setNegativeButtonText(textBundle.getString("rename_key_dialog_cancel_button_text"))
                .setAllowedCharacters(Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS);
        newKeyAliasDialog.open();
        if (newKeyAliasDialog.isResultPositive()) {
            String newKeyAlias = newKeyAliasDialog.getInputValue();
            if (null==newKeyAlias || newKeyAlias.isEmpty()) {
                showErrorMessage(textBundle.getString("error_message_failed_to_rename_key_empty_name", keyAlias));
            } else {
                try {
                    if (!keyManager.renameKeys(keyAlias, newKeyAlias)) {
                        showErrorMessage(textBundle.getString("error_message_failed_to_rename_key", keyAlias, newKeyAlias));
                    } else {
                        List<EncryptedDocument> documents =
                                encryptedDocuments.encryptedDocumentsWithKeyAlias(keyAlias);
                        for (EncryptedDocument encryptedDocument : documents) {
                            encryptedDocument.updateKeyAlias(newKeyAlias);
                        }
                        return true;
                    }
                } catch (CryptoException e) {
                    showErrorMessage(textBundle.getString("error_message_failed_to_rename_key", keyAlias, newKeyAlias));
                }
            }
        }
        return false;
    }

    /**
     * Shows the dialog to add a new key to the keystore.
     *
     * @return true if the user entered a new key name successfully
     */
    public boolean addKeyStoreKey() {
        TextInputDialog textInputDialog = new TextInputDialog(this)
                .setTitle(textBundle.getString("new_key_dialog_title"))
                .setPromptText(textBundle.getString("new_key_dialog_message"))
                .setPositiveButtonText(textBundle.getString("new_key_dialog_generate_button_text"))
                .setNegativeButtonText(textBundle.getString("new_key_dialog_cancel_button_text"))
                .setAllowedCharacters(Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS);
        textInputDialog.open();
        if (textInputDialog.isResultPositive()) {
            String newKeyAlias = textInputDialog.getInputValue();
            if (null != newKeyAlias && !newKeyAlias.isEmpty()) {
                try {
                    if (null != keyManager.getKeys(newKeyAlias)) {
                        showErrorMessage(textBundle.getString("error_message_key_already_exists"));
                    } else {
                        keyManager.generateKeys(newKeyAlias);
                        return true;
                    }
                } catch (CryptoException e) {
                    LOG.error("Failed to create key \"{}\"", newKeyAlias, e);
                    showErrorMessage(textBundle.getString("error_message_failed_to_create_key", newKeyAlias));
                }
            }
        }
        return false;
    }

    /**
     * Returns the key aliases in the application key store.
     *
     * @return the key aliases in the application key store
     */
    public List<String> getKeyStoreKeyAliases() {
        return keyManager.getKeyAliases();
    }

    /**
     * Shows the dialog to import keys into the application keystore.
     *
     * @return true if the user successfully selected keys to import
     */
    public boolean importKeyStoreKeys() {
        DocumentChooserDialog documentChooserDialog = new DocumentChooserDialog(this);
        String fileName = documentChooserDialog.chooseFile(
                textBundle.getString("document_chooser_select_keystore_to_import_title"),
                "*.ubr");
        if (null!=fileName) {
            File keyStoreFile = new File(fileName);
            if (!keyStoreFile.exists()) {
                showErrorMessage(textBundle.getString("error_message_keystore_not_found"));
            } else {
                TextInputDialog textInputDialog = new TextInputDialog(this)
                        .setTitle(textBundle.getString("enter_keystore_password_title"))
                        .setPromptText(textBundle.getString("enter_keystore_password_message"))
                        .setPassword(true)
                        .setPositiveButtonText(textBundle.getString("enter_keystore_password_open_button_text"))
                        .setNegativeButtonText(textBundle.getString("enter_keystore_password_cancel_button_text"));
                textInputDialog.open();
                if (textInputDialog.isResultPositive()) {
                    String keystorePassword = textInputDialog.getInputValue();
                    if (null == keystorePassword || keystorePassword.isEmpty()) {
                        showErrorMessage(textBundle.getString("error_message_keystore_password_cannot_be_empty"));
                    } else {
                        KeyStoreUber keyStoreUber = null;
                        try {
                            keyStoreUber = KeyStoreUber.loadKeyStore(keyStoreFile, keystorePassword);
                        } catch (CryptoException e) {
                            LOG.error("Failed to open keystore file \"{}\"", fileName, e);
                            showErrorMessage(textBundle.getString("error_message_failed_to_open_keystore"));
                        } catch (IOException e) {
                            LOG.error("Failed to open keystore file \"{}\"", fileName, e);
                            showErrorMessage(textBundle.getString("error_message_failed_to_open_keystore"));
                        }
                        if (null != keyStoreUber) {
                            List<String> keyStoreKeyAliases = keyStoreUber.getKeyAliases();
                            SelectAndRenameKeysDialog selectAndRenameKeysDialog = new SelectAndRenameKeysDialog(this,
                                    textBundle.getString("select_and_edit_keys_dialog_title"),
                                    textBundle.getString("select_and_edit_keys_to_import_message"),
                                    keyStoreKeyAliases);
                            selectAndRenameKeysDialog.open();
                            if (selectAndRenameKeysDialog.isResultPositive()) {
                                Map<String, String> renamedKeyAliases = selectAndRenameKeysDialog.getRenamedKeyAliases();
                                if (null != renamedKeyAliases && !renamedKeyAliases.isEmpty()) {
                                    try {
                                        keyManager.importKeys(keyStoreUber, renamedKeyAliases);
                                        return true;
                                    } catch (CryptoException e) {
                                        LOG.error("Failed to import keys", e);
                                        showErrorMessage(textBundle.getString("error_message_failed_to_import_keys"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Shows the dialog to export keys from the application keystore.
     */
    public void exportKeyStoreKeys() {
        SelectAndRenameKeysDialog selectAndRenameKeysDialog = new SelectAndRenameKeysDialog(this,
                textBundle.getString("select_and_edit_keys_dialog_title"),
                textBundle.getString("select_and_edit_keys_to_export_message"),
                getKeyStoreKeyAliases());
        selectAndRenameKeysDialog.open();
        if (selectAndRenameKeysDialog.isResultPositive()) {
            Map<String, String> renamedKeyAliases = selectAndRenameKeysDialog.getRenamedKeyAliases();
            if (null != renamedKeyAliases && !renamedKeyAliases.isEmpty()) {
                DocumentChooserDialog documentChooserDialog = new DocumentChooserDialog(this);
                String keyStoreFileName = documentChooserDialog.saveFile(
                        textBundle.getString("document_chooser_select_keystore_to_export_title"),
                        Constants.CRYPTO.KEY_STORE_UBER_DEFAULT_EXPORT_FILE_NAME);
                if (null != keyStoreFileName) {
                    TextInputDialog textInputDialog = new TextInputDialog(this)
                            .setTitle(textBundle.getString("enter_keystore_password_title"))
                            .setPromptText(textBundle.getString("enter_keystore_password_message"))
                            .setConfirmationPromptText(textBundle.getString("enter_keystore_password_confirmation_message"))
                            .setPassword(true)
                            .setPositiveButtonText(textBundle.getString("enter_keystore_password_export_button_text"))
                            .setNegativeButtonText(textBundle.getString("enter_keystore_password_cancel_button_text"));
                    textInputDialog.open();
                    if (textInputDialog.isResultPositive()) {
                        String keyStorePassword = textInputDialog.getInputValue();
                        if (null == keyStorePassword || keyStorePassword.isEmpty()) {
                            showErrorMessage(textBundle.getString("error_message_keystore_password_cannot_be_empty"));
                        } else {
                            try {
                                KeyStoreUber keyStoreUber = keyManager.exportKeys(renamedKeyAliases);
                                File keyStoreFile = new File(keyStoreFileName);
                                keyStoreUber.saveKeyStore(keyStoreFile, keyStorePassword);
                            } catch (CryptoException e) {
                                LOG.error("Failed to export keys", e);
                                showErrorMessage(textBundle.getString("error_message_failed_to_export_keys"));
                            } catch (IOException e) {
                                LOG.error("Failed to export keys", e);
                                showErrorMessage(textBundle.getString("error_message_failed_to_export_keys"));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows the dialog to change the application key store password.
     */
    public void changeKeyStorePassword() {
        KeyStoreChangePasswordDialog keyStoreChangePasswordDialog =
                new KeyStoreChangePasswordDialog(this);
        keyStoreChangePasswordDialog.open();
        if (keyStoreChangePasswordDialog.isResultPositive()) {
            String newPassword = keyStoreChangePasswordDialog.getNewPassword();
            if (null == newPassword || newPassword.isEmpty()) {
                showErrorMessage(textBundle.getString("error_message_keystore_password_cannot_be_empty"));
            } else {
                String currentPassword = keyStoreChangePasswordDialog.getCurrentPassword();
                if (!keyManager.checkKeyStorePassword(currentPassword)) {
                    showErrorMessage(textBundle.getString("error_message_keystore_wrong_current_password"));
                } else {
                    try {
                        keyManager.changeKeystorePassword(newPassword);
                    } catch (CryptoException e) {
                        LOG.error("Failed to change keystore password", e);
                        showErrorMessage(textBundle.getString("error_message_failed_to_change_keystore_password"));
                    }
                }
            }
        }
    }

    /**
     * Opens the given {@code encryptedDocument}.
     *
     * @param encryptedDocument the encrypted document to open
     */
    public void openFile(EncryptedDocument encryptedDocument) {
        File tempFile = new File(fileSystem.getTempFilesDir(), encryptedDocument.getDisplayName());
        try {
            String tempFilePath = tempFile.getCanonicalPath();
            appContext.getTask(FileDecryptionTask.class).decrypt(
                    encryptedDocument,
                    tempFilePath,
                    new OnCompletedListener<String>() {
                        @Override
                        public void onSuccess(final String result) {
                            asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    Program.launch(result);
                                }
                            });
                        }

                        @Override
                        public void onFailed(final StorageCryptException e) {
                            asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    showErrorMessage(textI18n.getExceptionDescription(e));
                                }
                            });
                        }
                    });
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        } catch (IOException e) {
            LOG.error("Failed to decrypt file \"{}\"", encryptedDocument.getDisplayName(), e);
            showErrorMessage(textBundle.getString("error_message_failed_to_decrypt_file",
                    encryptedDocument.getDisplayName()));
        }
    }

    /**
     * Deletes the given {@code encryptedDocument}.
     *
     * @param encryptedDocument the encrypted document to delete
     */
    public boolean deleteDocument(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        encryptedDocument.refresh();
        if (encryptedDocument.isRoot()) {
            if (encryptedDocument.isUnsynchronizedRoot()) {
                showErrorMessage(textBundle.getString("error_message_you_cannot_delete_the_local_storage_provider"));
                return false;
            }

            List<EncryptedDocument> children = encryptedDocument.children(false);
            if (null!=children && !children.isEmpty()) {
                if (askForConfirmation(textBundle.getString("confirmation_request_message_delete_non_empty_storage_provider"))) {
                    encryptedDocument.removeChildrenReferences();
                    deleteRoot(encryptedDocument.getId());
                    return true;
                } else {
                    return false;
                }
            } else {
                deleteRoot(encryptedDocument.getId());
                return true;
            }
        } else {
            if (encryptedDocument.isFolder()) {
                List<EncryptedDocument> children = encryptedDocument.children(false);
                if (null!=children && !children.isEmpty()) {
                    if (askForConfirmation(textBundle.getString("confirmation_request_message_delete_non_empty_folder"))) {
                        deleteFolder(encryptedDocument);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    deleteFolder(encryptedDocument);
                    return true;
                }
            } else {
                encryptedDocument.delete();
                update();
                try {
                    DocumentsSyncTask documentsSyncTask = appContext.getTask(DocumentsSyncTask.class);
                    documentsSyncTask.restartCurrentSync(encryptedDocument);
                    //try to delete the remote file
                    documentsSyncTask.syncDocument(encryptedDocument);
                } catch (TaskCreationException e) {
                    LOG.error("Failed to get task {}",
                            e.getTaskClass().getCanonicalName(), e);
                }
                return true;
            }
        }
    }

    private void deleteRoot(long documentId) throws DatabaseConnectionClosedException {
        final EncryptedDocument encryptedDocument =
                encryptedDocuments.encryptedDocumentWithId(documentId);
        if (null == encryptedDocument) {
            return;
        }
        try {
            appContext.getTask(DocumentsSyncTask.class).cancel();
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
        try {
            encryptedDocument.deleteRoot();
            File localRootFolder = encryptedDocument.file();
            fileSystem.deleteFolder(localRootFolder);
            encryptedDocuments.updateRoots();
        } catch (StorageCryptException e) {
            LOG.error("Error when deleting account", e);
            showErrorMessage(textBundle.getString("error_message_failed_to_delete_account"));
        }
        try {
            appContext.getTask(DocumentsSyncTask.class).start();
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}",
                    e.getTaskClass().getCanonicalName(), e);
        }
    }

    private void deleteFolder(EncryptedDocument folder) throws DatabaseConnectionClosedException {
        for (EncryptedDocument document : folder.unfoldAsList(false)) {
            document.delete();
            update();
            try {
                DocumentsSyncTask documentsSyncTask = appContext.getTask(DocumentsSyncTask.class);
                documentsSyncTask.restartCurrentSync(document);
                //try to delete the remote file
                documentsSyncTask.syncDocument(document);
            } catch (TaskCreationException e) {
                LOG.error("Failed to get task {}",
                        e.getTaskClass().getCanonicalName(), e);
            }
        }
    }
}
