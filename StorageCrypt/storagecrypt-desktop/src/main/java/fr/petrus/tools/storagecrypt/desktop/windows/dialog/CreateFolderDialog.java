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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to let the user choose the data for a new folder.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class CreateFolderDialog extends CustomDialog<CreateFolderDialog> {
    private EncryptedDocument parentEncryptedDocument = null;

    private String folderName = null;
    private String keyAlias = null;

    /**
     * Creates a new {@code CreateFolderDialog} to create a new document as a child of the given
     * {@code parentEncryptedDocument}.
     *
     * @param appWindow               the application window
     * @param parentEncryptedDocument the parent folder where to create the new document
     */
    public CreateFolderDialog(AppWindow appWindow,
                              EncryptedDocument parentEncryptedDocument) {
        super(appWindow);
        setClosable(true);
        setResizable(false);
        setTitle(textBundle.getString("create_folder_dialog_title"));
        setPositiveButtonText(textBundle.getString("create_folder_dialog_create_button_text"));
        setNegativeButtonText(textBundle.getString("create_folder_dialog_cancel_button_text"));
        this.parentEncryptedDocument = parentEncryptedDocument;
    }

    /**
     * Returns the folder name.
     *
     * @return the folder name
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * Returns the alias of the key used to encrypt the new folder.
     *
     * @return the alias of the key used to encrypt the new folder
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Label nameLabel = new Label(parent, SWT.NONE);
        nameLabel.setText(textBundle.getString("create_folder_dialog_folder_name_prompt"));
        applyGridData(nameLabel).withHorizontalFill();

        final Text nameText = new Text(parent, SWT.BORDER);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                folderName = nameText.getText();
                checkInputValidity();
            }
        });
        applyGridData(nameText).withHorizontalFill();

        Label keyAliasLabel = new Label(parent, SWT.NONE);
        keyAliasLabel.setText(textBundle.getString("create_folder_dialog_choose_encryption_key_alias_text"));
        applyGridData(keyAliasLabel).withHorizontalFill();

        final Combo keyAliasCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
        applyGridData(keyAliasCombo).withHorizontalFill();
        keyAliasCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                keyAlias = keyAliasCombo.getText();
                checkInputValidity();
            }
        });
        int i = 0;
        for (String keyAlias: appWindow.getKeyStoreKeyAliases()) {
            keyAliasCombo.add(keyAlias);
            if (parentEncryptedDocument.getKeyAlias().equals(keyAlias)) {
                keyAliasCombo.select(i);
            }
            i++;
        }
        keyAlias = keyAliasCombo.getText();

        validateOnReturnPressed(nameText, keyAliasCombo);
    }

    @Override
    protected boolean isInputValid() {
        return null!=folderName && !folderName.isEmpty() && null!=keyAlias && !keyAlias.isEmpty();
    }
}
