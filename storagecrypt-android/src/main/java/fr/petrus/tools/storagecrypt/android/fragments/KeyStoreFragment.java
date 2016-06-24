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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.android.activity.GetKeyAliasesListener;
import fr.petrus.tools.storagecrypt.android.activity.IsUnlockedListener;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.ShowDialogListener;
import fr.petrus.tools.storagecrypt.android.activity.ShowHelpListener;
import fr.petrus.tools.storagecrypt.android.adapters.KeyArrayAdapter;
import fr.petrus.tools.storagecrypt.android.events.KeyListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.KeyStoreStateChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.UnlockKeyStoreEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ConfirmationDialogFragment;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.TextInputDialogFragment;

/**
 * This fragment shows the keys contained in the application key store and lets the user manage them.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class KeyStoreFragment extends Fragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    public static final String TAG = "KeyStoreFragment";

    private FragmentListener fragmentListener;

    /**
     * The interface used by this fragment to communicate with the Activity.
     */
    public interface FragmentListener
            extends IsUnlockedListener, GetKeyAliasesListener, ShowDialogListener, ShowHelpListener {

        /**
         * Lock the application key store.
         */
        void onKeyStoreLock();

        /**
         * Returns the documents encrypted with the key known by the given {@code keyAlias}.
         *
         * @return a list containing the documents encrypted with the key known by the given
         *         {@code keyAlias}
         */
        List<EncryptedDocument> getDocumentsWithKeyAlias(String keyAlias)
                throws DatabaseConnectionClosedException;

        /**
         * Shows the dialog which lets the user change the application key store password.
         */
        void onKeyStoreChangePassword();

        /**
         * Shows the dialog which lets the user choose the name of a new key to be generated into
         * the application key store.
         */
        void onKeyStoreCreateKey();

        /**
         * Shows the dialog which lets the user choose and optionally rename the keys to export from
         * the application key store.
         */
        void onKeyStoreExportKeys();

        /**
         * Shows the dialog which lets the user choose and optionally rename the keys to import into
         * the application key store.
         */
        void onKeyStoreImportKeys();
    }

    private LinearLayout lockedLayout;
    private RelativeLayout unlockedLayout;
    private ListView keysListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_unlocked_keystore_fragment, menu);
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentListener) {
            fragmentListener = (FragmentListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement "+FragmentListener.class.getName());
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
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_keystore, container, false);

        Button keyStoreLockButton = (Button) view.findViewById(R.id.keystore_lock_button);
        keyStoreLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentListener.onKeyStoreLock();
            }
        });

        lockedLayout = (LinearLayout) view.findViewById(R.id.locked_layout);
        unlockedLayout = (RelativeLayout) view.findViewById(R.id.unlocked_layout);

        keysListView = (ListView) view.findViewById(R.id.keys_selection_list_view);
        List<String> keyAliases = fragmentListener.getKeyAliases();
        KeyArrayAdapter keyArrayAdapter = new KeyArrayAdapter(getActivity(), keyAliases);
        keysListView.setAdapter(keyArrayAdapter);

        registerForContextMenu(keysListView);

        selectLayout();

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.action_change_keystore_password:
                fragmentListener.onKeyStoreChangePassword();
                return true;
            case R.id.action_export_keys:
                fragmentListener.onKeyStoreExportKeys();
                return true;
            case R.id.action_import_keys:
                fragmentListener.onKeyStoreImportKeys();
                return true;
            case R.id.action_generate_key:
                fragmentListener.showDialog(new TextInputDialogFragment.Parameters()
                        .setDialogId(AndroidConstants.MAIN_ACTIVITY.NEW_KEY_ALIAS_TEXT_INPUT_DIALOG)
                        .setTitle(getString(R.string.new_key_dialog_fragment_title))
                        .setPromptText(getString(R.string.new_key_dialog_fragment_prompt))
                        .setAllowedCharacters(Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS)
                        .setPositiveChoiceText(getString(R.string.new_key_dialog_fragment_generate_button_text))
                        .setNegativeChoiceText(getString(R.string.new_key_dialog_fragment_cancel_button_text)));

                return true;
            case R.id.action_help:
                fragmentListener.showHelp("keys_and_keystore_management_title");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.keys_selection_list_view) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_context_key, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String alias = KeyArrayAdapter.getListItemAt(keysListView, info.position);
        switch(item.getItemId()) {
            case R.id.delete_key:
                try {
                    List<EncryptedDocument> documentsList = fragmentListener.getDocumentsWithKeyAlias(alias);
                    if (documentsList.isEmpty()) {
                        fragmentListener.showDialog(new ConfirmationDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.KEY_SUPPRESSION_DIALOG)
                                .setTitle(getString(R.string.delete_key_dialog_fragment_title))
                                .setMessage(getString(R.string.delete_key_dialog_fragment_confirmation_message, alias))
                                .setPositiveChoiceText(getString(R.string.delete_key_dialog_fragment_delete_button_text))
                                .setNegativeChoiceText(getString(R.string.delete_key_dialog_fragment_cancel_button_text))
                                .setParameter(alias));
                    } else {
                        fragmentListener.showDialog(new ConfirmationDialogFragment.Parameters()
                                .setDialogId(AndroidConstants.MAIN_ACTIVITY.KEY_SUPPRESSION_DIALOG)
                                .setTitle(getString(R.string.delete_key_dialog_fragment_title))
                                .setMessage(getString(R.string.delete_key_dialog_fragment_confirmation_message_referenced_documents,
                                        alias, documentsList.size()))
                                .setPositiveChoiceText(getString(R.string.delete_key_dialog_fragment_delete_button_text))
                                .setNegativeChoiceText(getString(R.string.delete_key_dialog_fragment_cancel_button_text))
                                .setParameter(alias));
                    }
                } catch (DatabaseConnectionClosedException e) {
                    Log.e(TAG, "Database is closed", e);
                }
                return true;
            case R.id.rename_key: {
                fragmentListener.showDialog(new TextInputDialogFragment.Parameters()
                        .setDialogId(AndroidConstants.MAIN_ACTIVITY.RENAME_KEY_ALIAS_TEXT_INPUT_DIALOG)
                        .setTitle(getString(R.string.rename_key_dialog_fragment_title))
                        .setPromptText(getString(R.string.rename_key_dialog_fragment_prompt))
                        .setDefaultValue(alias)
                        .setAllowedCharacters(Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS)
                        .setPositiveChoiceText(getString(R.string.rename_key_dialog_fragment_delete_button_text))
                        .setNegativeChoiceText(getString(R.string.rename_key_dialog_fragment_cancel_button_text))
                        .setParameter(alias));
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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

    /**
     * An {@link EventBus} callback which receives {@code KeyListChangeEvent}s.
     *
     * <p>This method updates the keys list when a change is notified.
     *
     * @param event the {@code KeyListChangeEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(KeyListChangeEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<String> keyAliases = fragmentListener.getKeyAliases();
                KeyArrayAdapter keyArrayAdapter = new KeyArrayAdapter(getActivity(), keyAliases);
                keysListView.setAdapter(keyArrayAdapter);
            }
        });
    }

    /**
     * An {@link EventBus} callback which receives {@code KeyStoreStateChangeEvent}s.
     *
     * <p>This method selects the layout to display, depending whether the key store is unlocked.
     * If it is unlocked, it displays the list of keys in the key store. If it is locked,
     * it simply displays a message saying that the application is locked.
     *
     * @param event the {@code KeyStoreStateChangeEvent} which triggered this callback
     */
    @Subscribe(sticky = true)
    public void onEvent(KeyStoreStateChangeEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectLayout();
            }
        });
        if (!fragmentListener.isUnlocked()) {
            UnlockKeyStoreEvent.postSticky();
        }
    }
}
