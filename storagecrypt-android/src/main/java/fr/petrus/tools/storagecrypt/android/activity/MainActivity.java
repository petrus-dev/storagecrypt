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

package fr.petrus.tools.storagecrypt.android.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteStorage;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.exceptions.NetworkException;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.KeyStoreUber;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionException;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.lib.core.processes.ChangesSyncProcess;
import fr.petrus.lib.core.processes.DocumentsDecryptionProcess;
import fr.petrus.lib.core.processes.DocumentsEncryptionProcess;
import fr.petrus.lib.core.processes.DocumentsImportProcess;
import fr.petrus.lib.core.processes.DocumentsUpdatesPushProcess;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.tools.filepicker.FilePicker;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.android.Application;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.StorageCryptService;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedKey;
import fr.petrus.tools.storagecrypt.android.events.ChangesSyncDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsDecryptionDoneEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.KeyStoreNoKeyDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ResultsDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ResultsListDialogFragment;
import fr.petrus.tools.storagecrypt.android.processes.FilesEncryptionProcess;
import fr.petrus.tools.storagecrypt.android.platform.AndroidFileSystem;
import fr.petrus.tools.storagecrypt.android.tasks.ChangesSyncTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsDecryptionTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsEncryptionTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsImportTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsUpdatesPushTask;
import fr.petrus.tools.storagecrypt.android.tasks.FileDecryptionTask;
import fr.petrus.tools.storagecrypt.android.tasks.FilesEncryptionTask;
import fr.petrus.tools.storagecrypt.android.tasks.ServiceTask;
import fr.petrus.tools.storagecrypt.android.utils.UriHelper;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsImportDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsUpdatesPushDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsEncryptionDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.FilesEncryptionDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.KeyListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.KeyStoreStateChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.UnlockKeyStoreEvent;
import fr.petrus.tools.storagecrypt.android.fragments.KeyStoreFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.CreateFolderDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.CustomDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.KeyStoreChangePasswordDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.KeyStoreCreateDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.KeyStoreExportKeysDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.KeyStoreImportKeysDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.KeyStoreUnlockDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ProgressDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.DocumentDetailsFragment;
import fr.petrus.tools.storagecrypt.android.fragments.DocumentListFragment;
import fr.petrus.tools.storagecrypt.android.fragments.PrefsFragment;
import fr.petrus.tools.storagecrypt.android.fragments.WebViewAuthFragment;
import fr.petrus.tools.storagecrypt.android.fragments.WebViewFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.AddStorageDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.AlertDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ConfirmationDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.DropDownListDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.TextInputDialogFragment;

/**
 * The application main Activity.
 *
 * @author Pierre Sagne
 * @since 12.12.2015
 */
