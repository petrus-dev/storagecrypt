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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import fr.petrus.lib.core.StorageType;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to let the user choose the data for a new remote storage.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class CreateRootDialog extends CustomDialog<CreateRootDialog> {

    private StorageType storageType = null;
    private String keyAlias = null;

    /**
     * Creates a new {@code CreateRootDialog} instance.
     *
     * @param appWindow the application window
     */
    public CreateRootDialog(AppWindow appWindow) {
        super(appWindow);
        setClosable(true);
        setResizable(false);
        setTitle(textBundle.getString("add_storage_dialog_title"));
        setPositiveButtonText(textBundle.getString("add_storage_dialog_link_button_text"));
        setNegativeButtonText(textBundle.getString("add_storage_dialog_cancel_button_text"));
    }

    /**
     * Returns the storage type of the remote storage.
     *
     * @return the storage type of the remote storage
     */
    public StorageType getStorageType() {
        return storageType;
    }

    /**
     * Returns the alias of the default key used to encrypt the new remote storage.
     *
     * @return the alias of the default key used to encrypt the new remote storage
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Label titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(textBundle.getString("add_storage_dialog_title"));
        applyGridData(titleLabel).horizontalSpan(2).withHorizontalFill();

        Label storageTypeLabel = new Label(parent, SWT.NONE);
        storageTypeLabel.setText(textBundle.getString("add_storage_dialog_storage_type_prompt"));
        applyGridData(storageTypeLabel).withHorizontalFill();

        final Combo storageTypeCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
        storageTypeCombo.add(textBundle.getString("storage_type_description_gdrive"));
        storageTypeCombo.add(textBundle.getString("storage_type_description_dropbox"));
        storageTypeCombo.add(textBundle.getString("storage_type_description_box"));
        storageTypeCombo.add(textBundle.getString("storage_type_description_hubic"));
        storageTypeCombo.add(textBundle.getString("storage_type_description_onedrive"));
        storageTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                switch(storageTypeCombo.getSelectionIndex()) {
                    case 0:
                        storageType = StorageType.GoogleDrive;
                        break;
                    case 1:
                        storageType = StorageType.Dropbox;
                        break;
                    case 2:
                        storageType = StorageType.Box;
                        break;
                    case 3:
                        storageType = StorageType.HubiC;
                        break;
                    case 4:
                        storageType = StorageType.OneDrive;
                        break;
                }
                checkInputValidity();
            }
        });
        applyGridData(storageTypeCombo).withHorizontalFill();

        Label keyAliasLabel = new Label(parent, SWT.NONE);
        keyAliasLabel.setText(textBundle.getString("add_storage_dialog_choose_storage_key_alias_text"));
        applyGridData(keyAliasLabel).withHorizontalFill();

        final Combo keyAliasCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
        keyAliasCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                keyAlias = keyAliasCombo.getText();
                checkInputValidity();
            }
        });
        for (String keyAlias: appWindow.getKeyStoreKeyAliases()) {
            keyAliasCombo.add(keyAlias);
        }
        keyAliasCombo.select(0);
        applyGridData(keyAliasCombo).withHorizontalFill();
        keyAlias = keyAliasCombo.getText();

        validateOnReturnPressed(storageTypeCombo, keyAliasCombo);
    }

    @Override
    protected boolean isInputValid() {
        return null!=storageType && null!=keyAlias && !keyAlias.isEmpty();
    }
}
