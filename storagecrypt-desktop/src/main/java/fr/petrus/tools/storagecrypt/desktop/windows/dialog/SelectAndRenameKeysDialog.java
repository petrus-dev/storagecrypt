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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.Constants;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.components.TableColumnTextEditingSupport;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to select and rename key aliases to import or export.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class SelectAndRenameKeysDialog extends CustomDialog<SelectAndRenameKeysDialog> {
    private LinkedHashMap<String, String> renamedKeyAliases = new LinkedHashMap<>();

    private String message = null;
    private List<String> keyList = null;

    /**
     * Creates a new {@code SelectAndRenameKeysDialog} instance with the given {@code title},
     * {@code message} for the given {@code keyList}.
     *
     * @param appWindow the application window
     * @param title     the text to display in the title bar
     * @param message   the message to display at the top of the window
     * @param keyList   the list of the key aliases to select and rename
     */
    public SelectAndRenameKeysDialog(AppWindow appWindow,
                                     String title, String message, List<String> keyList) {
        super(appWindow);
        setClosable(true);
        setResizable(true);
        setTitle(title);
        setPositiveButtonText(textBundle.getString("select_and_edit_keys_dialog_ok_button_text"));
        setNegativeButtonText(textBundle.getString("select_and_edit_keys_dialog_cancel_button_text"));
        this.message = message;
        this.keyList = keyList;
    }

    /**
     * Returns the map containing the selected and optionally renamed key aliases.
     *
     * <p>The selected entries are placed into the returned map. The values are the renamed keys.
     *
     * @return the map containing the selected and optionally renamed key aliases
     */
    public Map<String, String> getRenamedKeyAliases() {
        return renamedKeyAliases;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent);

        Label titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(message);
        applyGridData(titleLabel).withHorizontalFill();

        final TableViewer tableViewer = new TableViewer(parent,
                SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
        applyGridData(tableViewer.getTable()).withFill();
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        TableViewerColumn keyAliasColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
        keyAliasColumn.getColumn().setText(textBundle.getString("select_and_edit_keys_dialog_key_alias_column_name"));
        keyAliasColumn.getColumn().setWidth(100);

        TableViewerColumn renamedKeyAliasColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
        renamedKeyAliasColumn.getColumn().setText(textBundle.getString("select_and_edit_keys_dialog_renamed_key_alias_column_name"));
        renamedKeyAliasColumn.getColumn().setWidth(100);

        keyAliasColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return (String) element;
            }
        });

        renamedKeyAliasColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                String keyAlias = (String) element;
                if (renamedKeyAliases.containsKey(keyAlias)) {
                    return renamedKeyAliases.get(keyAlias);
                } else {
                    return keyAlias;
                }
            }
        });

        renamedKeyAliasColumn.setEditingSupport(new TableColumnTextEditingSupport(tableViewer,
                Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS) {
            @Override
            protected boolean canEdit(Object element) {
                String keyAlias = (String) element;
                return renamedKeyAliases.containsKey(keyAlias);
            }

            @Override
            protected Object getValue(Object element) {
                String keyAlias = (String) element;
                if (renamedKeyAliases.containsKey(keyAlias)) {
                    return renamedKeyAliases.get(keyAlias);
                } else {
                    return keyAlias;
                }
            }

            @Override
            protected void setValue(Object element, Object userInputValue) {
                renamedKeyAliases.put((String)element, String.valueOf(userInputValue));
                viewer.update(element, null);
            }
        });

        tableViewer.getTable().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Identify the selected row
                final TableItem item = (TableItem) e.item;
                if (null!=item) {
                    if (item.getChecked()) {
                        renamedKeyAliases.put(item.getText(0), item.getText(1));
                    } else {
                        renamedKeyAliases.remove(item.getText(0));
                    }
                }
            }
        });

        tableViewer.setInput(keyList);
        keyAliasColumn.getColumn().pack();
        renamedKeyAliasColumn.getColumn().pack();
        parent.layout();
        getShell().layout();
    }
}
