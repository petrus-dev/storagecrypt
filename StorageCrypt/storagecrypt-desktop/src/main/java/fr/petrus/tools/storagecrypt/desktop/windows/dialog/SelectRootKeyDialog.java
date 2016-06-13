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

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to let the user change the default key of a "top level" folder.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class SelectRootKeyDialog extends CustomDialog<SelectRootKeyDialog> {
    private EncryptedDocument encryptedDocument;

    private String keyAlias = null;

    /**
     * Creates a new {@code SelectRootKeyDialog} instance for the given {@code encryptedDocument}.
     *
     * @param appWindow         the application window
     * @param encryptedDocument the "top level" folder to choose the default key for
     */
    public SelectRootKeyDialog(AppWindow appWindow,
                               EncryptedDocument encryptedDocument) {
        super(appWindow);
        setClosable(true);
        setResizable(false);
        setTitle(textBundle.getString("select_key_dialog_title"));
        setPositiveButtonText(textBundle.getString("select_key_dialog_select_button_text"));
        setNegativeButtonText(textBundle.getString("select_key_dialog_cancel_button_text"));
        this.encryptedDocument = encryptedDocument;
    }

    /**
     * Returns the alias of the chosen key.
     *
     * @return the alias of the chosen key
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Label keyAliasLabel = new Label(parent, SWT.NONE);
        keyAliasLabel.setText(textBundle.getString("select_key_dialog_select_key_text",
                encryptedDocument.storageText()));
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
            if (encryptedDocument.getKeyAlias().equals(keyAlias)) {
                keyAliasCombo.select(i);
            }
            i++;
        }
        keyAlias = keyAliasCombo.getText();

        validateOnReturnPressed(keyAliasCombo);
    }

    @Override
    protected boolean isInputValid() {
        return null!=keyAlias && !keyAlias.isEmpty();
    }
}
