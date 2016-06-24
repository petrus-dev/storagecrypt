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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;

/**
 * The dialog used to create the initial key(s) in an empty key store.
 *
 * @author Pierre Sagne
 * @since 23.06.2016
 */
public class KeyStoreNoKeyDialog extends CustomDialog<KeyStoreNoKeyDialog> {
    /**
     * Creates a new {@code KeyStoreNoKeyDialog} instance.
     *
     * @param appWindow the application window
     */
    public KeyStoreNoKeyDialog(AppWindow appWindow) {
        super(appWindow);
        setClosable(false);
        setResizable(false);
        setTitle(textBundle.getString("empty_keystore_dialog_title"));
        setNegativeButtonText(textBundle.getString("empty_keystore_dialog_cancel_button_text"));
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Label titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(textBundle.getString("empty_keystore_dialog_message"));
        applyGridData(titleLabel).withHorizontalFill().horizontalSpan(2);

        Label createKeyLabel = new Label(parent, SWT.NONE);
        createKeyLabel.setText(textBundle.getString("empty_keystore_dialog_create_key_text"));
        applyGridData(createKeyLabel).withHorizontalFill();

        Button createKeyButton = new Button(parent, SWT.PUSH);
        createKeyButton.setText(textBundle.getString("empty_keystore_dialog_create_key_button_text"));
        createKeyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                if (appWindow.addKeyStoreKey()) {
                    returnResult(true);
                }
            }
        });
        applyGridData(createKeyButton).withHorizontalFill();

        Label importKeysLabel = new Label(parent, SWT.NONE);
        importKeysLabel.setText(textBundle.getString("empty_keystore_dialog_import_keys_text"));
        applyGridData(createKeyLabel).withHorizontalFill();

        Button importKeysButton = new Button(parent, SWT.PUSH);
        importKeysButton.setText(textBundle.getString("empty_keystore_dialog_import_keys_button_text"));
        importKeysButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                if (appWindow.importKeyStoreKeys()) {
                    returnResult(true);
                }
            }
        });
        applyGridData(importKeysButton).withHorizontalFill();
    }

    @Override
    protected boolean isInputValid() {
        return !appWindow.getKeyStoreKeyAliases().isEmpty();
    }
}
