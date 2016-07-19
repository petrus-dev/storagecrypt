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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.tools.storagecrypt.android.Application;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.ShowHelpListener;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsSyncServiceEvent;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * This fragment displays details about an {@code EncryptedDocument}.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class DocumentDetailsFragment extends Fragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    public static final String TAG = "DocumentDetailsFragment";

    /**
     * The argument used to pass the {@code EncryptedDocument} to display the details of
     */
    public static final String BUNDLE_ENCRYPTED_DOCUMENT_ID = "encrypted_document_id";

    private FragmentListener fragmentListener;

    /**
     * The interface used by this fragment to communicate with the Activity.
     */
    public interface FragmentListener extends ShowHelpListener {}

    private FileSystem fileSystem = null;
    private TextI18n textI18n = null;
    private EncryptedDocuments encryptedDocuments = null;

    private RelativeLayout header;
    private ImageView icon;
    private TextView displayName;
    private ImageView downloadIcon;
    private ImageView uploadIcon;
    private ImageView deletionIcon;
    private TextView storageName;

    private TableRow mimeTypeRow;
    private TextView mimeTypeText;
    private TextView keyAliasText;
    private TableRow versionRow;
    private TextView versionText;
    private TableRow localModificationTimeRow;
    private TextView localModificationTimeText;
    private TableRow remoteModificationTimeRow;
    private TextView remoteModificationTimeText;
    private TextView storageTypeText;
    private TableRow storageAccountRow;
    private TextView storageAccountText;
    private TableRow sizeRow;
    private TextView sizeText;
    private TableRow quotaAmountRow;
    private TextView quotaAmountText;
    private TableRow quotaUsedRow;
    private TextView quotaUsedText;
    private TableRow quotaFreeRow;
    private TextView quotaFreeText;

    private long documentId;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Application application = ((Application)activity.getApplication());
        fileSystem = application.getAppContext().getFileSystem();
        textI18n = application.getAppContext().getTextI18n();
        encryptedDocuments = application.getAppContext().getEncryptedDocuments();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        documentId = getArguments().getLong(BUNDLE_ENCRYPTED_DOCUMENT_ID);

        View view = inflater.inflate(R.layout.fragment_document_details, container, false);
        icon = (ImageView) view.findViewById(R.id.icon);
        displayName = (TextView) view.findViewById(R.id.display_name);
        storageName = (TextView) view.findViewById(R.id.storage_name);
        header = (RelativeLayout) view.findViewById(R.id.header);

        downloadIcon = (ImageView) view.findViewById(R.id.download_icon);
        uploadIcon = (ImageView) view.findViewById(R.id.upload_icon);
        deletionIcon = (ImageView) view.findViewById(R.id.delete_icon);

        mimeTypeRow = (TableRow) view.findViewById(R.id.mime_type_row);
        mimeTypeText = (TextView) view.findViewById(R.id.mime_type);
        keyAliasText = (TextView) view.findViewById(R.id.key_alias);
        versionRow = (TableRow) view.findViewById(R.id.version_row);
        versionText = (TextView) view.findViewById(R.id.version);
        localModificationTimeRow = (TableRow) view.findViewById(R.id.local_modification_row);
        localModificationTimeText = (TextView) view.findViewById(R.id.local_modification_time);
        remoteModificationTimeRow = (TableRow) view.findViewById(R.id.remote_modification_row);
        remoteModificationTimeText = (TextView) view.findViewById(R.id.remote_modification_time);
        storageTypeText = (TextView) view.findViewById(R.id.storage_type);
        storageAccountRow = (TableRow) view.findViewById(R.id.storage_account_row);
        storageAccountText = (TextView) view.findViewById(R.id.storage_account);
        sizeRow = (TableRow) view.findViewById(R.id.size_row);
        sizeText = (TextView) view.findViewById(R.id.size);
        quotaAmountRow = (TableRow) view.findViewById(R.id.quota_amount_row);
        quotaAmountText = (TextView) view.findViewById(R.id.quota_amount);
        quotaUsedRow = (TableRow) view.findViewById(R.id.quota_used_row);
        quotaUsedText = (TextView) view.findViewById(R.id.quota_used);
        quotaFreeRow = (TableRow) view.findViewById(R.id.quota_free_row);
        quotaFreeText = (TextView) view.findViewById(R.id.quota_free);

        try {
            updateDetails();
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_document_details_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.action_help:
                fragmentListener.showHelp(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateDetails() throws DatabaseConnectionClosedException {
        EncryptedDocument encryptedDocument = encryptedDocuments.encryptedDocumentWithId(documentId);
        encryptedDocument.log();
        if (null!= encryptedDocument) {
            if (encryptedDocument.isRoot()) {
                displayName.setText(encryptedDocument.storageText());
                if (encryptedDocument.isUnsynchronized()) {
                    icon.setImageResource(R.drawable.ic_folder);
                    storageName.setVisibility(View.GONE);
                } else {
                    icon.setImageResource(R.drawable.ic_cloud);
                    storageName.setText(encryptedDocument.getBackStorageAccount().getAccountName());
                    storageName.setVisibility(View.VISIBLE);
                }
            } else {
                displayName.setText(encryptedDocument.getDisplayName());
                storageName.setText(encryptedDocument.storageText());
                storageName.setVisibility(View.VISIBLE);
                if (encryptedDocument.isFolder()) {
                    icon.setImageResource(R.drawable.ic_folder);
                } else {
                    icon.setImageResource(R.drawable.ic_file);
                }
            }

            if (encryptedDocument.isRoot() || encryptedDocument.isFolder()) {
                mimeTypeRow.setVisibility(View.GONE);
            } else {
                mimeTypeRow.setVisibility(View.VISIBLE);
                mimeTypeText.setText(encryptedDocument.getMimeType());
            }
            keyAliasText.setText(encryptedDocument.getKeyAlias());
            if (encryptedDocument.getBackEntryVersion()>0) {
                versionRow.setVisibility(View.VISIBLE);
                versionText.setText(String.valueOf(encryptedDocument.getBackEntryVersion()));
            } else {
                versionRow.setVisibility(View.GONE);
            }
            if (encryptedDocument.getLocalModificationTime()>0) {
                localModificationTimeRow.setVisibility(View.VISIBLE);
                localModificationTimeText.setText(textI18n.getTimeText(encryptedDocument.getLocalModificationTime()));
            } else {
                localModificationTimeRow.setVisibility(View.GONE);
            }
            if (encryptedDocument.getRemoteModificationTime()>0) {
                remoteModificationTimeRow.setVisibility(View.VISIBLE);
                remoteModificationTimeText.setText(textI18n.getTimeText(encryptedDocument.getRemoteModificationTime()));
            } else {
                remoteModificationTimeRow.setVisibility(View.GONE);
            }
            storageTypeText.setText(textI18n.getStorageTypeText(encryptedDocument.getBackStorageType()));
            if (encryptedDocument.isUnsynchronized()) {
                storageAccountRow.setVisibility(View.GONE);
            } else {
                storageAccountRow.setVisibility(View.VISIBLE);
                storageAccountText.setText(encryptedDocument.getBackStorageAccount().getAccountName());
            }
            if (encryptedDocument.isRoot() || encryptedDocument.isFolder()) {
                sizeRow.setVisibility(View.GONE);
            } else {
                sizeRow.setVisibility(View.VISIBLE);
                sizeText.setText(getString(R.string.size_text, encryptedDocument.getSizeText()));
            }

            switch (encryptedDocument.getSyncState(SyncAction.Upload)) {
                case Done:
                    uploadIcon.setVisibility(View.GONE);
                    break;
                case Planned:
                    uploadIcon.setVisibility(View.VISIBLE);
                    uploadIcon.setImageResource(R.drawable.ic_upload_violet);
                    break;
                case Running:
                    uploadIcon.setVisibility(View.VISIBLE);
                    uploadIcon.setImageResource(R.drawable.ic_upload_green);
                    break;
                case Failed:
                    uploadIcon.setVisibility(View.VISIBLE);
                    uploadIcon.setImageResource(R.drawable.ic_upload_red);
                    break;
            }
            switch (encryptedDocument.getSyncState(SyncAction.Download)) {
                case Done:
                    downloadIcon.setVisibility(View.GONE);
                    break;
                case Planned:
                    downloadIcon.setVisibility(View.VISIBLE);
                    downloadIcon.setImageResource(R.drawable.ic_download_violet);
                    break;
                case Running:
                    downloadIcon.setVisibility(View.VISIBLE);
                    downloadIcon.setImageResource(R.drawable.ic_download_green);
                    break;
                case Failed:
                    downloadIcon.setVisibility(View.VISIBLE);
                    downloadIcon.setImageResource(R.drawable.ic_download_red);
                    break;
            }
            switch (encryptedDocument.getSyncState(SyncAction.Deletion)) {
                case Done:
                    deletionIcon.setVisibility(View.GONE);
                    break;
                case Planned:
                    deletionIcon.setVisibility(View.VISIBLE);
                    deletionIcon.setImageResource(R.drawable.ic_delete_violet);
                    break;
                case Running:
                    deletionIcon.setVisibility(View.VISIBLE);
                    deletionIcon.setImageResource(R.drawable.ic_delete_green);
                    break;
                case Failed:
                    deletionIcon.setVisibility(View.VISIBLE);
                    deletionIcon.setImageResource(R.drawable.ic_delete_red);
                    break;
            }
            long quotaAmount;
            long quotaFree;
            long quotaUsed;
            if (encryptedDocument.isUnsynchronized()) {
                File localAppFolder = fileSystem.getAppDir();
                quotaAmount = localAppFolder.getTotalSpace();
                quotaFree = localAppFolder.getFreeSpace();
                quotaUsed = quotaAmount - quotaFree;
            } else {
                quotaAmount = encryptedDocument.getBackStorageAccount().getQuotaAmount();
                quotaUsed = encryptedDocument.getBackStorageAccount().getQuotaUsed();
                if (quotaAmount>=0 && quotaUsed>=0) {
                    quotaFree = quotaAmount - quotaUsed;
                } else {
                    quotaFree = -1;
                }
            }
            if (quotaAmount>=0 && quotaUsed >=0 && quotaFree>=0) {
                quotaAmountRow.setVisibility(View.VISIBLE);
                quotaUsedRow.setVisibility(View.VISIBLE);
                quotaFreeRow.setVisibility(View.VISIBLE);
                quotaAmountText.setText(textI18n.getSizeText(quotaAmount));
                quotaUsedText.setText(textI18n.getSizeText(quotaUsed));
                quotaFreeText.setText(textI18n.getSizeText(quotaFree));
            } else {
                quotaAmountRow.setVisibility(View.GONE);
                quotaUsedRow.setVisibility(View.GONE);
                quotaFreeRow.setVisibility(View.GONE);
            }
        } else {
            getFragmentManager().popBackStackImmediate();
        }
    }

    /**
     * An {@link EventBus} callback which receives {@code DocumentsListChangeEvent}s.
     *
     * <p>This method updates the displayed details when a change in documents is notified.
     *
     * @param event the {@code DocumentsListChangeEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(DocumentListChangeEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        try {
            updateDetails();
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }
    }

    /**
     * An {@link EventBus} callback which receives {@code DocumentsSyncServiceEvent}s.
     *
     * <p>This method updates the displayed details when a change in the sync service state is notified.
     *
     * @param event the {@code DocumentsSyncServiceEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(DocumentsSyncServiceEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        try {
            updateDetails();
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }
    }
}
