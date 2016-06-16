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
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import fr.petrus.lib.core.StorageType;
import fr.petrus.tools.storagecrypt.android.activity.GetKeyAliasesListener;
import fr.petrus.tools.storagecrypt.R;

/**
 * This dialog lets the user choose the type of storage and the default key to encrypt documents in
 * this storage.
 *
 * <p>This is the first step, before the logon page of the account.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class AddStorageDialogFragment extends CustomDialogFragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "AddStorageDialogFragment";

    private DialogListener dialogListener = null;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener extends GetKeyAliasesListener {

        /**
         * Shows the account logon page for the given {@code storageType}.
         *
         * @param storageType the storage type to show the logon page of
         * @param keyAlias    the alias of the default key used to encrypt documents on this
         *                    remote storage.
         */
        void onAddStorage(StorageType storageType, String keyAlias);
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
        View view = layoutInflater.inflate(R.layout.fragment_add_storage, null);

        final Spinner keyAliasSpinner = (Spinner) view.findViewById(R.id.key_alias_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, dialogListener.getKeyAliases());

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyAliasSpinner.setAdapter(adapter);
        keyAliasSpinner.setSelection(0);

        final Spinner storageTypeSpinner = (Spinner) view.findViewById(R.id.storage_type_spinner);
        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.add_storage_fragment_title))
                .setPositiveButton(getActivity().getString(R.string.add_storage_fragment_link_button_text),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String keyAlias = keyAliasSpinner.getSelectedItem().toString();
                                int storageTypeItemPos = storageTypeSpinner.getSelectedItemPosition();
                                if (storageTypeItemPos >= 0) {
                                    String storageType = getResources().getStringArray(R.array.storage_types)[storageTypeItemPos];
                                    dialogListener.onAddStorage(StorageType.valueOf(storageType), keyAlias);
                                }
                            }
                        }
                )
                .setNegativeButton(getActivity().getString(R.string.add_storage_fragment_cancel_button_text), null);
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    /**
     * Creates an {@code AddStorageDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code AddStorageDialogFragment} to
     * @return the newly created {@code AddStorageDialogFragment}
     */
    public static AddStorageDialogFragment showFragment(FragmentManager fragmentManager) {
        AddStorageDialogFragment fragment = new AddStorageDialogFragment();
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}
