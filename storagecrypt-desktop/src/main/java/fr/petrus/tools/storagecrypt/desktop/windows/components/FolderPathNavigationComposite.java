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

package fr.petrus.tools.storagecrypt.desktop.windows.components;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.tools.storagecrypt.desktop.DesktopConstants;
import fr.petrus.tools.storagecrypt.desktop.Resources;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.onGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * A custom SWT ScrolledComposite, with buttons for each component of an EncryptedDocument path.
 *
 * @author Pierre Sagne
 * @since 29.07.2016
 */
public class FolderPathNavigationComposite extends ScrolledComposite {
    private static Logger LOG = LoggerFactory.getLogger(FolderPathNavigationComposite.class);

    private AppWindow appWindow = null;
    private MenuManager currentFolderContextMenuManager = null;
    private TextBundle textBundle = null;
    private Resources resources = null;
    private Composite foldersComposite = null;

    private Long currentFolderId = null;

    /**
     * Creates a new {@code FolderPathNavigationComposite} instance.
     *
     * @param parent                          the parent of this {@code FolderPathNavigationComposite}
     * @param appWindow                       the application window
     * @param currentFolderContextMenuManager the {@code MenuManager} to attach to the current folder
     * @param textBundle                      a {@code TextBundle} instance
     * @param resources                       a {@code Resources} instance
     */
    public FolderPathNavigationComposite(Composite parent, AppWindow appWindow,
                                         MenuManager currentFolderContextMenuManager,
                                         TextBundle textBundle, Resources resources) {
        super(parent, SWT.H_SCROLL);
        this.appWindow = appWindow;
        this.currentFolderContextMenuManager = currentFolderContextMenuManager;
        this.textBundle = textBundle;
        this.resources = resources;

        setLayout(new FillLayout());

        foldersComposite = new Composite(this, SWT.NULL);
        applyGridLayout(foldersComposite);

        setExpandHorizontal(true);
        setExpandVertical(true);
        setContent(foldersComposite);
    }

    /**
     * Updates this {@code FolderPathNavigationComposite}, displaying a "locked" status.
     */
    public void updateLocked() {
        // Remove children
        for (Control child : foldersComposite.getChildren()) {
            child.dispose();
        }

        onGridLayout(foldersComposite).numColumns(1);
        Label lockedLabel = new Label(foldersComposite, SWT.NONE);
        applyGridData(lockedLabel);
        lockedLabel.setText(textBundle.getString("locked_text"));

        currentFolderId = null;
    }

    private boolean needsUpdate(EncryptedDocument folder) {
        if (null==currentFolderId) {
            return true;
        } else if (null==folder) {
            return Constants.STORAGE.ROOT_PARENT_ID != currentFolderId;
        } else {
            return folder.getId() != currentFolderId;
        }
    }

    /**
     * Updates this {@code FolderPathNavigationComposite}, from the given {@code newFolder} path.
     *
     * @param newFolder the {@code EncryptedDocument} representing the new current folder
     */
    public void update(EncryptedDocument newFolder) {
        if (needsUpdate(newFolder)) {

            // Remove children
            for (Control child : foldersComposite.getChildren()) {
                child.dispose();
            }

            // Add the new children
            if (null == newFolder) {
                currentFolderId = Constants.STORAGE.ROOT_PARENT_ID;
                onGridLayout(foldersComposite).numColumns(1);

                Button button = new Button(foldersComposite, SWT.PUSH);
                applyGridData(button);
                button.setText(textBundle.getString("data_stores_header_text"));
            } else {
                currentFolderId = newFolder.getId();
                List<EncryptedDocument> parents = null;
                try {
                    parents = newFolder.parents();
                } catch (DatabaseConnectionClosedException e) {
                    LOG.error("The database is closed", e);
                }
                onGridLayout(foldersComposite).numColumns(parents.size() * 2 + 3);
                Button button = new Button(foldersComposite, SWT.PUSH);
                applyGridData(button);
                button.setText(textBundle.getString("data_stores_header_text"));
                button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent selectionEvent) {
                        appWindow.setCurrentFolderId(Constants.STORAGE.ROOT_PARENT_ID);
                    }
                });

                Label nextIcon = new Label(foldersComposite, SWT.NONE);
                applyGridData(nextIcon);
                nextIcon.setImage(resources.loadImage(DesktopConstants.RESOURCES.IC_NEXT));

                for (final EncryptedDocument parent : parents) {
                    button = new Button(foldersComposite, SWT.PUSH);
                    applyGridData(button);
                    if (parent.isRoot()) {
                        button.setText(parent.storageText());
                    } else {
                        button.setText(parent.getDisplayName());
                    }
                    button.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent selectionEvent) {
                            appWindow.setCurrentFolderId(parent.getId());
                        }
                    });

                    nextIcon = new Label(foldersComposite, SWT.NONE);
                    applyGridData(nextIcon);
                    nextIcon.setImage(resources.loadImage(DesktopConstants.RESOURCES.IC_NEXT));
                }

                button = new Button(foldersComposite, SWT.PUSH);
                applyGridData(button);
                if (newFolder.isRoot()) {
                    button.setText(newFolder.storageText());
                } else {
                    button.setText(newFolder.getDisplayName());
                }

                Menu currentFolderContextMenu = currentFolderContextMenuManager.createContextMenu(button);
                button.setMenu(currentFolderContextMenu);
                button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent selectionEvent) {
                        currentFolderContextMenu.setVisible(true);
                    }
                });
            }
            setMinSize(foldersComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            setOrigin(foldersComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            foldersComposite.layout();
        }
    }
}
