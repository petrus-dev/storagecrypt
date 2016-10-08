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
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.crypto.keystore.KeyStore;

import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.ShowDialogListener;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedKey;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedKeyArrayAdapter;

/**
 * This dialog lets the user select and optionally rename the keys to to import into the application
 * key store.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class KeyStoreImportKeysDialogFragment extends CustomDialogFragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "KeyStoreImportKeysDialogFragment";

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener extends ShowDialogListener {

        /**
         * Imports the given {@code selectedKeys} from the given {@code keyStore} into
         * the application key store.
         *
         * @param keyStore the key store to read the keys from
         * @param selectedKeys the keys to import
         * @throws StorageCryptException if an error occurs when reading the keys from the key store
         */
        void onImportKeys(KeyStore keyStore, List<SelectedKey> selectedKeys)
                throws StorageCryptException;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DialogListener) {
            dialogListener = (DialogListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement " + DialogListener.class.getName());
        }
    }

    @Override
    public void onDetach() {
        dialogListener = null;
        super.onDetach();
    }

    private ListView keysSelectionListView = null;
    private KeyStore keyStore = null;

    /**
     * Sets the key store to read the keys from
     *
     * @param keyStore the key store to read the keys from
     */
    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_keystore_import_keys, null);

        keysSelectionListView = (ListView) view.findViewById(R.id.keys_selection_list_view);
        final List<SelectedKey> selectedKeys = new ArrayList<>();
        if (null!= keyStore) {
            for (String keyAlias : keyStore.getKeyAliases()) {
                selectedKeys.add(new SelectedKey(keyAlias, false, null));
            }
            keysSelectionListView.setAdapter(new SelectedKeyArrayAdapter(getActivity(), selectedKeys));
        }

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.import_keys_fragment_title));

        dialogBuilder.setPositiveButton(getString(R.string.import_keys_fragment_select_button_text), new AlertDialog.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                List<SelectedKey> selectedKeys =
                        SelectedKeyArrayAdapter.getListItems(keysSelectionListView);
                try {
                    dialogListener.onImportKeys(keyStore, selectedKeys);
                } catch (StorageCryptException e) {
                    dialogListener.showErrorDialog(e);
                }
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel_button_text), null);

        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    /**
     * Creates a {@code KeyStoreImportKeysDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code KeyStoreImportKeysDialogFragment} to
     * @return the newly created {@code KeyStoreImportKeysDialogFragment}
     */
    public static KeyStoreImportKeysDialogFragment showFragment(FragmentManager fragmentManager, KeyStore keyStore) {
        KeyStoreImportKeysDialogFragment fragment = new KeyStoreImportKeysDialogFragment();
        fragment.setKeyStore(keyStore);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}