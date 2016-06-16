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
import android.widget.EditText;
import android.widget.TextView;

import fr.petrus.tools.storagecrypt.R;

/**
 * This dialog lets the user choose a password for the new application key store.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class KeyStoreCreateDialogFragment extends CustomDialogFragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "KeyStoreCreateDialogFragment";

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener {

        /**
         * Creates a new application key store
         *
         * @param keyStorePassword             the password to set on the new key store
         * @param keyStorePasswordConfirmation the confirmation of the password
         */
        void onKeyStoreCreate(String keyStorePassword, String keyStorePasswordConfirmation);
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

    private EditText keyStorePasswordEditText = null;
    private EditText keyStorePasswordConfirmationEditText = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_keystore_create, null);

        keyStorePasswordEditText = (EditText) view.findViewById(R.id.keystore_password_edit);
        keyStorePasswordConfirmationEditText = (EditText) view.findViewById(R.id.keystore_password_confirmation_edit);

        keyStorePasswordEditText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (TextInputDialogFragment.isEditorValidated(actionId, event)) {
                            keyStorePasswordConfirmationEditText.requestFocus();
                            return true; // consume.
                        }
                        return false; // pass on to other listeners.
                    }
                });

        keyStorePasswordConfirmationEditText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (TextInputDialogFragment.isEditorValidated(actionId, event)) {
                            done();
                            return true; // consume.
                        }
                        return false; // pass on to other listeners.
                    }
                });

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.create_keystore_fragment_title))
                .setPositiveButton(getActivity().getString(R.string.create_keystore_fragment_create_button_text),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                done();
                            }
                        }
                );
        dialogBuilder.setView(view);
        Dialog dialog = dialogBuilder.create();
        setCancelable(false);
        return dialog;
    }

    private void done() {
        String keyStorePassword = String.valueOf(keyStorePasswordEditText.getText());
        String keyStorePasswordConfirmation = String.valueOf(keyStorePasswordConfirmationEditText.getText());
        dialogListener.onKeyStoreCreate(keyStorePassword, keyStorePasswordConfirmation);
        dismiss();
    }

    /**
     * Displays a {@code KeyStoreCreateDialogFragment}.
     *
     * @param fragmentManager the fragment manager to add the {@code KeyStoreCreateDialogFragment} to
     * @return the found or created {@code KeyStoreCreateDialogFragment}
     */
    public static KeyStoreCreateDialogFragment showFragment(FragmentManager fragmentManager) {
        KeyStoreCreateDialogFragment fragment = getFragment(fragmentManager);
        if (null==fragment) {
            fragment = new KeyStoreCreateDialogFragment();
            fragment.show(fragmentManager, TAG);
        }
        return fragment;
    }

    /**
     * Returns an existing {@code KeyStoreCreateDialogFragment} or create one if needed.
     *
     * @param fragmentManager the fragment manager to get the {@code KeyStoreCreateDialogFragment} from
     * @return the found or created {@code KeyStoreCreateDialogFragment}
     */
    public static KeyStoreCreateDialogFragment getFragment(FragmentManager fragmentManager) {
        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (null==fragment) {
            return null;
        }
        return (KeyStoreCreateDialogFragment) fragment;
    }
}
