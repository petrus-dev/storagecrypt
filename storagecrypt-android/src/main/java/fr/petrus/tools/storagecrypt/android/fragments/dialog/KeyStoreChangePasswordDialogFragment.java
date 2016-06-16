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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import fr.petrus.tools.storagecrypt.R;

/**
 * This dialog lets the user change the password of the application key store.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class KeyStoreChangePasswordDialogFragment extends CustomDialogFragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "KeyStoreChangePasswordDialogFragment";

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener {

        /**
         * Changes the password of the application key store
         *
         * @param currentPassword         the current password of the key store
         * @param newPassword             the new password to set on the key store
         * @param newPasswordConfirmation the confirmation of the new password
         */
        void onChangeKeyStorePassword(String currentPassword, String newPassword, String newPasswordConfirmation);
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

    private EditText currentPasswordEditText = null;
    private EditText newPasswordEditText = null;
    private EditText newPasswordConfirmationEditText = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_keystore_change_password, null);

        currentPasswordEditText = (EditText) view.findViewById(R.id.keystore_current_password_edit);
        newPasswordEditText = (EditText) view.findViewById(R.id.keystore_new_password_edit);
        newPasswordConfirmationEditText = (EditText) view.findViewById(R.id.keystore_new_password_confirmation_edit);

        currentPasswordEditText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (TextInputDialogFragment.isEditorValidated(actionId, event)) {
                            newPasswordEditText.requestFocus();
                            return true; // consume.
                        }
                        return false; // pass on to other listeners.
                    }
                });

        newPasswordEditText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (TextInputDialogFragment.isEditorValidated(actionId, event)) {
                            newPasswordConfirmationEditText.requestFocus();
                            return true; // consume.
                        }
                        return false; // pass on to other listeners.
                    }
                });

        newPasswordConfirmationEditText.setOnEditorActionListener(
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
                .setTitle(getActivity().getString(R.string.change_keystore_password_fragment_title))
                .setPositiveButton(getActivity().getString(R.string.change_keystore_password_fragment_validate_button_text),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                done();
                            }
                        }
                );
        dialogBuilder.setView(view);
        Dialog dialog = dialogBuilder.create();
        return dialog;
    }

    private void done() {
        String currentPassword = currentPasswordEditText.getText().toString();
        String newPassword = newPasswordEditText.getText().toString();
        String newPasswordConfirmation = newPasswordConfirmationEditText.getText().toString();
        dialogListener.onChangeKeyStorePassword(currentPassword,
                newPassword, newPasswordConfirmation);
        dismiss();
    }

    /**
     * Creates a {@code KeyStoreChangePasswordDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code KeyStoreChangePasswordDialogFragment} to
     * @return the newly created {@code KeyStoreChangePasswordDialogFragment}
     */
    public static KeyStoreChangePasswordDialogFragment showFragment(FragmentManager fragmentManager) {
        KeyStoreChangePasswordDialogFragment fragment = new KeyStoreChangePasswordDialogFragment();
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}
