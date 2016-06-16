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

package fr.petrus.tools.storagecrypt.desktop.windows;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.String;

import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The window which lets the user manage the encryption keys
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class KeyStoreManageKeysWindow extends ApplicationWindow {
    private static Logger LOG = LoggerFactory.getLogger(KeyStoreManageKeysWindow.class);

    private AppWindow appWindow = null;
    private TextBundle textBundle = null;
    private ListViewer listViewer = null;

    /**
     * Creates a new {@code KeyStoreManageKeysWindow} instance.
     *
     * @param appWindow   the application window
     */
    public KeyStoreManageKeysWindow(AppWindow appWindow) {
        super(appWindow.getShell());
        this.appWindow = appWindow;
        textBundle = appWindow.getTextBundle();
        addMenuBar();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(textBundle.getString("unlocked_keystore_dialog_title"));
    }

    @Override
    protected Control createContents(final Composite parent) {
        Composite contents = new Composite(parent, SWT.NONE);
        applyGridLayout(contents);

        Label existingKeysLabel = new Label(contents, SWT.NULL);
        existingKeysLabel.setText(textBundle.getString("unlocked_keystore_dialog_existing_keys_text"));
        applyGridData(existingKeysLabel).withHorizontalFill();

        listViewer = new ListViewer(contents, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        applyGridData(listViewer.getList()).withFill();
        listViewer.setContentProvider(ArrayContentProvider.getInstance());
        listViewer.setInput(appWindow.getKeyStoreKeyAliases());

        registerContextMenu(listViewer);

        getShell().pack();

        return contents;
    }

    private void registerContextMenu(final ListViewer listViewer) {
        final MenuManager contextMenuManager = new MenuManager();
        Menu menu = contextMenuManager.createContextMenu(listViewer.getControl());
        contextMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                final String keyAlias = getFirstSelected();
                if (null!=keyAlias) {
                    Action keyDeleteAction = new Action(textBundle.getString("key_context_menu_delete")) {
                        @Override
                        public void run() {
                            try {
                                if (appWindow.deleteKeyStoreKey(keyAlias)) {
                                    listViewer.setInput(appWindow.getKeyStoreKeyAliases());
                                }
                            } catch (DatabaseConnectionClosedException e) {
                                LOG.error("Failed to decrypt file", e);
                            }
                        }
                    };
                    contextMenuManager.add(keyDeleteAction);
                    Action keyRenameAction = new Action(textBundle.getString("key_context_menu_rename")) {
                        @Override
                        public void run() {
                            try {
                                if (appWindow.renameKeyStoreKey(keyAlias)) {
                                    listViewer.setInput(appWindow.getKeyStoreKeyAliases());
                                }
                            } catch (DatabaseConnectionClosedException e) {
                                LOG.error("Failed to decrypt file", e);
                            }
                        }
                    };
                    contextMenuManager.add(keyRenameAction);
                }
            }
        });

        contextMenuManager.setRemoveAllWhenShown(true);
        listViewer.getControl().setMenu(menu);
    }

    private String getFirstSelected() {
        IStructuredSelection selection = listViewer.getStructuredSelection();
        Object firstElement = selection.getFirstElement();
        if (null==firstElement) {
            return null;
        } else {
            return (String) firstElement;
        }
    }

    @Override
    protected MenuManager createMenuManager() {
        MenuManager menuManager = new MenuManager("menu");

        //KeyStore menu
        MenuManager keyStoreMenuManager = new MenuManager(textBundle.getString("menu_keystore"));
        menuManager.add(keyStoreMenuManager);

        keyStoreMenuManager.add(new Action(textBundle.getString("menu_change_keystore_password")) {
            @Override
            public void run() {
                appWindow.changeKeyStorePassword();
            }
        });

        keyStoreMenuManager.add(new Action(textBundle.getString("menu_export_keys")) {
            @Override
            public void run() {
                appWindow.exportKeyStoreKeys();
            }
        });

        keyStoreMenuManager.add(new Action(textBundle.getString("menu_import_keys")) {
            @Override
            public void run() {
                if (appWindow.importKeyStoreKeys()) {
                    listViewer.setInput(appWindow.getKeyStoreKeyAliases());
                }
            }
        });

        //Key Menu
        MenuManager keyMenuManager = new MenuManager(textBundle.getString("menu_key"));
        menuManager.add(keyMenuManager);

        keyMenuManager.add(new Action(textBundle.getString("menu_generate_key")) {
            @Override
            public void run() {
                if (appWindow.addKeyStoreKey()) {
                    listViewer.setInput(appWindow.getKeyStoreKeyAliases());
                }
            }
        });

        return menuManager;
    }
}