public class MainActivity
        extends Activity
        implements
        IsUnlockedListener,
        GetKeyAliasesListener,
        ShowDialogListener,
        ShowHelpListener,
        AlertDialogFragment.DialogListener,
        ConfirmationDialogFragment.DialogListener,
        DropDownListDialogFragment.DialogListener,
        TextInputDialogFragment.DialogListener,
        KeyStoreUnlockDialogFragment.DialogListener,
        KeyStoreCreateDialogFragment.DialogListener,
        KeyStoreNoKeyDialogFragment.DialogListener,
        KeyStoreChangePasswordDialogFragment.DialogListener,
        KeyStoreImportKeysDialogFragment.DialogListener,
        KeyStoreExportKeysDialogFragment.DialogListener,
        CreateFolderDialogFragment.DialogListener,
        AddStorageDialogFragment.DialogListener,
        ProgressDialogFragment.DialogListener,
        KeyStoreFragment.FragmentListener,
        PrefsFragment.FragmentListener,
        DocumentListFragment.FragmentListener,
        DocumentDetailsFragment.FragmentListener,
        WebViewAuthFragment.FragmentListener {

    private static final String TAG = "MainActivity";

    private AppContext appContext;
    private FileSystem fileSystem;
    private Database database;
    private Crypto crypto;
    private KeyManager keyManager;
    private CloudAppKeys cloudAppKeys;
    private TextI18n textI18n;
    private Accounts accounts;
    private EncryptedDocuments encryptedDocuments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Application application = ((Application) getApplication());
        appContext = application.getAppContext();
        fileSystem = appContext.getFileSystem();
        crypto = appContext.getCrypto();
        keyManager = appContext.getKeyManager();
        cloudAppKeys = appContext.getCloudAppKeys();
        textI18n = appContext.getTextI18n();
        database = appContext.getDatabase();
        accounts = appContext.getAccounts();
        encryptedDocuments = appContext.getEncryptedDocuments();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new DocumentListFragment(), DocumentListFragment.TAG)
                    .commit();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(getString(R.string.pref_key_keep_alive), true)) {
            StorageCryptService.startService(this);
        }

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // Handle single file being sent
            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {
                application.getEncryptQueue().add(fileUri);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (fileUris != null) {
                application.getEncryptQueue().addAll(fileUris);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (android.os.Build.VERSION.SDK_INT < 23) {
            finishAppStart();
        } else {
            if (hasReadWriteExternalStoragePermission()) {
                finishAppStart();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.d(TAG, "should request");
                    showDialog(new ConfirmationDialogFragment.Parameters()
                            .setDialogId(AndroidConstants.MAIN_ACTIVITY.READ_WRITE_EXTERNAL_STORAGE_PERMISSION_EXPLANATION_DIALOG)
                            .setTitle(getString(R.string.permission_explanation_dialog_title))
                            .setMessage(getString(R.string.permission_explanation_dialog_message))
                            .setPositiveChoiceText(getString(R.string.permission_explanation_dialog_continue_button_text))
                            .setNegativeChoiceText(getString(R.string.permission_explanation_dialog_exit_button_text)));
                } else {
                    Log.d(TAG, "should not request");
                    requestReadWriteExternalStoragePermission();
                }
            }
        }
    }

    private void finishAppStart() {
        fileSystem.createAppDir();
        if (keyManager.isKeyStoreUnlocked()) {
            try {
                appContext.getTask(DocumentsSyncTask.class).start();
            } catch (TaskCreationException e) {
                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
            }
        } else {
            UnlockKeyStoreEvent.postSticky();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @TargetApi(23)
    private boolean hasReadWriteExternalStoragePermission() {
        Log.d(TAG, "hasReadWriteExternalStoragePermission()");
        return PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @TargetApi(23)
    private void requestReadWriteExternalStoragePermission() {
        Log.d(TAG, "requestReadWriteExternalStoragePermission()");
        requestPermissions(
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                AndroidConstants.MAIN_ACTIVITY.REQUEST_PERMISSION_READ_WRITE_EXTERNAL_STORAGE);
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        switch (requestCode) {
            case AndroidConstants.MAIN_ACTIVITY.REQUEST_PERMISSION_READ_WRITE_EXTERNAL_STORAGE:
                for (int grantResult : grantResults) {
                    Log.d(TAG, "permission " + Arrays.toString(permissions) + " = " + grantResult);
                    if (PackageManager.PERMISSION_GRANTED != grantResult) {
                        showDialog(new ConfirmationDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.READ_WRITE_EXTERNAL_STORAGE_PERMISSION_REFUSED_DIALOG)
                                .setTitle(getString(R.string.permission_refused_dialog_title))
                                .setMessage(getString(R.string.permission_refused_dialog_message))
                                .setPositiveChoiceText(getString(R.string.permission_refused_dialog_restart_button_text))
                                .setNegativeChoiceText(getString(R.string.permission_refused_dialog_exit_button_text)));
                        return;
                    }
                }
                finishAppStart();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (keyManager.isKeyStoreUnlocked()) {
            Fragment f = getFragmentManager().findFragmentById(R.id.container);
            if (f instanceof DocumentListFragment) {
                Application application = ((Application) getApplication());
                if (application.isInSelectionMode()) {
                    application.clearSelectedDocuments();
                    application.setSelectionMode(false);
                    return;
                } else {
                    if (application.backToParentFolder()) {
                        return;
                    }
                }
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                getFragmentManager().popBackStack();
                return true;
            }
            case R.id.action_settings: {
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new PrefsFragment(), PrefsFragment.TAG)
                        .addToBackStack(PrefsFragment.TAG)
                        .commit();
                return true;
            }
            case R.id.action_about: {
                WebViewFragment webViewFragment = new WebViewFragment();
                Bundle args = new Bundle();
                args.putInt(WebViewFragment.BUNDLE_MARKDOWN_TEXT_FILE_ID, R.raw.about);
                webViewFragment.setArguments(args);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.container, webViewFragment, WebViewFragment.TAG);
                transaction.addToBackStack(WebViewFragment.TAG);
                transaction.commit();
                return true;
            }
            case R.id.action_back: {
                getFragmentManager().popBackStack();
                return true;
            }
            case R.id.action_quit: {
                exitApp();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void exitApp() {
        try {
            appContext.getTask(ChangesSyncTask.class).stop();
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        try {
            appContext.getTask(DocumentsSyncTask.class).stop();
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        StorageCryptService.stopService(this);
        keyManager.lockKeyStore();
        DocumentListChangeEvent.postSticky();
        finish();
    }

    @Override
    public void showHelp(String anchor) {
        WebViewFragment webViewFragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putInt(WebViewFragment.BUNDLE_HTML_TEXT_FILE_ID, R.raw.help);
        if (null != anchor) {
            args.putString(WebViewFragment.BUNDLE_HTML_ANCHOR, anchor);
        }
        webViewFragment.setArguments(args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, webViewFragment, WebViewFragment.TAG);
        transaction.addToBackStack(WebViewFragment.TAG);
        transaction.commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Application application = ((Application) getApplication());
        switch (requestCode) {
            case AndroidConstants.MAIN_ACTIVITY.INTENT_PICK_ENCRYPTION_SOURCE_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        showDialog(new DropDownListDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.ENCRYPT_DOCUMENT_KEY_SELECTION_LIST_DIALOG)
                                .setTitle(getString(R.string.choose_key_alias_fragment_choose_encryption_key_alias_text))
                                .setChoiceList(keyManager.getKeyAliases())
                                .setDefaultChoice(application.getCurrentFolder().getKeyAlias())
                                .setPositiveChoiceText(getString(R.string.choose_key_alias_fragment_select_button_text))
                                .setNegativeChoiceText(getString(R.string.choose_key_alias_fragment_cancel_button_text))
                                .setParameter(uri));
                    }
                }
                break;
            case AndroidConstants.MAIN_ACTIVITY.INTENT_CREATE_DECRYPTION_DESTINATION_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        List<EncryptedDocument> selectedDocuments = application.getDocumentsReferences();
                        if (selectedDocuments.size() == 1) {
                            try {
                                appContext.getTask(FileDecryptionTask.class).decrypt(selectedDocuments.get(0), uri);
                            } catch (TaskCreationException e) {
                                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                            }
                            application.clearDocumentsReferences();
                        }
                    }
                }
                break;
            case AndroidConstants.MAIN_ACTIVITY.INTENT_SELECT_DECRYPTION_DESTINATION_FOLDER:
                if (resultCode == Activity.RESULT_OK) {
                    String[] documents = resultData.getStringArrayExtra(FilePicker.INTENT_RESULT_FILES);
                    if (null != documents && 1 == documents.length) {
                        String dstFolder = documents[0];
                        List<EncryptedDocument> srcDocuments = application.getDocumentsReferences();
                        if (!srcDocuments.isEmpty() && null != dstFolder && !dstFolder.isEmpty()) {
                            try {
                                appContext.getTask(DocumentsDecryptionTask.class).decrypt(srcDocuments, dstFolder);
                            } catch (TaskCreationException e) {
                                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                            }
                            application.clearDocumentsReferences();
                        }
                    }
                }
                break;
            case AndroidConstants.MAIN_ACTIVITY.INTENT_PICK_KEYSTORE_IMPORT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        showDialog(new TextInputDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.IMPORT_KEYSTORE_PASSWORD_TEXT_INPUT_DIALOG)
                                .setTitle(getString(R.string.import_keys_fragment_title))
                                .setPromptText(getString(R.string.import_keys_fragment_password_prompt))
                                .setPassword(true).setConfirmation(false)
                                .setPositiveChoiceText(getString(R.string.import_keys_fragment_unlock_button_text))
                                .setNegativeChoiceText(getString(R.string.cancel_button_text))
                                .setParameter(uri));
                    }
                }
                break;
            case AndroidConstants.MAIN_ACTIVITY.INTENT_CREATE_KEYSTORE_EXPORT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        showDialog(new TextInputDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.EXPORT_KEYSTORE_PASSWORD_TEXT_INPUT_DIALOG)
                                .setTitle(getString(R.string.export_keys_fragment_title))
                                .setPromptText(getString(R.string.export_keys_fragment_password_prompt))
                                .setConfirmationPromptText(getString(R.string.export_keys_fragment_password_confirmation_prompt))
                                .setPassword(true).setConfirmation(true)
                                .setPositiveChoiceText(getString(R.string.export_keys_fragment_export_keys_button_text))
                                .setNegativeChoiceText(getString(R.string.cancel_button_text))
                                .setParameter(uri));
                    }
                }
                break;
            case AndroidConstants.MAIN_ACTIVITY.INTENT_SELECT_ENCRYPTION_MULTIPLE_SOURCE_FILES:
                if (resultCode == Activity.RESULT_OK) {
                    String[] documents = resultData.getStringArrayExtra(FilePicker.INTENT_RESULT_FILES);
                    showDialog(new DropDownListDialogFragment.Parameters()
                            .setDialogId(AndroidConstants.MAIN_ACTIVITY.ENCRYPT_MULTIPLE_DOCUMENTS_KEY_SELECTION_LIST_DIALOG)
                            .setTitle(getString(R.string.choose_key_alias_fragment_choose_encryption_key_alias_text))
                            .setChoiceList(keyManager.getKeyAliases())
                            .setDefaultChoice(application.getCurrentFolder().getKeyAlias())
                            .setPositiveChoiceText(getString(R.string.choose_key_alias_fragment_select_button_text))
                            .setNegativeChoiceText(getString(R.string.choose_key_alias_fragment_cancel_button_text))
                            .setParameter(documents));
                }
                break;
        }
    }

    private void openFileWithApp(EncryptedDocument encryptedDocument) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(AndroidConstants.CONTENT_PROVIDER.BASE_DOCUMENT_URI + encryptedDocument.getId()),
                encryptedDocument.getMimeType());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_no_app_found_to_open)));
        }
    }

    @Override
    public boolean isUnlocked() {
        return keyManager.isKeyStoreUnlocked();
    }

    @Override
    public boolean hasCloudAppKeys() {
        return cloudAppKeys.found();
    }

    @Override
    public void onLinkRemoteAccount() {
        if (!cloudAppKeys.found()) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_no_cloud_app_keys)));
        } else {
            AddStorageDialogFragment.showFragment(getFragmentManager());
        }
    }

    @Override
    public void onCreateFolder() {
        Application application = ((Application) getApplication());
        if (!application.isCurrentFolderRoot()) {
            CreateFolderDialogFragment.showFragment(getFragmentManager(),
                    application.getCurrentFolder().getKeyAlias());
        }
    }

    @Override
    public void onEncryptFile() {
        Application application = ((Application) getApplication());
        if (application.isCurrentFolderRoot()) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_you_cannot_encrypt_a_document_here)));
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, AndroidConstants.MAIN_ACTIVITY.INTENT_PICK_ENCRYPTION_SOURCE_FILE);
        }
    }

    @Override
    public void onEncryptDocuments() {
        Application application = ((Application) getApplication());
        if (application.isCurrentFolderRoot()) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_you_cannot_encrypt_documents_here)));
        } else {
            Intent intent = new Intent(this, FilePicker.class);
            intent.putExtra(FilePicker.INTENT_PARAM_TITLE, getString(R.string.file_picker_choose_documents_to_encrypt_title));
            intent.putExtra(FilePicker.INTENT_PARAM_ROOT_DIR, AndroidFileSystem.getExternalStoragePath());
            intent.putExtra(FilePicker.INTENT_PARAM_MIME_TYPE_FILTER, "*/*");
            intent.putExtra(FilePicker.INTENT_PARAM_SELECTION_MODE, FilePicker.SELECTION_MODE_MULTIPLE_RECURSIVE);
            startActivityForResult(intent, AndroidConstants.MAIN_ACTIVITY.INTENT_SELECT_ENCRYPTION_MULTIPLE_SOURCE_FILES);
        }
    }

    @Override
    public void onManageSecret() {
        Fragment f = getFragmentManager().findFragmentById(R.id.container);
        if (!(f instanceof KeyStoreFragment)) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new KeyStoreFragment(), KeyStoreFragment.TAG)
                    .addToBackStack(KeyStoreFragment.TAG)
                    .commit();
        }
    }

    @Override
    public void onOpenFile(EncryptedDocument encryptedDocument) {
        if (null == encryptedDocument) {
            return;
        }
        if (encryptedDocument.isFolder()) {
            Application application = ((Application) getApplication());
            application.setCurrentFolderId(encryptedDocument.getId());
        } else {
            openFileWithApp(encryptedDocument);
        }
    }

    @Override
    public void onShareFile(EncryptedDocument encryptedDocument) {
        if (null == encryptedDocument) {
            return;
        }
        if (!encryptedDocument.isFolder()) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    Uri.parse(AndroidConstants.CONTENT_PROVIDER.BASE_DOCUMENT_URI + encryptedDocument.getId()));
            shareIntent.setType(encryptedDocument.getMimeType());
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(Intent.createChooser(shareIntent, getString(R.string.file_share_pick_app_title)));
            } catch (ActivityNotFoundException e) {
                showDialog(new AlertDialogFragment.Parameters()
                        .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                        .setMessage(getString(R.string.error_message_no_app_found_to_share)));
            }
        }
    }

    @Override
    public void onShowFileDetails(EncryptedDocument encryptedDocument) {
        if (null == encryptedDocument) {
            return;
        }
        DocumentDetailsFragment documentDetailsFragment = new DocumentDetailsFragment();
        Bundle args = new Bundle();
        args.putLong(DocumentDetailsFragment.BUNDLE_ENCRYPTED_DOCUMENT_ID, encryptedDocument.getId());
        documentDetailsFragment.setArguments(args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, documentDetailsFragment, DocumentDetailsFragment.TAG);
        transaction.addToBackStack(DocumentDetailsFragment.TAG);
        transaction.commit();
    }

    @Override
    public void onDecryptDocument(EncryptedDocument encryptedDocument) {
        if (null == encryptedDocument) {
            return;
        }
        Application application = ((Application) getApplication());
        application.setDocumentReference(encryptedDocument);
        if (encryptedDocument.isFolder()) {
            Intent intent = new Intent(this, FilePicker.class);
            intent.putExtra(FilePicker.INTENT_PARAM_TITLE, getString(R.string.file_picker_choose_decryption_destination_folder_title));
            intent.putExtra(FilePicker.INTENT_PARAM_ROOT_DIR, AndroidFileSystem.getExternalStoragePath());
            intent.putExtra(FilePicker.INTENT_PARAM_SELECTION_MODE, FilePicker.SELECTION_MODE_SINGLE_DIR);
            startActivityForResult(intent, AndroidConstants.MAIN_ACTIVITY.INTENT_SELECT_DECRYPTION_DESTINATION_FOLDER);
        } else {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(encryptedDocument.getMimeType());
            intent.putExtra(Intent.EXTRA_TITLE, encryptedDocument.getDisplayName());
            startActivityForResult(intent, AndroidConstants.MAIN_ACTIVITY.INTENT_CREATE_DECRYPTION_DESTINATION_FILE);
        }
    }

    @Override
    public void onDecryptDocuments(List<EncryptedDocument> encryptedDocuments) {
        if (encryptedDocuments.isEmpty()) {
            return;
        }
        Application application = ((Application) getApplication());
        application.setDocumentsReferences(encryptedDocuments);
        Intent intent = new Intent(this, FilePicker.class);
        intent.putExtra(FilePicker.INTENT_PARAM_TITLE, getString(R.string.file_picker_choose_decryption_destination_folder_title));
        intent.putExtra(FilePicker.INTENT_PARAM_ROOT_DIR, AndroidFileSystem.getExternalStoragePath());
        intent.putExtra(FilePicker.INTENT_PARAM_SELECTION_MODE, FilePicker.SELECTION_MODE_SINGLE_DIR);
        startActivityForResult(intent, AndroidConstants.MAIN_ACTIVITY.INTENT_SELECT_DECRYPTION_DESTINATION_FOLDER);
    }

    @Override
    public void onAlertDialogDone(int dialogId) {
        switch (dialogId) {
            case AndroidConstants.MAIN_ACTIVITY.WRONG_PASSWORD_ALERT_DIALOG:
                UnlockKeyStoreEvent.postSticky();
                break;
            case AndroidConstants.MAIN_ACTIVITY.DATABASE_UNLOCK_ERROR_ALERT_DIALOG:
                exitApp();
                break;
        }
    }

    @Override
    public void onConfirmationDialogPositiveChoice(int dialogId, Object parameter) {
        try {
            Application application = ((Application) getApplication());
            switch (dialogId) {
                case AndroidConstants.MAIN_ACTIVITY.NON_EMPTY_PROVIDER_SUPPRESSION_DIALOG: {
                    long id = (Long) parameter;
                    EncryptedDocument encryptedDocument = encryptedDocuments.encryptedDocumentWithId(id);
                    encryptedDocument.removeChildrenReferences();
                    application.deleteRoot(encryptedDocument);
                    break;
                }
                case AndroidConstants.MAIN_ACTIVITY.NON_EMPTY_FOLDER_SUPPRESSION_DIALOG: {
                    long id = (Long) parameter;
                    EncryptedDocument encryptedDocument = encryptedDocuments.encryptedDocumentWithId(id);
                    application.deleteFolder(encryptedDocument);
                    break;
                }
                case AndroidConstants.MAIN_ACTIVITY.KEY_SUPPRESSION_DIALOG: {
                    String alias = (String) parameter;
                    keyManager.deleteKeys(alias);
                    KeyListChangeEvent.postSticky();
                    break;
                }
                case AndroidConstants.MAIN_ACTIVITY.READ_WRITE_EXTERNAL_STORAGE_PERMISSION_EXPLANATION_DIALOG: {
                    requestReadWriteExternalStoragePermission();
                    break;
                }
                case AndroidConstants.MAIN_ACTIVITY.READ_WRITE_EXTERNAL_STORAGE_PERMISSION_REFUSED_DIALOG: {
                    requestReadWriteExternalStoragePermission();
                    break;
                }
            }
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }
    }

    @Override
    public void onConfirmationDialogNegativeChoice(int dialogId, Object parameter) {
        switch (dialogId) {
            case AndroidConstants.MAIN_ACTIVITY.READ_WRITE_EXTERNAL_STORAGE_PERMISSION_EXPLANATION_DIALOG:
                exitApp();
                break;
            case AndroidConstants.MAIN_ACTIVITY.READ_WRITE_EXTERNAL_STORAGE_PERMISSION_REFUSED_DIALOG:
                exitApp();
                break;
        }
    }

    @Override
    public void onDropDownDialogPositiveChoice(int dialogId, String result, Object parameter) {
        Application application = ((Application) getApplication());
        switch (dialogId) {
            case AndroidConstants.MAIN_ACTIVITY.ENCRYPT_DOCUMENT_KEY_SELECTION_LIST_DIALOG: {
                String keyAlias = result;
                if (null != parameter && parameter instanceof Uri) {
                    if (null != keyAlias && !keyAlias.isEmpty()) {
                        if (application.checkCurrentFolder()) {
                            List<Uri> fileUris = new ArrayList<>();
                            fileUris.add((Uri) parameter);
                            try {
                                appContext.getTask(FilesEncryptionTask.class)
                                        .encrypt(fileUris, application.getCurrentFolder(), keyAlias);
                            } catch (TaskCreationException e) {
                                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                            }
                        } else {
                            showDialog(new AlertDialogFragment.Parameters()
                                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                    .setMessage(getString(R.string.error_message_you_cannot_create_a_document_here)));
                        }
                    }
                }
                break;
            }
            case AndroidConstants.MAIN_ACTIVITY.ENCRYPT_MULTIPLE_DOCUMENTS_KEY_SELECTION_LIST_DIALOG: {
                String keyAlias = result;
                if (null != parameter && parameter instanceof String[]) {
                    String[] documents = (String[]) parameter;
                    if (null != keyAlias && !keyAlias.isEmpty()) {
                        try {
                            appContext.getTask(DocumentsEncryptionTask.class)
                                    .encrypt(documents, application.getCurrentFolder(), keyAlias);
                        } catch (TaskCreationException e) {
                            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                        }
                    }
                }
                break;
            }
            case AndroidConstants.MAIN_ACTIVITY.ENCRYPT_QUEUED_FILES_KEY_SELECTION_LIST_DIALOG: {
                String keyAlias = result;
                if (null != keyAlias && !keyAlias.isEmpty()) {
                    for (Uri fileUri : application.getEncryptQueue()) {
                        Log.d(TAG, "File to encrypt : " + fileUri.toString());
                    }
                    try {
                        appContext.getTask(FilesEncryptionTask.class).encrypt(
                                application.getEncryptQueue(),
                                application.getCurrentFolder(),
                                keyAlias);
                    } catch (TaskCreationException e) {
                        Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                    }
                }
                break;
            }
            case AndroidConstants.MAIN_ACTIVITY.SELECT_ROOT_DEFAULT_KEY_SELECTION_LIST_DIALOG: {
                try {
                    String keyAlias = result;
                    if (null != keyAlias && !keyAlias.isEmpty()) {
                        EncryptedDocument encryptedDocument =
                                encryptedDocuments.encryptedDocumentWithId((Long) parameter);
                        if (null != encryptedDocument && encryptedDocument.isRoot()) {
                            encryptedDocument.updateKeyAlias(keyAlias);
                        }
                    }
                } catch (DatabaseConnectionClosedException e) {
                    Log.e(TAG, "Database is closed", e);
                }
                break;
            }
        }
    }

    @Override
    public void onDropDownDialogNegativeChoice(int dialogId, Object parameter) {
    }

    @Override
    public void onTextInputDialogPositiveChoice(int dialogId, String text, String confirmation, Object parameter) {
        switch (dialogId) {
            case AndroidConstants.MAIN_ACTIVITY.NEW_KEY_ALIAS_TEXT_INPUT_DIALOG: {
                String keyAlias = text;
                if (null != keyAlias && !keyAlias.isEmpty()) {
                    try {
                        if (null != keyManager.getKeys(keyAlias)) {
                            showDialog(new AlertDialogFragment.Parameters()
                                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                    .setMessage(getString(R.string.error_message_key_already_exists)));
                        } else {
                            keyManager.generateKeys(keyAlias);
                            KeyListChangeEvent.postSticky();
                        }
                    } catch (CryptoException e) {
                        showDialog(new AlertDialogFragment.Parameters()
                                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                .setMessage(getString(R.string.error_message_failed_to_get_key, keyAlias)));
                    }
                }
                break;
            }
            case AndroidConstants.MAIN_ACTIVITY.RENAME_KEY_ALIAS_TEXT_INPUT_DIALOG: {
                String oldAlias = (String) parameter;
                String newAlias = text;
                if (null != oldAlias && !oldAlias.isEmpty() && null != newAlias && !newAlias.isEmpty()) {
                    try {
                        if (!keyManager.renameKeys(oldAlias, newAlias)) {
                            showDialog(new AlertDialogFragment.Parameters()
                                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                    .setMessage(getString(R.string.error_message_failed_to_rename_key, oldAlias, newAlias)));
                        } else {
                            List<EncryptedDocument> documents = encryptedDocuments.encryptedDocumentsWithKeyAlias(oldAlias);
                            for (EncryptedDocument encryptedDocument : documents) {
                                encryptedDocument.updateKeyAlias(newAlias);
                            }
                            KeyListChangeEvent.postSticky();
                        }
                    } catch (CryptoException e) {
                        showDialog(new AlertDialogFragment.Parameters()
                                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                .setMessage(getString(R.string.error_message_failed_to_rename_key, oldAlias, newAlias)));
                    } catch (DatabaseConnectionClosedException e) {
                        Log.e(TAG, "Database is closed", e);
                    }
                }
                break;
            }
            case AndroidConstants.MAIN_ACTIVITY.IMPORT_KEYSTORE_PASSWORD_TEXT_INPUT_DIALOG: {
                String password = text;
                Uri keyStoreUri = (Uri) parameter;
                if (null == keyStoreUri) {
                    showDialog(new AlertDialogFragment.Parameters()
                            .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                            .setMessage(getString(R.string.error_message_keystore_file_not_found)));
                } else if (null == password || password.isEmpty()) {
                    showDialog(new AlertDialogFragment.Parameters()
                            .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                            .setMessage(getString(R.string.error_message_keystore_password_cannot_be_empty)));
                } else {
                    try {
                        UriHelper keyStoreUriHelper = new UriHelper(this, keyStoreUri);
                        KeyStoreUber keyStoreUber = KeyStoreUber.loadKeyStore(
                                keyStoreUriHelper.openInputStream(true), password);
                        KeyStoreImportKeysDialogFragment.showFragment(getFragmentManager(), keyStoreUber);
                    } catch (FileNotFoundException e) {
                        showDialog(new AlertDialogFragment.Parameters()
                                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                .setMessage(getString(R.string.error_message_keystore_file_not_found)));
                    } catch (CryptoException e) {
                        showDialog(new AlertDialogFragment.Parameters()
                                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                .setMessage(getString(R.string.error_message_keystore_unlock)));
                    } catch (IOException e) {
                        showDialog(new AlertDialogFragment.Parameters()
                                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                .setMessage(getString(R.string.error_message_keystore_read)));
                    }
                }
                break;
            }
            case AndroidConstants.MAIN_ACTIVITY.EXPORT_KEYSTORE_PASSWORD_TEXT_INPUT_DIALOG: {
                String password = text;
                String passwordConfirmation = confirmation;
                if (null == password || password.isEmpty()) {
                    showDialog(new AlertDialogFragment.Parameters()
                            .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                            .setMessage(getString(R.string.error_message_keystore_password_cannot_be_empty)));
                } else if (!password.equals(passwordConfirmation)) {
                    showDialog(new AlertDialogFragment.Parameters()
                            .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                            .setMessage(getString(R.string.error_message_keystore_password_and_confirmation_do_not_match)));
                } else {
                    Uri keyStoreUri = (Uri) parameter;
                    Application application = ((Application) getApplication());
                    KeyStoreUber exportedKeyStore = application.getExportedKeyStore();
                    if (null != keyStoreUri && null != exportedKeyStore) {
                        try {
                            UriHelper keyStoreUriHelper = new UriHelper(this, keyStoreUri);
                            exportedKeyStore.saveKeyStore(keyStoreUriHelper.openOutputStream(true), password);
                            application.setExportedKeyStore(null);
                        } catch (CryptoException e) {
                            showDialog(new AlertDialogFragment.Parameters()
                                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                    .setMessage(getString(R.string.error_message_keystore_create)));
                        } catch (IOException e) {
                            showDialog(new AlertDialogFragment.Parameters()
                                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                    .setMessage(getString(R.string.error_message_keystore_write)));
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onTextInputDialogNegativeChoice(int dialogId, Object parameter) {
        switch (dialogId) {
            case AndroidConstants.MAIN_ACTIVITY.EXPORT_KEYSTORE_PASSWORD_TEXT_INPUT_DIALOG:
                Application application = ((Application) getApplication());
                application.setExportedKeyStore(null);
                Uri keyStoreUri = (Uri) parameter;
                if (null != keyStoreUri) {
                    UriHelper keyStoreUriHelper = new UriHelper(this, keyStoreUri);
                    keyStoreUriHelper.delete();
                }
                break;
        }
    }

    private ServiceTask getTask(int dialogId) throws TaskCreationException {
        switch (dialogId) {
            case AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_ENCRYPTION_PROGRESS_DIALOG:
                return appContext.getTask(DocumentsEncryptionTask.class);
            case AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_DECRYPTION_PROGRESS_DIALOG:
                return appContext.getTask(DocumentsDecryptionTask.class);
            case AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_IMPORT_PROGRESS_DIALOG:
                return appContext.getTask(DocumentsImportTask.class);
            case AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG:
                return appContext.getTask(DocumentsUpdatesPushTask.class);
            case AndroidConstants.MAIN_ACTIVITY.CHANGES_SYNC_PROGRESS_DIALOG:
                return appContext.getTask(ChangesSyncTask.class);
            case AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_SYNC_PROGRESS_DIALOG:
                return appContext.getTask(DocumentsSyncTask.class);
            case AndroidConstants.MAIN_ACTIVITY.FILE_DECRYPTION_PROGRESS_DIALOG:
                return appContext.getTask(FileDecryptionTask.class);
            case AndroidConstants.MAIN_ACTIVITY.FILES_ENCRYPTION_PROGRESS_DIALOG:
                return appContext.getTask(FilesEncryptionTask.class);
        }
        throw new TaskCreationException("Failed to find a task for dialogId " + dialogId, Void.class);
    }

    @Override
    public void onPauseTask(int dialogId) {
        try {
            getTask(dialogId).pause();
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onResumeTask(int dialogId) {
        try {
            getTask(dialogId).resume();
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public boolean isTaskPaused(int dialogId) {
        try {
            return getTask(dialogId).isPaused();
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        return false;
    }

    @Override
    public void onCancelTask(int dialogId) {
        try {
            getTask(dialogId).cancel();
            //getTask(dialogId).stop();
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onSelectDefaultKey(EncryptedDocument encryptedDocument) {
        Log.d(TAG, "select default key for : " + encryptedDocument.getId());
        if (encryptedDocument.isRoot()) {
            showDialog(new DropDownListDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.SELECT_ROOT_DEFAULT_KEY_SELECTION_LIST_DIALOG)
                    .setTitle(getString(R.string.choose_key_alias_fragment_choose_default_key_alias_text,
                            encryptedDocument.storageText()))
                    .setChoiceList(keyManager.getKeyAliases())
                    .setDefaultChoice(encryptedDocument.getKeyAlias())
                    .setPositiveChoiceText(getString(R.string.choose_key_alias_fragment_select_button_text))
                    .setNegativeChoiceText(getString(R.string.choose_key_alias_fragment_cancel_button_text))
                    .setParameter(encryptedDocument.getId()));
        }
    }

    @Override
    public void onImportDocuments(EncryptedDocument encryptedDocument) {
        Log.d(TAG, "import docs : " + encryptedDocument.getId());
        try {
            appContext.getTask(DocumentsImportTask.class).importDocuments(encryptedDocument);
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onImportDocuments(List<EncryptedDocument> encryptedDocuments) {
        try {
            appContext.getTask(DocumentsImportTask.class).importDocuments(encryptedDocuments);
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onRefreshRemoteDocuments(EncryptedDocument encryptedDocument) {
        Log.d(TAG, "refresh remote docs : " + encryptedDocument.getId());
        try {
            appContext.getTask(DocumentsUpdatesPushTask.class).pushUpdates(encryptedDocument);
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onRefreshRemoteDocuments(List<EncryptedDocument> encryptedDocuments) {
        try {
            appContext.getTask(DocumentsUpdatesPushTask.class).pushUpdates(encryptedDocuments);
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onSyncRemoteDocumentsChanges(Account account) {
        Log.d(TAG, "sync remote docs changes: " + account.getId());
        try {
            appContext.getTask(ChangesSyncTask.class).syncAccount(account, true);
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onSyncRemoteDocumentsChanges(List<Account> accounts) {
        try {
            appContext.getTask(ChangesSyncTask.class).syncAccounts(accounts, true);
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
    }

    @Override
    public void onEncryptQueuedFiles() {
        Application application = ((Application) getApplication());
        if (application.isCurrentFolderRoot()) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_you_cannot_encrypt_documents_here)));
        } else {
            showDialog(new DropDownListDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.ENCRYPT_QUEUED_FILES_KEY_SELECTION_LIST_DIALOG)
                    .setTitle(getString(R.string.choose_key_alias_fragment_choose_encryption_key_alias_text))
                    .setChoiceList(keyManager.getKeyAliases())
                    .setDefaultChoice(application.getCurrentFolder().getKeyAlias())
                    .setPositiveChoiceText(getString(R.string.choose_key_alias_fragment_select_button_text))
                    .setNegativeChoiceText(getString(R.string.choose_key_alias_fragment_cancel_button_text)));
        }
    }

    @Override
    public void onCreateDocument(final String displayName, final String mimeType, final String keyAlias) {
        final Application application = ((Application) getApplication());
        if (application.checkCurrentFolder() && !application.isCurrentFolderRoot()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        EncryptedDocument currentFolder = application.getCurrentFolder();
                        if (null != currentFolder) {
                            EncryptedDocument encryptedDocument =
                                    currentFolder.createChild(displayName, mimeType, keyAlias);
                            if (!encryptedDocument.isUnsynchronized()) {
                                try {
                                    appContext.getTask(DocumentsSyncTask.class)
                                            .syncDocument(encryptedDocument);
                                } catch (TaskCreationException e) {
                                    Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                                }
                            }
                            DocumentListChangeEvent.post();
                        }
                    } catch (DatabaseConnectionClosedException e) {
                        Log.e(TAG, "Database is closed", e);
                    } catch (StorageCryptException e) {
                        Log.e(TAG, "Error while creating document", e);
                        showDialog(new AlertDialogFragment.Parameters()
                                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                                .setMessage(getString(R.string.error_message_failed_to_create_document)));
                    }
                }
            }.start();
        } else {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_you_cannot_create_a_document_here)));
        }
    }

    @Override
    public RemoteStorage getRemoteStorage(StorageType storageType) {
        return  appContext.getRemoteStorage(storageType);
    }


    @Override
    public void onAddStorage(StorageType storageType, String keyAlias) {
        RemoteStorage remoteStorage = appContext.getRemoteStorage(storageType);
        if (null != remoteStorage) {
            WebViewAuthFragment webViewAuthFragment = new WebViewAuthFragment();
            Bundle argsBundle = new Bundle();
            argsBundle.putString(WebViewAuthFragment.BUNDLE_STORAGE_TYPE, storageType.name());
            argsBundle.putString(WebViewAuthFragment.BUNDLE_DEFAULT_KEY_ALIAS, keyAlias);
            webViewAuthFragment.setArguments(argsBundle);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.container, webViewAuthFragment, WebViewAuthFragment.TAG);
            transaction.addToBackStack(WebViewAuthFragment.TAG);
            transaction.commit();
        } else {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_failed_to_add_account)));
        }
    }

    @Override
    public void onAccessCode(final StorageType storageType, final String keyAlias,
                             final Map<String, String> responseParameters) {
        getFragmentManager().popBackStack();
        Log.d(TAG, "Received access code :");
        for (String paramName : responseParameters.keySet()) {
            Log.d(TAG, "  " + paramName + " = " + responseParameters.get(paramName));
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    showDialog(new ProgressDialogFragment.Parameters()
                            .setDialogId(AndroidConstants.MAIN_ACTIVITY.ADD_ACCOUNT_PROGRESS_DIALOG)
                            .setTitle(getString(R.string.progress_text_adding_account))
                            .setProgresses(new Progress().setMessage(storageType.name())));
                    Account account = accounts.connectWithAccessCode(storageType, keyAlias, responseParameters);
                    DocumentListChangeEvent.post();
                    getContentResolver().notifyChange(DocumentsContract.buildRootsUri(AndroidConstants.CONTENT_PROVIDER.AUTHORITY), null);
                    if (null != account) {
                        try {
                            appContext.getTask(ChangesSyncTask.class).syncAccount(account, true);
                        } catch (TaskCreationException e) {
                            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                        }
                    }
                } catch (NetworkException | RemoteException e) {
                    Log.e(TAG, "Error when connecting with access code", e);
                    showDialog(new AlertDialogFragment.Parameters()
                            .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                            .setMessage(getString(R.string.error_message_failed_to_add_account)));
                } catch (DatabaseConnectionClosedException e) {
                    Log.e(TAG, "Database is closed", e);
                }
                new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.ADD_ACCOUNT_PROGRESS_DIALOG).postSticky();
            }
        }.start();
    }

    @Override
    public void onAuthFailed(StorageType storageType, Map<String, String> responseParameters) {
        getFragmentManager().popBackStack();
        Log.d(TAG, "Received error code :");
        for (String paramName : responseParameters.keySet()) {
            Log.d(TAG, "  " + paramName + " = " + responseParameters.get(paramName));
        }
        showDialog(new AlertDialogFragment.Parameters()
                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                .setMessage(getString(R.string.error_message_failed_to_add_account_with_name,
                        textI18n.getStorageTypeText(storageType))));
    }

    @Override
    public void onPreferenceChanged(String key) {
        if (null != key) {
            if (key.equals(getString(R.string.pref_key_deletion_wifi_only))
                    || key.equals(getString(R.string.pref_key_upload_wifi_only))
                    || key.equals(getString(R.string.pref_key_download_wifi_only))) {
                try {
                    appContext.getTask(DocumentsSyncTask.class).start();
                } catch (TaskCreationException e) {
                    Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                }
            } else if (key.equals(getString(R.string.pref_key_keep_alive))) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (prefs.getBoolean(key, true)) {
                    StorageCryptService.startService(this);
                } else {
                    StorageCryptService.stopService(this);
                }
            }
        }
    }

    @Override
    public void onKeyStoreLock() {
        keyManager.lockKeyStore();
        getContentResolver().notifyChange(
                DocumentsContract.buildRootsUri(AndroidConstants.CONTENT_PROVIDER.AUTHORITY), null);
        KeyStoreStateChangeEvent.postSticky();
        getFragmentManager().popBackStack();
    }

    @Override
    public List<String> getKeyAliases() {
        return keyManager.getKeyAliases();
    }

    @Override
    public List<EncryptedDocument> getDocumentsWithKeyAlias(String keyAlias)
            throws DatabaseConnectionClosedException {
        return encryptedDocuments.encryptedDocumentsWithKeyAlias(keyAlias);
    }

    @Override
    public void onKeyStoreChangePassword() {
        KeyStoreChangePasswordDialogFragment.showFragment(getFragmentManager());
    }

    @Override
    public void onKeyStoreCreateKey() {
        showDialog(new TextInputDialogFragment.Parameters()
                .setDialogId(AndroidConstants.MAIN_ACTIVITY.NEW_KEY_ALIAS_TEXT_INPUT_DIALOG)
                .setTitle(getString(R.string.new_key_dialog_fragment_title))
                .setPromptText(getString(R.string.new_key_dialog_fragment_prompt))
                .setAllowedCharacters(Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS)
                .setPositiveChoiceText(getString(R.string.new_key_dialog_fragment_generate_button_text))
                .setNegativeChoiceText(getString(R.string.new_key_dialog_fragment_cancel_button_text)));
    }

    @Override
    public void onKeyStoreExportKeys() {
        KeyStoreExportKeysDialogFragment.showFragment(getFragmentManager());
    }

    @Override
    public void onKeyStoreImportKeys() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, AndroidConstants.MAIN_ACTIVITY.INTENT_PICK_KEYSTORE_IMPORT_FILE);
    }

    @Override
    public void onChangeKeyStorePassword(String currentPassword, String newPassword, String newPasswordConfirmation) {
        if (!keyManager.checkKeyStorePassword(currentPassword)) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_keystore_wrong_current_password)));
        } else if (null == newPassword || newPassword.isEmpty()) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_keystore_password_cannot_be_empty)));
        } else if (!newPassword.equals(newPasswordConfirmation)) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_keystore_password_and_confirmation_do_not_match)));
        } else {
            try {
                keyManager.changeKeystorePassword(newPassword);
                getFragmentManager().popBackStackImmediate();
            } catch (CryptoException e) {
                showDialog(new AlertDialogFragment.Parameters()
                        .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                        .setMessage(getString(R.string.error_message_keystore_change_password)));
                Log.e(TAG, "Failed to change keystore password", e);
            }
        }
    }

    @Override
    public void onKeyStoreCreate(String keyStorePassword, String keyStorePasswordConfirmation) {
        if (null == keyStorePassword || keyStorePassword.isEmpty()) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_keystore_password_cannot_be_empty)));
        } else if (!keyStorePassword.equals(keyStorePasswordConfirmation)) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_keystore_password_and_confirmation_do_not_match)));
        } else if (keyManager.createKeyStore(keyStorePassword)) {
            if (keyManager.getKeyAliases().isEmpty()) {
                KeyStoreNoKeyDialogFragment.showFragment(getFragmentManager());
            } else {
                finishUnlock();
            }
        }
    }

    @Override
    public void onKeyStoreUnlock(String keyStorePassword) {
        if (!keyManager.unlockKeyStore(keyStorePassword)) {
            showDialog(new AlertDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.WRONG_PASSWORD_ALERT_DIALOG)
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_unable_to_unlock_the_keystore_check_your_password)));
        } else {
            if (keyManager.getKeyAliases().isEmpty()) {
                KeyStoreNoKeyDialogFragment.showFragment(getFragmentManager());
            } else {
                finishUnlock();
            }
        }
    }

    @Override
    public void onKeyStoreFirstKeyCreated() {
        finishUnlock();
    }

    private void finishUnlock() {
        try {
            if (unlockDatabase()) {
                try {
                    encryptedDocuments.updateRoots();
                    //refreshes the StorageCryptProvider
                    getContentResolver().notifyChange(
                            DocumentsContract.buildRootsUri(AndroidConstants.CONTENT_PROVIDER.AUTHORITY), null);
                    KeyStoreStateChangeEvent.postSticky();
                    if (accounts.size() > 0L) {
                        try {
                            appContext.getTask(ChangesSyncTask.class).start();
                        } catch (TaskCreationException e) {
                            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                        }
                        try {
                            appContext.getTask(DocumentsSyncTask.class).start();
                        } catch (TaskCreationException e) {
                            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                        }
                    }
                } catch (DatabaseConnectionClosedException e) {
                    Log.e(TAG, "Database is closed", e);
                }
                DocumentListChangeEvent.postSticky();
            } else {
                Log.e(TAG, "Failed to unlock the database");
                showDialog(new AlertDialogFragment.Parameters()
                        .setDialogId(AndroidConstants.MAIN_ACTIVITY.DATABASE_UNLOCK_ERROR_ALERT_DIALOG)
                        .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                        .setMessage(getString(R.string.error_message_unable_to_unlock_the_database)));
            }
        } catch (StorageCryptException e) {
            Log.e(TAG, "Failed to unlock the database", e);
            showDialog(new AlertDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.DATABASE_UNLOCK_ERROR_ALERT_DIALOG)
                    .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                    .setMessage(getString(R.string.error_message_unable_to_unlock_the_database)));
        }

    }

    private boolean unlockDatabase() throws StorageCryptException {
        if (!keyManager.isKeyStoreUnlocked()) {
            return false;
        }

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String encryptedDatabaseEncryptionPassword = prefs.getString(
                AndroidConstants.MAIN_ACTIVITY.PREF_DATABASE_ENCRYPTION_PASSWORD, null);

        if (null == encryptedDatabaseEncryptionPassword) {
            try {
                String databaseEncryptionPassword = crypto.generateRandomPassword(32);
                encryptedDatabaseEncryptionPassword = keyManager.encryptWithDatabaseSecurityKey(databaseEncryptionPassword);

                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putString(AndroidConstants.MAIN_ACTIVITY.PREF_DATABASE_ENCRYPTION_PASSWORD,
                        encryptedDatabaseEncryptionPassword);
                prefsEditor.commit();
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
            showDialog(new ProgressDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.UNLOCK_DATABASE_PROGRESS_DIALOG)
                    .setTitle(getString(R.string.progress_text_unlocking_database))
                    .setProgresses(new Progress()));

            String databaseEncryptionPassword =
                    keyManager.decryptWithDatabaseSecurityKey(encryptedDatabaseEncryptionPassword);
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
        } finally {
            new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.UNLOCK_DATABASE_PROGRESS_DIALOG).postSticky();
        }
    }

    @Override
    public void onImportKeys(KeyStoreUber keyStore, List<SelectedKey> selectedKeys) throws StorageCryptException {
        try {
            keyManager.importKeys(keyStore, SelectedKey.selectedKeysToMap(selectedKeys));
            KeyListChangeEvent.postSticky();
        } catch (CryptoException e) {
            throw new StorageCryptException("Failed to open keystore with the given password",
                    StorageCryptException.Reason.BadPassword, e);
        }
    }

    @Override
    public void onExportKeys(List<SelectedKey> selectedKeys) throws StorageCryptException {
        try {
            Application application = ((Application) getApplication());
            application.setExportedKeyStore(keyManager.exportKeys(SelectedKey.selectedKeysToMap(selectedKeys)));
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_TITLE, Constants.CRYPTO.KEY_STORE_UBER_DEFAULT_EXPORT_FILE_NAME);
            startActivityForResult(intent, AndroidConstants.MAIN_ACTIVITY.INTENT_CREATE_KEYSTORE_EXPORT_FILE);
        } catch (CryptoException e) {
            throw new StorageCryptException("Failed to create keystore",
                    StorageCryptException.Reason.KeyStoreCreationError, e);
        }
    }


    @Override
    public void showDialog(CustomDialogFragment.Parameters parameters) {
        if (null != parameters) {
            if (parameters instanceof AlertDialogFragment.Parameters) {
                AlertDialogFragment.showFragment(getFragmentManager(),
                        (AlertDialogFragment.Parameters) parameters);
            } else if (parameters instanceof ConfirmationDialogFragment.Parameters) {
                ConfirmationDialogFragment.showFragment(getFragmentManager(),
                        (ConfirmationDialogFragment.Parameters) parameters);
            } else if (parameters instanceof DropDownListDialogFragment.Parameters) {
                DropDownListDialogFragment.showFragment(getFragmentManager(),
                        (DropDownListDialogFragment.Parameters) parameters);
            } else if (parameters instanceof TextInputDialogFragment.Parameters) {
                TextInputDialogFragment.showFragment(getFragmentManager(),
                        (TextInputDialogFragment.Parameters) parameters);
            } else if (parameters instanceof ProgressDialogFragment.Parameters) {
                ProgressDialogFragment.showFragment(getFragmentManager(),
                        (ProgressDialogFragment.Parameters) parameters);
            } else if (parameters instanceof ResultsDialogFragment.Parameters) {
                ResultsDialogFragment.showFragment(getFragmentManager(),
                        (ResultsDialogFragment.Parameters) parameters);
            } else if (parameters instanceof ResultsListDialogFragment.Parameters) {
                ResultsListDialogFragment.showFragment(getFragmentManager(),
                        (ResultsListDialogFragment.Parameters) parameters);
            }
        }
    }

    @Override
    public void showErrorDialog(StorageCryptException e) {
        showDialog(new AlertDialogFragment.Parameters()
                .setTitle(getString(R.string.alert_dialog_fragment_error_title))
                .setMessage(textI18n.getExceptionDescription(e)));
    }

    /**
     * An {@link EventBus} callback which receives {@code UnlockKeyStoreEvent}s.
     *
     * <p>This method checks if the key store file exists and displays a dialog to unlock or create
     * it.
     *
     * @param event the {@code UnlockKeyStoreEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(UnlockKeyStoreEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        if (keyManager.isKeyStoreExisting()) {
            KeyStoreUnlockDialogFragment.showFragment(getFragmentManager());
        } else {
            KeyStoreCreateDialogFragment.showFragment(getFragmentManager());
        }
    }

    /**
     * An {@link EventBus} callback which receives a {@code DocumentsEncryptionDoneEvent} when the
     * {@link DocumentsEncryptionTask} has finished its work.
     *
     * @param event the {@code DocumentsEncryptionDoneEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(DocumentsEncryptionDoneEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        DocumentsEncryptionProcess.Results results = event.getResults();
        try {
            appContext.getTask(DocumentsSyncTask.class)
                    .syncDocuments(results.getSuccessfulyEncryptedDocuments());
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        DocumentListChangeEvent.postSticky();
        showDialog(new ResultsDialogFragment.Parameters()
                .setResults(results)
                .setTitle(getString(R.string.results_dialog_encryption_results_title))
                .setMessage(getString(R.string.results_dialog_encryption_results_header)));
    }

    /**
     * An {@link EventBus} callback which receives a {@code DocumentsDecryptionDoneEvent} when the
     * {@link DocumentsDecryptionTask} has finished its work.
     *
     * @param event the {@code DocumentsDecryptionDoneEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(DocumentsDecryptionDoneEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        DocumentsDecryptionProcess.Results results = event.getResults();
        DocumentListChangeEvent.postSticky();
        showDialog(new ResultsDialogFragment.Parameters()
                .setResults(results)
                .setTitle(getString(R.string.results_dialog_decryption_results_title))
                .setMessage(getString(R.string.results_dialog_decryption_results_header)));
    }

    /**
     * An {@link EventBus} callback which receives a {@code FilesEncryptionDoneEvent} when the
     * {@link FilesEncryptionTask} has finished its work.
     *
     * @param event the {@code FilesEncryptionDoneEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(FilesEncryptionDoneEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        Application application = ((Application) getApplication());
        FilesEncryptionProcess.Results results = event.getResults();
        List<Uri> successfulyEncryptedUris = results.getSuccessfulyEncryptedUris();
        application.getEncryptQueue().removeAll(successfulyEncryptedUris);
        try {
            appContext.getTask(DocumentsSyncTask.class)
                    .syncDocuments(results.getSuccessfulyEncryptedDocuments());
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        DocumentListChangeEvent.postSticky();
        showDialog(new ResultsDialogFragment.Parameters()
                .setResults(results)
                .setTitle(getString(R.string.results_dialog_encryption_results_title))
                .setMessage(getString(R.string.results_dialog_encryption_results_header)));
    }

    /**
     * An {@link EventBus} callback which receives a {@code DocumentsImportDoneEvent} when the
     * {@link DocumentsImportTask} has finished its work.
     *
     * @param event the {@code DocumentsImportDoneEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(DocumentsImportDoneEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        DocumentsImportProcess.Results results = event.getResults();
        try {
            appContext.getTask(DocumentsSyncTask.class)
                    .syncDocuments(results.getSuccessfulyImportedDocuments());
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        DocumentListChangeEvent.postSticky();
        showDialog(new ResultsDialogFragment.Parameters()
                .setResults(results)
                .setTitle(getString(R.string.results_dialog_import_results_title))
                .setMessage(getString(R.string.results_dialog_import_results_header)));
    }

    /**
     * An {@link EventBus} callback which receives a {@code DocumentsUpdatesPushDoneEvent} when the
     * {@link DocumentsUpdatesPushTask} has finished its work.
     *
     * @param event the {@code DocumentsUpdatesPushDoneEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(DocumentsUpdatesPushDoneEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        DocumentsUpdatesPushProcess.Results results = event.getResults();
        try {
            appContext.getTask(DocumentsSyncTask.class).syncDocuments(results.getSuccessResultsList());
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        DocumentListChangeEvent.postSticky();
        showDialog(new ResultsDialogFragment.Parameters()
                .setResults(results)
                .setTitle(getString(R.string.results_dialog_push_updates_results_title))
                .setMessage(getString(R.string.results_dialog_push_updates_results_header)));
    }

    /**
     * An {@link EventBus} callback which receives a {@code ChangesSyncDoneEvent} when the
     * {@link ChangesSyncTask} has finished its work.
     *
     * @param event the {@code ChangesSyncDoneEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(ChangesSyncDoneEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        ChangesSyncProcess.Results results = event.getResults();
        try {
            appContext.getTask(DocumentsSyncTask.class).syncDocuments(results.getSuccessResultsList());
        } catch (TaskCreationException e) {
            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
        }
        DocumentListChangeEvent.postSticky();
        if (results.showResults) {
            showDialog(new ResultsDialogFragment.Parameters()
                    .setResults(results)
                    .setTitle(getString(R.string.results_dialog_changes_sync_results_title))
                    .setMessage(getString(R.string.results_dialog_changes_sync_results_header)));
        }
    }

    /**
     * An {@link EventBus} callback which receives {@code ShowDialogEvent}s.
     *
     * <p>This method created a dialog with the parameters set in the event and displays it.
     *
     * @param event the {@code ShowDialogEvent} which triggered this callback
     */
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(ShowDialogEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        showDialog(event.getParameters());
    }
}
