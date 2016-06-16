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
 * The dialog used to unlock the app keystore.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class KeyStoreUnlockDialog extends CustomDialog<KeyStoreUnlockDialog> {
    private String password = null;

    /**
     * Creates a new {@code KeyStoreUnlockDialog} instance.
     *
     * @param appWindow the application window
     */
    public KeyStoreUnlockDialog(AppWindow appWindow) {
        super(appWindow);
        setClosable(false);
        setResizable(false);
        setModal(true);
        setTitle(textBundle.getString("locked_keystore_dialog_title"));
        setPositiveButtonText(textBundle.getString("locked_keystore_dialog_unlock_button_text"));
        setNegativeButtonText(textBundle.getString("locked_keystore_dialog_cancel_button_text"));
    }

    /**
     * Returns the password used to unlock the keystore.
     *
     * @return the password used to unlock the keystore
     */
    public String getPassword() {
        return password;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Label titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(textBundle.getString("locked_keystore_dialog_message"));
        applyGridData(titleLabel).withHorizontalFill().horizontalSpan(2);

        Label subTitleLabel = new Label(parent, SWT.NULL);
        subTitleLabel.setText(textBundle.getString("locked_keystore_dialog_enter_keystore_password_text"));
        applyGridData(subTitleLabel).withHorizontalFill().horizontalSpan(2);

        Label passwordLabel = new Label(parent, SWT.NONE);
        passwordLabel.setText(textBundle.getString("locked_keystore_dialog_keystore_password_text"));
        applyGridData(passwordLabel).withHorizontalFill();

        final Text passwordText = new Text(parent, SWT.PASSWORD | SWT.BORDER);
        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                password = passwordText.getText();
                checkInputValidity();
            }
        });
        applyGridData(passwordText).withHorizontalFill();

        validateOnReturnPressed(passwordText);
    }

    @Override
    protected boolean isInputValid() {
        return null!=password && !password.isEmpty();
    }
}
