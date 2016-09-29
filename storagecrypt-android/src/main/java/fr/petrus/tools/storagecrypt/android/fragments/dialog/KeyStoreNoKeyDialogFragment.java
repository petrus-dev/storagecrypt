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

package fr.petrus.tools.storagecrypt.android.fragments.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.android.activity.GetKeyAliasesListener;
import fr.petrus.tools.storagecrypt.android.adapters.KeyArrayAdapter;
import fr.petrus.tools.storagecrypt.android.events.KeyListChangeEvent;

/**
 * The dialog used to create the initial key(s) in an empty key store.
 *
 * @author Pierre Sagne
 * @since 24.06.2015
 */
public class KeyStoreNoKeyDialogFragment extends CustomDialogFragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "KeyStoreNoKeyDialogFragment";

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener extends GetKeyAliasesListener {

        /**
         * Shows the dialog which lets the user choose the name of a new key to be generated into
         * the application key store.
         */
        void onKeyStoreCreateKey();

        /**
         * Shows the dialog which lets the user choose and optionally rename the keys to import into
         * the application key store.
         */
        void onKeyStoreImportKeys();

        /**
         * Unlocks the database after the first key is added into the keystore
         */
        void onKeyStoreFirstKeyCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DialogListener) {
            dialogListener = (DialogListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement "+DialogListener.class.getName());
        }
    }

    @Override
    public void onDetach() {
        dialogListener = null;
        super.onDetach();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_keystore_no_key, null);

        Button createKeyButton = (Button) view.findViewById(R.id.keystore_create_key_button);
        createKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogListener.onKeyStoreCreateKey();
            }
        });

        Button importKeysButton = (Button) view.findViewById(R.id.keystore_import_keys_button);
        importKeysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogListener.onKeyStoreImportKeys();
            }
        });

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.empty_keystore_fragment_title));
        dialogBuilder.setView(view);
        Dialog dialog = dialogBuilder.create();
        setCancelable(false);
        return dialog;
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
                if (!dialogListener.getKeyAliases().isEmpty()) {
                    dialogListener.onKeyStoreFirstKeyCreated();
                    dismiss();
                }
            }
        });
    }

    /**
     * Displays a {@code KeyStoreNoKeyDialogFragment}.
     *
     * @param fragmentManager the fragment manager to add the {@code KeyStoreNoKeyDialogFragment} to
     * @return the found or created {@code KeyStoreNoKeyDialogFragment}
     */
    public static KeyStoreNoKeyDialogFragment showFragment(FragmentManager fragmentManager) {
        KeyStoreNoKeyDialogFragment fragment = getFragment(fragmentManager);
        if (null==fragment) {
            fragment = new KeyStoreNoKeyDialogFragment();
            fragment.show(fragmentManager, TAG);
        }
        return fragment;
    }

    /**
     * Returns an existing {@code KeyStoreNoKeyDialogFragment} or create one if needed.
     *
     * @param fragmentManager the fragment manager to get the {@code KeyStoreNoKeyDialogFragment} from
     * @return the found or created {@code KeyStoreNoKeyDialogFragment}
     */
    public static KeyStoreNoKeyDialogFragment getFragment(FragmentManager fragmentManager) {
        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (null==fragment) {
            return null;
        }
        return (KeyStoreNoKeyDialogFragment) fragment;
    }
}
