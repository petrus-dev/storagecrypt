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

package fr.petrus.tools.storagecrypt.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.android.Application;
import fr.petrus.tools.storagecrypt.android.DocumentsSelection;
import fr.petrus.tools.storagecrypt.android.activity.IsUnlockedListener;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.ShowDialogListener;
import fr.petrus.tools.storagecrypt.android.activity.ShowHelpListener;
import fr.petrus.tools.storagecrypt.android.events.ChangesSyncDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.ChangesSyncStartEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.tools.storagecrypt.android.adapters.EncryptedDocumentArrayAdapter;
import fr.petrus.tools.storagecrypt.android.events.KeyStoreStateChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsSyncServiceEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ProgressDialogFragment;
import fr.petrus.tools.storagecrypt.android.tasks.ChangesSyncTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsImportTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsUpdatesPushTask;

/**
 * The main fragment of the main Activity, which displays the list of {@code EncryptedDocument}s.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class DocumentListFragment extends Fragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    public static final String TAG = "DocumentListFragment";

    private FragmentListener fragmentListener;

    /**
     * The interface used by this fragment to communicate with the Activity.
     */
    public interface FragmentListener extends IsUnlockedListener, ShowDialogListener, ShowHelpListener {

        /**
         * Returns whether the file containing the cloud app keys was found and successfully loaded.
         *
         * @return true if the file containing the cloud app keys was found and successfully loaded
         */
        boolean hasCloudAppKeys();

        /**
         * Shows the dialog to let the user link a new remote account.
         */
        void onLinkRemoteAccount();

        /**
         * Shows the dialog to let the user create a new encrypted folder.
         */
        void onCreateFolder();

        /**
         * Shows the dialog to choose a file to encrypt.
         */
        void onEncryptFile();

        /**
         * Shows the dialog to choose documents to encrypt.
         */
        void onEncryptDocuments();

        /**
         * Shows the key store management screen.
         */
        void onManageSecret();

        /**
         * Shows the file details screen for the given {@code encryptedDocument}.
         *
         * @param encryptedDocument the encrypted document to show the details of
         */
        void onShowFileDetails(EncryptedDocument encryptedDocument);

        /**
         * Opens the given {@code encryptedDocument} if it is a file, or make it the current folder
         * if it is a folder.
         *
         * @param encryptedDocument the file to open or the folder to change to
         */
        void onOpenFile(EncryptedDocument encryptedDocument);

        /**
         * Opens the system dialog to select an app to share the given {@code encryptedDocument} with.
         *
         * @param encryptedDocument the encrypted document to share
         */
        void onShareFile(EncryptedDocument encryptedDocument);

        /**
         * Opens a dialog to let the user choose the folder where to decrypt the given {@code encryptedDocument}.
         *
         * @param encryptedDocument the encrypted document to decrypt
         */
        void onDecryptDocument(EncryptedDocument encryptedDocument);

        /**
         * Opens a dialog to let the user choose the folder where to decrypt the given {@code encryptedDocuments}.
         *
         * @param encryptedDocuments the encrypted documents to decrypt
         */
        void onDecryptDocuments(List<EncryptedDocument> encryptedDocuments);

        /**
         * Opens a dialog to let the user select another default key to encrypt files in the "top level"
         * folder represented by the given {@code encryptedDocument} with.
         *
         * @param encryptedDocument the "top level" folder to change the default key of
         */
        void onSelectDefaultKey(EncryptedDocument encryptedDocument);

        /**
         * Launches a {@link DocumentsImportTask} for the given {@code encryptedDocument}.
         *
         * @param encryptedDocument the "top level" folder to launch a {@code DocumentsImportTask} for
         */
        void onImportDocuments(EncryptedDocument encryptedDocument);

        /**
         * Launches a {@link DocumentsImportTask} for the given {@code encryptedDocuments}.
         *
         * @param encryptedDocuments the "top level" folders to launch a {@code DocumentsImportTask} for
         */
        void onImportDocuments(List<EncryptedDocument> encryptedDocuments);

        /**
         * Launches a {@link DocumentsUpdatesPushTask} for the given {@code encryptedDocument}.
         *
         * @param encryptedDocument the "top level" folder to launch a {@code DocumentsUpdatesPushTask} for
         */
        void onRefreshRemoteDocuments(EncryptedDocument encryptedDocument);

        /**
         * Launches a {@link DocumentsUpdatesPushTask} for the given {@code encryptedDocuments}.
         *
         * @param encryptedDocuments the "top level" folders to launch a {@code DocumentsUpdatesPushTask} for
         */
        void onRefreshRemoteDocuments(List<EncryptedDocument> encryptedDocuments);

        /**
         * Launches a {@link ChangesSyncTask} for the given {@code account}.
         *
         * @param account the account to launch a {@code ChangesSyncTask} for
         */
        void onSyncRemoteDocumentsChanges(Account account);

        /**
         * Launches a {@link ChangesSyncTask} for the given {@code accounts}.
         *
         * @param accounts the accounts to launch a {@code ChangesSyncTask} for
         */
        void onSyncRemoteDocumentsChanges(List<Account> accounts);

        /**
         * Launches the encryption of the queued files (usually passed to the Activity by another app).
         */
        void onEncryptQueuedFiles();
    }

    private Application application = null;
    private AppContext appContext = null;
    private DocumentsSelection documentsSelection = null;

    private LinearLayout lockedLayout;
    private LinearLayout unlockedLayout;
    private HorizontalScrollView foldersScrollView;
    private LinearLayout parentPathLayout;
    private Button currentFolderButton;
    private ImageButton changesSyncButton;
    private Button syncButton;
    private ListView filesListView;
    private ImageButton selectModeButton;
    private Button encryptButton;

    private long currentFolderId;
    private EncryptedDocument contextMenuTarget;

    /**
     * Creates a new {@code DocumentsListFragment} instance.
     */
    public DocumentListFragment() {
        super();
        currentFolderId = Constants.STORAGE.ROOT_PARENT_ID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (application.isCurrentFolderRoot()) {
            menuInflater.inflate(R.menu.menu_documents_list_fragment_roots, menu);
        } else {
            menuInflater.inflate(R.menu.menu_documents_list_fragment, menu);
        }
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        application = ((Application)activity.getApplication());
        appContext = application.getAppContext();
        documentsSelection = application;
        if (activity instanceof FragmentListener) {
            fragmentListener = (FragmentListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement "+ FragmentListener.class.getName());
        }
    }

    @Override
    public void onDetach() {
        fragmentListener = null;
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        contextMenuTarget = null;
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document_list, container, false);

        lockedLayout = (LinearLayout) view.findViewById(R.id.locked_layout);
        unlockedLayout = (LinearLayout) view.findViewById(R.id.unlocked_layout);

        foldersScrollView = (HorizontalScrollView) view.findViewById(R.id.folders_scrollview);
        parentPathLayout = (LinearLayout) view.findViewById(R.id.parent_path_layout);
        currentFolderButton = (Button) view.findViewById(R.id.current_folder_button);

        registerForContextMenu(currentFolderButton);
        currentFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.showContextMenu();
            }
        });

        currentFolderId = application.getCurrentFolderId();
        updateHeader();

        changesSyncButton = (ImageButton) view.findViewById(R.id.changes_sync_button);
        if (!fragmentListener.hasCloudAppKeys()) {
            changesSyncButton.setVisibility(View.GONE);
        } else {
            changesSyncButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        appContext.getTask(ChangesSyncTask.class).start();
                        fragmentListener.showDialog(new ProgressDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.CHANGES_SYNC_PROGRESS_DIALOG)
                                .setTitle(getString(R.string.progress_text_syncing_remote_documents_changes))
                                .setMessage(getString(R.string.progress_text_syncing_remote_documents_changes))
                                .setDialogCancelable(true).setCancelButton(true).setPauseButton(true)
                                .setProgresses(new Progress(false), new Progress(false)));
                    } catch (TaskCreationException e) {
                        Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                    }
                }
            });
        }

        syncButton = (Button) view.findViewById(R.id.sync_button);
        if (!fragmentListener.hasCloudAppKeys()) {
            syncButton.setVisibility(View.GONE);
        } else {
            syncButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        appContext.getTask(DocumentsSyncTask.class).start();
                        fragmentListener.showDialog(new ProgressDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_SYNC_PROGRESS_DIALOG)
                                .setTitle(getString(R.string.progress_text_syncing_remote_documents))
                                .setMessage(getString(R.string.progress_message_syncing_remote_documents))
                                .setDialogCancelable(true).setCancelButton(true).setPauseButton(true)
                                .setProgresses(new Progress(false), new Progress(false)));
                    } catch (TaskCreationException e) {
                        Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                    }
                }
            });
        }

        filesListView = (ListView) view.findViewById(R.id.files_list_view);
        filesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (documentsSelection.isInSelectionMode()) {
                    if (null!=view) {
                        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
                        checkBox.setChecked(!checkBox.isChecked());
                    }
                } else {
                    EncryptedDocument encryptedDocument =
                            EncryptedDocumentArrayAdapter.getListItemAt(filesListView, position);
                    if (null != encryptedDocument) {
                        fragmentListener.onOpenFile(encryptedDocument);
                    }
                }
            }
        });

        List<EncryptedDocument> documents = application.getCurrentFolderChildren();
        EncryptedDocumentArrayAdapter documentsAdapter;
        if (documentsSelection.isInSelectionMode()) {
            documentsAdapter = new EncryptedDocumentArrayAdapter(getActivity(), documents,
                    documentsSelection);
        } else {
            documentsAdapter = new EncryptedDocumentArrayAdapter(getActivity(), documents);
        }
        filesListView.setAdapter(documentsAdapter);

        selectModeButton = (ImageButton) view.findViewById(R.id.select_mode_button);
        registerForContextMenu(selectModeButton);
        selectModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.showContextMenu();
            }
        });

        encryptButton = (Button) view.findViewById(R.id.encrypt_button);
        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentListener.onEncryptQueuedFiles();
            }
        });

        registerForContextMenu(filesListView);

        selectLayout();

        updateButtonsLayout();

        return view;
    }

    private void selectLayout() {
        if (fragmentListener.isUnlocked()) {
            lockedLayout.setVisibility(View.GONE);
            unlockedLayout.setVisibility(View.VISIBLE);
        } else {
            lockedLayout.setVisibility(View.VISIBLE);
            unlockedLayout.setVisibility(View.GONE);
        }
    }

    private void updateButtonsLayout() {
        int numQueuedFiles = application.getEncryptQueue().size();
        if (numQueuedFiles>0) {
            if (numQueuedFiles==1) {
                encryptButton.setText(getString(R.string.document_list_fragment_encrypt_file_button_text));
            } else {
                encryptButton.setText(getString(R.string.document_list_fragment_encrypt_files_button_text, numQueuedFiles));
            }
            encryptButton.setVisibility(View.VISIBLE);
        } else {
            encryptButton.setVisibility(View.GONE);
        }
        if (documentsSelection.isInSelectionMode()) {
            selectModeButton.setVisibility(View.VISIBLE);
        } else {
            selectModeButton.setVisibility(View.GONE);
        }
    }

    private void updateHeader() {
        parentPathLayout.removeAllViews();
        EncryptedDocument currentFolder = application.getCurrentFolder();
        if (null!=currentFolder) {
            try {
                final List<EncryptedDocument> parents = currentFolder.parents();

                Button button = new Button(getActivity());
                if (Build.VERSION.SDK_INT >= 23) {
                    button.setTextAppearance(android.R.style.TextAppearance_Small);
                } else {
                    button.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
                }
                button.setTransformationMethod(null);
                button.setText(getText(R.string.data_stores_header_text));
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        application.setCurrentFolderId(Constants.STORAGE.ROOT_PARENT_ID);
                        updateHeader();
                    }
                });
                parentPathLayout.addView(button);

                FrameLayout.LayoutParams nextIconLayoutParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER_VERTICAL);

                ImageView nextIcon = new ImageView(getActivity());
                nextIcon.setLayoutParams(nextIconLayoutParams);
                nextIcon.setImageResource(R.drawable.ic_next_black_24dp);
                parentPathLayout.addView(nextIcon);

                for (final EncryptedDocument parent : parents) {
                    button = new Button(getActivity());
                    if (Build.VERSION.SDK_INT >= 23) {
                        button.setTextAppearance(android.R.style.TextAppearance_Small);
                    } else {
                        button.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
                    }
                    button.setTransformationMethod(null);
                    if (parent.isRoot()) {
                        button.setText(parent.storageText());
                    } else {
                        button.setText(parent.getDisplayName());
                    }
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            application.setCurrentFolder(parent);
                            updateHeader();
                        }
                    });
                    parentPathLayout.addView(button);

                    nextIcon = new ImageView(getActivity());
                    nextIcon.setLayoutParams(nextIconLayoutParams);
                    nextIcon.setImageResource(R.drawable.ic_next_black_24dp);
                    parentPathLayout.addView(nextIcon);
                }
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Error when building parent folder path", e);
            }
            if (currentFolder.isRoot()) {
                currentFolderButton.setText(currentFolder.storageText());
            } else {
                currentFolderButton.setText(currentFolder.getDisplayName());
            }
        } else {
            currentFolderButton.setText(R.string.data_stores_header_text);
        }

        foldersScrollView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                foldersScrollView.removeOnLayoutChangeListener(this);
                foldersScrollView.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.action_link_cloud_account:
                fragmentListener.onLinkRemoteAccount();
                return true;
            case R.id.action_create_folder:
                fragmentListener.onCreateFolder();
                return true;
            case R.id.action_encrypt:
                fragmentListener.onEncryptFile();
                return true;
            case R.id.action_encrypt_multiple:
                fragmentListener.onEncryptDocuments();
                return true;
            case R.id.action_order_by_name_asc:
                application.setOrderBy(OrderBy.NameAsc);
                return true;
            case R.id.action_order_by_name_desc:
                application.setOrderBy(OrderBy.NameDesc);
                return true;
            case R.id.action_order_by_mimetype_asc:
                application.setOrderBy(OrderBy.MimeTypeAsc);
                return true;
            case R.id.action_order_by_mimetype_desc:
                application.setOrderBy(OrderBy.MimeTypeDesc);
                return true;
            case R.id.action_order_by_size_asc:
                application.setOrderBy(OrderBy.SizeAsc);
                return true;
            case R.id.action_order_by_size_desc:
                application.setOrderBy(OrderBy.SizeDesc);
                return true;
            case R.id.action_manage_secret:
                fragmentListener.onManageSecret();
                return true;
            case R.id.action_help: {
                fragmentListener.showHelp(null);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private EncryptedDocument getContextMenuTarget(ContextMenu.ContextMenuInfo menuInfo) {
        if (null!=menuInfo && menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            return EncryptedDocumentArrayAdapter.getListItemAt(filesListView, info.position);
        } else {
            return application.getCurrentFolder();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // remember the document for which the context menu is shown
        contextMenuTarget = getContextMenuTarget(menuInfo);

        if (v.getId()==R.id.files_list_view) {
            if (documentsSelection.isInSelectionMode()) {
                createSelectionModeContextMenu(menu);
            } else {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                EncryptedDocument encryptedDocument =
                        EncryptedDocumentArrayAdapter.getListItemAt(filesListView, info.position);
                if (null != encryptedDocument) {
                    createEncryptedDocumentContextMenu(menu, encryptedDocument);
                }
            }
        } else if (v.getId()==R.id.current_folder_button) {
            createCurrentFolderContextMenu(menu);
        } else if (v.getId()==R.id.select_mode_button) {
            if (documentsSelection.isInSelectionMode()) {
                createSelectionModeContextMenu(menu);
            }
        }
    }

    private void createEncryptedDocumentContextMenu(ContextMenu menu, EncryptedDocument encryptedDocument) {
        MenuInflater inflater = getActivity().getMenuInflater();
        if (encryptedDocument.isRoot()) {
            if (encryptedDocument.isUnsynchronizedRoot()) {
                inflater.inflate(R.menu.menu_context_local_root, menu);
            } else {
                inflater.inflate(R.menu.menu_context_root, menu);
            }
        } else if (encryptedDocument.isFolder()) {
            inflater.inflate(R.menu.menu_context_folder, menu);
        } else {
            inflater.inflate(R.menu.menu_context_file, menu);
        }
    }

    private void createCurrentFolderContextMenu(ContextMenu menu) {
        EncryptedDocument encryptedDocument = application.getCurrentFolder();
        if (null== encryptedDocument) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_context_header_data_stores_root, menu);
        } else {
            MenuInflater inflater = getActivity().getMenuInflater();
            if (encryptedDocument.isRoot()) {
                if (encryptedDocument.isUnsynchronizedRoot()) {
                    inflater.inflate(R.menu.menu_context_header_local_root, menu);
                } else {
                    inflater.inflate(R.menu.menu_context_header_root, menu);
                }
            } else if (encryptedDocument.isFolder()) {
                inflater.inflate(R.menu.menu_context_header_folder, menu);
            }
        }
    }

    private void createSelectionModeContextMenu(ContextMenu menu) {
        List<EncryptedDocument> selectedDocuments = documentsSelection.getSelectedDocuments();
        int numRoots = 0;
        int numUnsynchronizedRoots = 0;
        int numFolders = 0;
        int numFiles = 0;
        for (EncryptedDocument encryptedDocument : selectedDocuments) {
            if (encryptedDocument.isUnsynchronizedRoot()) {
                numUnsynchronizedRoots++;
            } else if (encryptedDocument.isRoot()) {
                numRoots++;
            } else if (encryptedDocument.isFolder()) {
                numFolders++;
            } else {
                numFiles++;
            }
        }

        if (selectedDocuments.size() == 1) {
            menu.add(0, R.id.details, 0, getString(R.string.document_context_menu_details));
            menu.add(0, R.id.open, 0, getString(R.string.document_context_menu_open));
            if (1==numFiles) {
                menu.add(0, R.id.share, 0, getString(R.string.document_context_menu_share));
            }
            if (1==numRoots || 1==numFolders) {
                menu.add(0, R.id.select_default_key, 0, getString(R.string.document_context_menu_select_default_key));
            }
        }

        if (0==numUnsynchronizedRoots) {
            menu.add(0, R.id.delete, 0, getString(R.string.document_context_menu_delete));
        }

        if (selectedDocuments.size() == numRoots) {
            menu.add(0, R.id.push_updates, 0, getString(R.string.document_context_menu_push_updates));
            menu.add(0, R.id.sync_remote_changes, 0, getString(R.string.document_context_menu_sync_remote_changes));
        }

        if (selectedDocuments.size() == numUnsynchronizedRoots) {
            menu.add(0, R.id.import_documents, 0, getString(R.string.document_context_menu_import_existing));
        }

        if (0==numRoots && 0==numUnsynchronizedRoots) {
            menu.add(0, R.id.decrypt, 0, getString(R.string.document_context_menu_decrypt));
        }

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_selection_mode, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.selection_mode:
                documentsSelection.clearSelectedDocuments();
                setSelectionMode(true);
                return true;
            case R.id.quit_selection_mode:
                documentsSelection.clearSelectedDocuments();
                setSelectionMode(false);
                return true;
            case R.id.select_all:
                for (EncryptedDocument encryptedDocument : application.getCurrentFolderChildren()) {
                    documentsSelection.setDocumentSelected(encryptedDocument, true);
                }
                DocumentListChangeEvent.postSticky();
                return true;
            case R.id.deselect_all:
                documentsSelection.clearSelectedDocuments();
                DocumentListChangeEvent.postSticky();
                return true;
        }

        if (documentsSelection.isInSelectionMode()) {
            List<EncryptedDocument> selectedDocuments = documentsSelection.getSelectedDocuments();
            switch (item.getItemId()) {
                case R.id.details:
                    if (selectedDocuments.size() == 1) {
                        fragmentListener.onShowFileDetails(selectedDocuments.get(0));
                        clearSelectionMode();
                    }
                    return true;
                case R.id.open:
                    if (selectedDocuments.size() == 1) {
                        fragmentListener.onOpenFile(selectedDocuments.get(0));
                        clearSelectionMode();
                    }
                    return true;
                case R.id.share:
                    if (selectedDocuments.size() == 1) {
                        fragmentListener.onShareFile(selectedDocuments.get(0));
                        clearSelectionMode();
                    }
                    return true;
                case R.id.decrypt:
                    if (!selectedDocuments.isEmpty()) {
                        fragmentListener.onDecryptDocuments(selectedDocuments);
                        clearSelectionMode();
                    }
                    return true;
                case R.id.delete:
                    if (!selectedDocuments.isEmpty()) {
                        try {
                            application.deleteDocuments(selectedDocuments);
                        } catch (DatabaseConnectionClosedException e) {
                            Log.e(TAG, "Database is closed", e);
                        }
                        clearSelectionMode();
                    }
                    return true;
                case R.id.select_default_key:
                    if (selectedDocuments.size() == 1) {
                        fragmentListener.onSelectDefaultKey(selectedDocuments.get(0));
                        clearSelectionMode();
                    }
                    return true;
                case R.id.import_documents:
                    if (!selectedDocuments.isEmpty()) {
                        fragmentListener.onImportDocuments(selectedDocuments);
                        clearSelectionMode();
                    }
                    return true;
                case R.id.push_updates:
                    if (!selectedDocuments.isEmpty()) {
                        fragmentListener.onRefreshRemoteDocuments(selectedDocuments);
                        clearSelectionMode();
                    }
                    return true;
                case R.id.sync_remote_changes:
                    if (!selectedDocuments.isEmpty()) {
                        fragmentListener.onSyncRemoteDocumentsChanges(EncryptedDocuments.getAccounts(selectedDocuments));
                    }
                    return true;
            }
        } else if (null != contextMenuTarget) {
            // get the document for which the context menu is shown
            EncryptedDocument encryptedDocument = contextMenuTarget;

            if (null != encryptedDocument) {
                // reset the context menu target
                contextMenuTarget = null;

                switch (item.getItemId()) {
                    case R.id.details:
                        fragmentListener.onShowFileDetails(encryptedDocument);
                        return true;
                    case R.id.open:
                        fragmentListener.onOpenFile(encryptedDocument);
                        return true;
                    case R.id.share:
                        fragmentListener.onShareFile(encryptedDocument);
                        return true;
                    case R.id.decrypt:
                        fragmentListener.onDecryptDocument(encryptedDocument);
                        return true;
                    case R.id.delete:
                        try {
                            application.deleteDocument(encryptedDocument);
                        } catch (DatabaseConnectionClosedException e) {
                            Log.e(TAG, "Database is closed", e);
                        }
                        return true;
                    case R.id.select_default_key:
                        fragmentListener.onSelectDefaultKey(encryptedDocument);
                        return true;
                    case R.id.import_documents:
                        fragmentListener.onImportDocuments(encryptedDocument);
                        return true;
                    case R.id.push_updates:
                        fragmentListener.onRefreshRemoteDocuments(encryptedDocument);
                        return true;
                    case R.id.sync_remote_changes:
                        if (!encryptedDocument.isUnsynchronized()) {
                            fragmentListener.onSyncRemoteDocumentsChanges(encryptedDocument.getBackStorageAccount());
                        }
                        return true;
                    case R.id.select:
                        documentsSelection.clearSelectedDocuments();
                        documentsSelection.setDocumentSelected(encryptedDocument, true);
                        setSelectionMode(true);
                        return true;
                }
            }
        }

        return super.onContextItemSelected(item);
    }

    private void setSelectionMode(boolean selectionMode) {
        if (selectionMode) {
            documentsSelection.setSelectionMode(true);
            selectModeButton.setVisibility(View.VISIBLE);
        } else {
            documentsSelection.setSelectionMode(false);
            selectModeButton.setVisibility(View.GONE);
        }
    }

    private void clearSelectionMode() {
        documentsSelection.clearSelectedDocuments();
        setSelectionMode(false);
    }

    /**
     * An {@link EventBus} callback which receives {@code KeyStoreStateChangeEvent}s.
     *
     * <p>This method selects the layout to display, depending whether the key store is unlocked.
     * If it is unlocked, it displays the list of documents in the current forder. If it is locked,
     * it simply displays a message saying that the application is locked.
     *
     * @param event the {@code KeyStoreStateChangeEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(KeyStoreStateChangeEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        selectLayout();
    }

    /**
     * An {@link EventBus} callback which receives {@code DocumentListChangeEvent}s.
     *
     * <p>This method updates the documents list when a change is notified.
     *
     * @param event the {@code DocumentListChangeEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(DocumentListChangeEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        final List<EncryptedDocument> encryptedDocuments = application.getCurrentFolderChildren();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Adapter adapter = filesListView.getAdapter();
                if (null!=adapter) {
                    EncryptedDocumentArrayAdapter documentsAdapter =
                            (EncryptedDocumentArrayAdapter) adapter;
                    if (documentsAdapter.isInSelectionMode() != documentsSelection.isInSelectionMode()) {
                        filesListView.setAdapter(null);
                        adapter = null;
                    }
                }
                if (null==adapter) {
                    EncryptedDocumentArrayAdapter documentsAdapter;
                    if (documentsSelection.isInSelectionMode()) {
                        documentsAdapter = new EncryptedDocumentArrayAdapter(getActivity(),
                                encryptedDocuments, documentsSelection);
                    } else {
                        documentsAdapter = new EncryptedDocumentArrayAdapter(getActivity(),
                                encryptedDocuments);
                    }
                    filesListView.setAdapter(documentsAdapter);
                } else {
                    EncryptedDocumentArrayAdapter documentsAdapter =
                            (EncryptedDocumentArrayAdapter) adapter;
                    documentsAdapter.clear();
                    documentsAdapter.addAll(encryptedDocuments);
                    documentsAdapter.notifyDataSetChanged();
                }
                // if the event was triggered because the current folder changed.
                if (currentFolderId != application.getCurrentFolderId()) {
                    getActivity().invalidateOptionsMenu();
                    filesListView.setScrollX(0);
                    currentFolderId = application.getCurrentFolderId();
                    updateHeader();
                }
                updateButtonsLayout();
            }
        });
    }

    /**
     * An {@link EventBus} callback which receives {@code DocumentsSyncServiceEvent}s.
     *
     * <p>This method updates the documents sync button state when a change is notified.
     *
     * @param event the {@code DocumentsSyncServiceEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(DocumentsSyncServiceEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        int progress = event.getDocumentsListProgress().getProgress();
        int max = event.getDocumentsListProgress().getMax();
        SyncAction currentSyncAction = event.getSyncAction();
        if (progress <= max) {
            syncButton.setText(String.format(Locale.getDefault(), "%d/%d", progress, max));
            if (null == currentSyncAction) {
                syncButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_download_black_36dp, 0);
            } else {
                switch (currentSyncAction) {
                    case Upload:
                        syncButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_upload_green_36dp, 0);
                        break;
                    case Download:
                        syncButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_download_green_36dp, 0);
                        break;
                    case Deletion:
                        syncButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_delete_green_36dp, 0);
                        break;
                }
            }
        } else {
            syncButton.setText(String.format(Locale.getDefault(), "%d/%d", 0, 0));
            syncButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_download_black_36dp, 0);
        }
    }

    /**
     * An {@link EventBus} callback which receives {@code ChangesSyncStartEvent}s.
     *
     * <p>This method updates the changes sync button state when a change is notified.
     *
     * @param event the {@code ChangesSyncServiceEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(ChangesSyncStartEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        changesSyncButton.setImageResource(R.drawable.ic_sync_green_36dp);
    }

    /**
     * An {@link EventBus} callback which receives {@code ChangesSyncDoneEvent}s.
     *
     * <p>This method updates the changes sync button state when a change is notified.
     *
     * @param event the {@code ChangesSyncDoneEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(ChangesSyncDoneEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        changesSyncButton.setImageResource(R.drawable.ic_sync_black_36dp);
    }
}
