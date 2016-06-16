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
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.petrus.tools.storagecrypt.R;

/**
 * This dialog asks the user to enter a text, with an optional confirmation.
 *
 * <p>This dialog can be configured to hide the typed text, for passwords.
 *
 * <p>An optional filter can be set to let the user only use certain characters.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class TextInputDialogFragment extends CustomDialogFragment<TextInputDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "TextInputDialogFragment";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private int dialogId = -1;
        private String title = null;
        private String promptText = null;
        private String confirmationPromptText = null;
        private String defaultValue = null;
        private String allowedCharacters = null;
        private boolean password = false;
        private boolean confirmation = false;
        private String positiveChoiceText = null;
        private String negativeChoiceText = null;
        private Object parameter = null;

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
         * Sets the prompt text for the input field of the dialog to create.
         *
         * @param promptText the prompt text for the input field of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setPromptText(String promptText) {
            this.promptText = promptText;
            return this;
        }

        /**
         * Sets the prompt text for the confirmation field of the dialog to create.
         *
         * @param confirmationPromptText the prompt text for the confirmation field of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setConfirmationPromptText(String confirmationPromptText) {
            this.confirmationPromptText = confirmationPromptText;
            return this;
        }

        /**
         * Sets the default value of the input field of the dialog to create.
         *
         * @param defaultValue the default value of the input field of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Sets the allowed characters for the user input.
         *
         * @param allowedCharacters the allowed characters for the user input
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setAllowedCharacters(String allowedCharacters) {
            this.allowedCharacters = allowedCharacters;
            return this;
        }

        /**
         * Sets whether the dialog should hide user input, for passwords.
         *
         * @param password if true, the dialog will hide user input, for passwords
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setPassword(boolean password) {
            this.password = password;
            return this;
        }

        /**
         * Sets whether the dialog should show a confirmation input field.
         *
         * @param confirmation if true, the dialog will show a confirmation input field
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setConfirmation(boolean confirmation) {
            this.confirmation = confirmation;
            return this;
        }

        /**
         * Sets the text to display in the positive choice button.
         *
         * <p>If this method is not called, the positive choice button will not be shown.
         *
         * @param positiveChoiceText the text to display in the positive choice button
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setPositiveChoiceText(String positiveChoiceText) {
            this.positiveChoiceText = positiveChoiceText;
            return this;
        }

        /**
         * Sets the text to display in the negative choice button.
         *
         * <p>If this method is not called, the negative choice button will not be shown.
         *
         * @param negativeChoiceText the text to display in the negative choice button
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setNegativeChoiceText(String negativeChoiceText) {
            this.negativeChoiceText = negativeChoiceText;
            return this;
        }

        /**
         * Sets an additional parameter which will be passed back to the Activity on close.
         *
         * @param parameter the additional parameter which will be passed back to the Activity on close
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setParameter(Object parameter) {
            this.parameter = parameter;
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
         * Returns the prompt text for the input field of the dialog to create.
         *
         * @return the prompt text for the input field of the dialog to create
         */
        public String getPromptText() {
            return promptText;
        }

        /**
         * Returns the prompt text for the confirmation field of the dialog to create.
         *
         * @return the prompt text for the confirmation field of the dialog to create
         */
        public String getConfirmationPromptText() {
            return confirmationPromptText;
        }

        /**
         * Returns the default value of the input field of the dialog to create.
         *
         * @return the default value of the input field of the dialog to create
         */
        public String getDefaultValue() {
            return defaultValue;
        }

        /**
         * Returns the allowed characters for the user input.
         *
         * @return the allowed characters for the user input
         */
        public String getAllowedCharacters() {
            return allowedCharacters;
        }

        /**
         * Returns whether the dialog should hide user input, for passwords.
         *
         * @return true if the dialog should hide user input, for passwords
         */
        public boolean isPassword() {
            return password;
        }

        /**
         * Returns whether the dialog should show a confirmation input field.
         *
         * @return true if the dialog should show a confirmation input field
         */
        public boolean hasConfirmation() {
            return confirmation;
        }

        /**
         * Returns the text to display in the positive choice button.
         *
         * @return the text to display in the positive choice button
         */
        public String getPositiveChoiceText() {
            return positiveChoiceText;
        }

        /**
         * Returns the text to display in the negative choice button.
         *
         * @return the text to display in the negative choice button
         */
        public String getNegativeChoiceText() {
            return negativeChoiceText;
        }

        /**
         * Returns the additional parameter which will be passed back to the Activity on close.
         *
         * @return the additional parameter which will be passed back to the Activity on close
         */
        public Object getParameter() {
            return parameter;
        }
    }

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener {

        /**
         * The method called when the user presses the positive button of the dialog with the given
         * {@code dialogId}.
         *
         * @param dialogId     the dialog id
         * @param text         the text the user typed
         * @param confirmation the confirmation text the user typed
         * @param parameter    the additional parameter passed at the creation of the dialog
         */
        void onTextInputDialogPositiveChoice(int dialogId, String text, String confirmation, Object parameter);

        /**
         * The method called when the user presses the negative button of the dialog with the given
         * {@code dialogId}.
         *
         * @param dialogId  the dialog id
         * @param parameter the additional parameter passed at the creation of the dialog
         */
        void onTextInputDialogNegativeChoice(int dialogId, Object parameter);
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

    private LinearLayout editTextLayout = null;
    private LinearLayout confirmationEditTextLayout = null;
    private EditText editText = null;
    private EditText confirmationEditText = null;
    private TextView editTextPrompt = null;
    private TextView confirmationEditTextPrompt = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        if (null!=parameters) {
            LayoutInflater layoutInflater = getActivity().getLayoutInflater();
            View view;
            if (parameters.isPassword()) {
                view = layoutInflater.inflate(R.layout.fragment_password_input, null);
            } else {
                view = layoutInflater.inflate(R.layout.fragment_text_input, null);
            }
            dialogBuilder.setView(view);

            editTextLayout = (LinearLayout) view.findViewById(R.id.edit_text_layout);
            confirmationEditTextLayout = (LinearLayout) view.findViewById(R.id.confirmation_edit_text_layout);

            editText = (EditText) view.findViewById(R.id.edit_text);
            confirmationEditText = (EditText) view.findViewById(R.id.confirmation_edit_text);

            editTextPrompt = (TextView) view.findViewById(R.id.edit_text_prompt);
            confirmationEditTextPrompt = (TextView) view.findViewById(R.id.confirmation_edit_text_prompt);

            if (null==parameters.getPromptText()) {
                editTextPrompt.setVisibility(View.GONE);
            } else {
                editTextPrompt.setText(parameters.getPromptText());
                editTextPrompt.setVisibility(View.VISIBLE);
            }

            if (null==parameters.getConfirmationPromptText()) {
                confirmationEditTextPrompt.setVisibility(View.GONE);
            } else {
                confirmationEditTextPrompt.setText(parameters.getConfirmationPromptText());
                confirmationEditTextPrompt.setVisibility(View.VISIBLE);
            }

            if (null!=parameters.getDefaultValue()) {
                editText.setText(parameters.getDefaultValue());
            }

            InputFilter[] inputFilters = null;
            if (null!=parameters.getAllowedCharacters()) {
                final String allowedCharacters = parameters.getAllowedCharacters();
                inputFilters = new InputFilter[1];
                inputFilters[0] = new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        if (source instanceof SpannableStringBuilder) {
                            SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
                            for (int i = end - 1; i >= start; i--) {
                                char currentChar = source.charAt(i);
                                if (!allowedCharacters.contains(String.valueOf(currentChar))) {
                                    sourceAsSpannableBuilder.delete(i, i+1);
                                }
                            }
                            return source;
                        } else {
                            StringBuilder filteredStringBuilder = new StringBuilder();
                            for (int i = start; i < end; i++) {
                                char currentChar = source.charAt(i);
                                if (allowedCharacters.contains(String.valueOf(currentChar))) {
                                    filteredStringBuilder.append(currentChar);
                                }
                            }
                            return filteredStringBuilder.toString();
                        }
                    }
                };
            }

            if (parameters.hasConfirmation()) {
                editText.setOnEditorActionListener(
                        new EditText.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                if (isEditorValidated(actionId, event)) {
                                    confirmationEditText.requestFocus();
                                    return true; // consume.
                                }
                                return false; // pass on to other listeners.
                            }
                        });
                confirmationEditText.setOnEditorActionListener(
                        new EditText.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                if (isEditorValidated(actionId, event)) {
                                    done(true);
                                    return true; // consume.
                                }
                                return false; // pass on to other listeners.
                            }
                        });
                if (null!=inputFilters) {
                    editText.setFilters(inputFilters);
                    confirmationEditText.setFilters(inputFilters);
                }
                confirmationEditTextLayout.setVisibility(View.VISIBLE);
            } else {
                editText.setOnEditorActionListener(
                        new EditText.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                if (isEditorValidated(actionId, event)) {
                                    done(true);
                                    return true; // consume.
                                }
                                return false; // pass on to other listeners.
                            }
                        });
                if (null!=inputFilters) {
                    editText.setFilters(inputFilters);
                }
                confirmationEditTextLayout.setVisibility(View.GONE);
            }

            if (null != parameters.getTitle()) {
                dialogBuilder.setTitle(parameters.getTitle());
            }
            if (null != parameters.getPositiveChoiceText()) {
                dialogBuilder.setPositiveButton(parameters.getPositiveChoiceText(), new AlertDialog.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {
                        done(true);
                    }
                });
            }
            if (null != parameters.getNegativeChoiceText()) {
                dialogBuilder.setNegativeButton(parameters.getNegativeChoiceText(), new AlertDialog.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {
                        done(false);
                    }
                });
            }
        }
        AlertDialog dialog = dialogBuilder.create();
        return dialog;
    }

    private void done(boolean positiveResult) {
        if (!positiveResult) {
            dialogListener.onTextInputDialogNegativeChoice(parameters.getDialogId(), parameters.getParameter());
        } else {
            if (parameters.hasConfirmation()) {
                String text = editText.getText().toString();
                String confirmation = confirmationEditText.getText().toString();
                dialogListener.onTextInputDialogPositiveChoice(
                        parameters.getDialogId(), text, confirmation, parameters.getParameter());
            } else {
                String text = editText.getText().toString();
                dialogListener.onTextInputDialogPositiveChoice(
                        parameters.getDialogId(), text, null, parameters.getParameter());
            }
        }
        dismiss();
    }

    /**
     * Creates a {@code TextInputDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code TextInputDialogFragment} to
     * @param parameters      the parameters to create the {@code TextInputDialogFragment}
     * @return the newly created {@code TextInputDialogFragment}
     */
    public static TextInputDialogFragment showFragment(FragmentManager fragmentManager,
                                                       Parameters parameters) {
        TextInputDialogFragment fragment = new TextInputDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }

    /**
     * A static utility method to determine if the input field has been validated with the virtual
     * keyboard RETURN button or a physical keyboard RETURN button.
     *
     * @param actionId the action ID received
     * @param event    the key press event
     * @return true if the input field has been validated with the virtual keyboard RETURN button
     *         or a physical keyboard RETURN button
     */
    public static boolean isEditorValidated(int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
            return true;
        }
        if (null!=event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN  && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (!event.isShiftPressed()) {
                    return true;
                }
            }
        }
        return false;
    }
}
