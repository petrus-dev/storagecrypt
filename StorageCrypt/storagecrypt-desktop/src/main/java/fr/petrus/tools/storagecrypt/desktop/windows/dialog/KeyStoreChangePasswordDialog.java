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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to change the app keystore password.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class KeyStoreChangePasswordDialog extends CustomDialog {
    private String currentPassword = null;
    private String newPassword = null;
    private String newPasswordConfirmation = null;

    /**
     * Creates a new {@code KeyStoreChangePasswordDialog} instance.
     *
     * @param appWindow the application window
     */
    public KeyStoreChangePasswordDialog(AppWindow appWindow) {
        super(appWindow);
        setClosable(true);
        setResizable(false);
        setTitle(textBundle.getString("change_keystore_password_dialog_title"));
        setPositiveButtonText(textBundle.getString("change_keystore_password_dialog_validate_button_text"));
        setNegativeButtonText(textBundle.getString("change_keystore_password_dialog_cancel_button_text"));
    }

    /**
     * Returns the current password.
     *
     * @return the current password
     */
    public String getCurrentPassword() {
        return currentPassword;
    }

    /**
     * Returns the new password.
     *
     * @return the new password
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * Returns the new password confirmation.
     *
     * @return the new password confirmation
     */
    public String getNewPasswordConfirmation() {
        return newPasswordConfirmation;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Label titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(textBundle.getString("change_keystore_password_dialog_message"));
        applyGridData(titleLabel).horizontalSpan(2).withHorizontalFill();

        Label currentPasswordSubTitleLabel = new Label(parent, SWT.NULL);
        currentPasswordSubTitleLabel.setText(textBundle.getString("change_keystore_password_dialog_enter_current_keystore_password_text"));
        applyGridData(currentPasswordSubTitleLabel).withHorizontalFill().horizontalSpan(2);

        Label currentPasswordLabel = new Label(parent, SWT.NONE);
        currentPasswordLabel.setText(textBundle.getString("change_keystore_password_dialog_current_keystore_password_text"));
        applyGridData(currentPasswordLabel).withHorizontalFill();

        final Text currentPasswordText = new Text(parent, SWT.PASSWORD | SWT.BORDER);
        currentPasswordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                currentPassword = currentPasswordText.getText();
                checkInputValidity();
            }
        });
        applyGridData(currentPasswordText).withHorizontalFill();

        Label newPasswordSubTitleLabel = new Label(parent, SWT.NULL);
        newPasswordSubTitleLabel.setText(textBundle.getString("change_keystore_password_dialog_enter_and_confirm_new_keystore_password_text"));
        applyGridData(newPasswordSubTitleLabel).withHorizontalFill().horizontalSpan(2);

        Label newPasswordLabel = new Label(parent, SWT.NONE);
        newPasswordLabel.setText(textBundle.getString("change_keystore_password_dialog_new_keystore_password_text"));
        applyGridData(newPasswordLabel).withHorizontalFill();

        final Text newPasswordText = new Text(parent, SWT.PASSWORD | SWT.BORDER);
        newPasswordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                newPassword = newPasswordText.getText();
                checkInputValidity();
            }
        });
        applyGridData(newPasswordText).withHorizontalFill();

        Label newPasswordConfirmationLabel = new Label(parent, SWT.NONE);
        newPasswordConfirmationLabel.setText(textBundle.getString("change_keystore_password_dialog_new_keystore_password_confirmation_text"));
        applyGridData(newPasswordConfirmationLabel).withHorizontalFill();

        final Text newPasswordConfirmationText = new Text(parent, SWT.PASSWORD | SWT.BORDER);
        newPasswordConfirmationText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                newPasswordConfirmation = newPasswordConfirmationText.getText();
                checkInputValidity();
            }
        });
        applyGridData(newPasswordConfirmationText).withHorizontalFill();

        validateOnReturnPressed(currentPasswordText, newPasswordText, newPasswordConfirmationText);
    }

    @Override
    protected boolean isInputValid() {
        return null!=currentPassword && !currentPassword.isEmpty()
                && null!=newPassword && !newPassword.isEmpty() && newPassword.equals(newPasswordConfirmation);
    }
}
