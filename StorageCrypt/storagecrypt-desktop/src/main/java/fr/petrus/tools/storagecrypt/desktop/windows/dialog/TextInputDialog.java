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

package fr.petrus.tools.storagecrypt.desktop.windows.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to let the user type some text, with an optional confirmation
 * <p/>
 * <p>This dialog can also be configured for password hidden input.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class TextInputDialog extends CustomDialog<TextInputDialog> {

    private String promptText = null;
    private String confirmationPromptText = null;
    private boolean isPassword = false;
    private String initialValue = null;
    private String allowedCharacters = null;

    private String text = null;
    private String confirmation = null;

    /**
     * Creates a new {@code TextInputDialog} instance.
     *
     * @param appWindow the application window
     */
    public TextInputDialog(AppWindow appWindow) {
        super(appWindow);
        setClosable(true);
        setResizable(false);
    }

    /**
     * Sets the prompt text for the input field.
     *
     * @param promptText the prompt text for the input field
     * @return this {@code TextInputDialog} for further configuration
     */
    public TextInputDialog setPromptText(String promptText) {
        this.promptText = promptText;
        return this;
    }

    /**
     * Sets the prompt text for the confirmation input field.
     *
     * <p>If this method is not called before {@link TextInputDialog#open}, no confirmation input field
     * will be shown.
     *
     * @param confirmationPromptText the prompt text for the confirmation input field
     * @return this {@code TextInputDialog} for further configuration
     */
    public TextInputDialog setConfirmationPromptText(String confirmationPromptText) {
        this.confirmationPromptText = confirmationPromptText;
        return this;
    }

    /**
     * Sets whether this dialog should hide user input, for passwords.
     *
     * @param isPassword if true, this dialog will hide user input, for passwords
     * @return this {@code TextInputDialog} for further configuration
     */
    public TextInputDialog setPassword(boolean isPassword) {
        this.isPassword = isPassword;
        return this;
    }

    /**
     * Sets the initial value of the input field.
     *
     * @param initialValue the initial value of the input field
     * @return this {@code TextInputDialog} for further configuration
     */
    public TextInputDialog setInitialValue(String initialValue) {
        this.initialValue = initialValue;
        return this;
    }

    /**
     * Sets the allowed characters for the user input.
     *
     * @param allowedCharacters the allowed characters for the user input
     * @return this {@code TextInputDialog} for further configuration
     */
    public TextInputDialog setAllowedCharacters(String allowedCharacters) {
        this.allowedCharacters = allowedCharacters;
        return this;
    }

    /**
     * Returns the value entered by the user.
     *
     * @return the value entered by the user
     */
    public String getInputValue() {
        return text;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Label promptLabel = new Label(parent, SWT.NONE);
        promptLabel.setText(promptText);
        applyGridData(promptLabel).withHorizontalFill();

        int resultTextFlags = SWT.BORDER;
        if (isPassword) {
            resultTextFlags |= SWT.PASSWORD;
        }

        final Text resultText = new Text(parent, resultTextFlags);
        if (null!= initialValue && !initialValue.isEmpty()) {
            resultText.setText(initialValue);
        }
        resultText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                text = resultText.getText();
                checkInputValidity();
            }
        });
        applyGridData(resultText).withHorizontalFill();

        VerifyListener verifyListener = null;
        if (null!=allowedCharacters) {
            verifyListener = new VerifyListener() {
                @Override
                public void verifyText(VerifyEvent verifyEvent) {
                    for (int i = 0; i<verifyEvent.text.length(); i++) {
                        if (!allowedCharacters.contains(String.valueOf(verifyEvent.text.charAt(i)))) {
                            verifyEvent.doit = false;
                            return;
                        }
                    }
                }
            };
        }

        if (null!=verifyListener) {
            resultText.addVerifyListener(verifyListener);
        }

        if (!hasConfirmation()) {
            //Single Text input
            validateOnReturnPressed(resultText);
        } else {
            // Double text input (with confirmation)

            Label confirmationPromptLabel = new Label(parent, SWT.NONE);
            confirmationPromptLabel.setText(confirmationPromptText);
            applyGridData(confirmationPromptLabel).withHorizontalFill();

            final Text confirmationText = new Text(parent, resultTextFlags);
            confirmationText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent modifyEvent) {
                    confirmation = confirmationText.getText();
                    checkInputValidity();
                }
            });
            applyGridData(confirmationText).withHorizontalFill();

            if (null!=verifyListener) {
                confirmationText.addVerifyListener(verifyListener);
            }

            validateOnReturnPressed(resultText, confirmationText);
        }
    }

    @Override
    protected boolean isInputValid() {
        if (hasConfirmation()) {
            return null!=text && !text.isEmpty() && text.equals(confirmation);
        } else {
            return true;
        }
    }

    private boolean hasConfirmation() {
        return null!=confirmationPromptText;
    }
}
