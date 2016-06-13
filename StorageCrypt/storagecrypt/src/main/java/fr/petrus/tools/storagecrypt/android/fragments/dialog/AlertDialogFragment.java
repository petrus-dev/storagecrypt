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

import fr.petrus.tools.storagecrypt.R;

/**
 * This dialog displays an alert to the user.
 *
 * @author Pierre Sagne
 * @since 11.04.2015
 */
public class AlertDialogFragment extends CustomDialogFragment<AlertDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "AlertDialogFragment";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private int dialogId = -1;
        private String title = null;
        private String message = null;

        /**
         * Creates a new empty {@code Parameters} instance.
         */
        public Parameters() {}

        /**
         * Sets the ID of the dialog to create.
         *
         * @param dialogId the ID of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setDialogId(int dialogId) {
            this.dialogId = dialogId;
            return this;
        }

        /**
         * Sets the title of the dialog to create.
         *
         * @param title the title of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the message to display in the dialog.
         *
         * @param message the message to display in the dialog
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Returns the ID of the dialog to create.
         *
         * @return the ID of the dialog to create
         */
        public int getDialogId() {
            return dialogId;
        }

        /**
         * Returns the title of the dialog to create.
         *
         * @return the title of the dialog to create
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the message to display in the dialog.
         *
         * @return the message to display in the dialog
         */
        public String getMessage() {
            return message;
        }
    }

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener {
        /**
         * The method called when a dialog is closed.
         */
        void onAlertDialogDone(int dialogId);
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
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        if (null!=parameters) {
            if (null != parameters.getTitle()) {
                dialogBuilder.setTitle(parameters.getTitle());
            }
            if (null != parameters.getMessage()) {
                dialogBuilder.setMessage(parameters.getMessage());
            }
            dialogBuilder.setNeutralButton(
                    getActivity().getString(R.string.alert_dialog_fragment_ok_button_text),
                    new AlertDialog.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int which) {
                            dialogListener.onAlertDialogDone(parameters.getDialogId());
                        }
                    });
        }
        AlertDialog dialog = dialogBuilder.create();
        return dialog;
    }

    /**
     * Creates an {@code AlertDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code AlertDialogFragment} to
     * @param parameters the parameters to create the {@code AlertDialogFragment}
     * @return the newly created {@code AlertDialogFragment}
     */
    public static AlertDialogFragment showFragment(FragmentManager fragmentManager, Parameters parameters) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}