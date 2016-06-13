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
import android.widget.EditText;
import android.widget.Spinner;

import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.tools.storagecrypt.android.activity.GetKeyAliasesListener;
import fr.petrus.tools.storagecrypt.R;

/**
 * This dialog lets the user choose the name of a new folder and the default key to encrypt documents in
 * this folder.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class CreateFolderDialogFragment extends CustomDialogFragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "CreateFolderDialogFragment";

    private static final String BUNDLE_DEFAULT_KEY_ALIAS = "default_key_alias";

    private DialogListener dialogListener = null;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener extends GetKeyAliasesListener {

        /**
         * Creates an empty document as a child of the current folder, with the given
         * {@code displayName}, the given {@code mimeType}, encrypted with the key stored with the
         * given {@code keyAlias}.
         *
         * @param displayName the name of the new document
         * @param mimeType the MIME type of the new document
         * @param keyAlias the alias of the key to encrypt the new document with
         */
        void onCreateDocument(String displayName, String mimeType, String keyAlias);
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
        List<String> keyAliases = dialogListener.getKeyAliases();
        String defaultKeyAlias = getArguments().getString(BUNDLE_DEFAULT_KEY_ALIAS, null);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_create_folder, null);

        final EditText folderNameEditText = (EditText) view.findViewById(R.id.folder_name_edit);

        final Spinner keyAliasSpinner = (Spinner) view.findViewById(R.id.key_alias_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, keyAliases);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyAliasSpinner.setAdapter(adapter);
        int defaultKeyAliasPos = 0;
        for (int i = 0; i<keyAliases.size(); i++) {
            if (keyAliases.get(i).equals(defaultKeyAlias)) {
                defaultKeyAliasPos = i;
                break;
            }
        }
        keyAliasSpinner.setSelection(defaultKeyAliasPos);

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.create_folder_fragment_title))
                .setPositiveButton(getActivity().getString(R.string.create_folder_fragment_create_button_text),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String folderName = String.valueOf(folderNameEditText.getText());
                                String keyAlias = keyAliasSpinner.getSelectedItem().toString();
                                if (null != folderName && !folderName.isEmpty()) {
                                    dialogListener.onCreateDocument(folderName,
                                            Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE, keyAlias);
                                }
                            }
                        }
                )
                .setNegativeButton(getActivity().getString(R.string.create_folder_fragment_cancel_button_text), null);
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    /**
     * Creates a {@code CreateFolderDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code CreateFolderDialogFragment} to
     * @param defaultKeyAlias the default key alias of the parent folder of the the new folder
     * @return the newly created {@code CreateFolderDialogFragment}
     */
    public static CreateFolderDialogFragment showFragment(FragmentManager fragmentManager,
                                                          String defaultKeyAlias) {
        CreateFolderDialogFragment fragment = new CreateFolderDialogFragment();
        Bundle args = new Bundle();
        args.putString(CreateFolderDialogFragment.BUNDLE_DEFAULT_KEY_ALIAS, defaultKeyAlias);
        fragment.setArguments(args);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}
