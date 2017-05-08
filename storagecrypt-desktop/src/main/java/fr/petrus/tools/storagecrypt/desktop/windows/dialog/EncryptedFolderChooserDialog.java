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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.utils.StringUtils;
import fr.petrus.tools.storagecrypt.desktop.DesktopConstants;
import fr.petrus.tools.storagecrypt.desktop.Resources;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;

/**
 * The dialog used to select an encrypted folder.
 *
 * @author Pierre Sagne
 * @since 23.04.2017
 */
public class EncryptedFolderChooserDialog extends CustomDialog<EncryptedFolderChooserDialog> {
    private static final Logger LOG = LoggerFactory.getLogger(EncryptedFolderChooserDialog.class);

    private Resources resources;
    private List<EncryptedDocument> roots;
    private EncryptedDocument expandedFolderOnStart;
    private EncryptedDocument selectedFolder;

    /**
     * Creates a new {@code EncryptedFolderChooserDialog} instance for the given {@code documents}.
     *
     * @param appWindow the application window
     * @param resources a {@code Resources} instance
     * @param roots     the root folders
     */
    public EncryptedFolderChooserDialog(AppWindow appWindow, Resources resources,
                                        List<EncryptedDocument> roots) {
        this(appWindow, resources, roots, null);
    }

    /**
     * Creates a new {@code EncryptedFolderChooserDialog} instance for the given {@code documents}.
     *
     * @param appWindow             the application window
     * @param resources             a {@code Resources} instance
     * @param roots                 the root folders
     * @param expandedFolderOnStart the folder to start expanded
     */
    public EncryptedFolderChooserDialog(AppWindow appWindow, Resources resources,
                                        List<EncryptedDocument> roots,
                                        EncryptedDocument expandedFolderOnStart) {
        super(appWindow);
        setClosable(true);
        setResizable(true);
        setTitle(textBundle.getString("encrypted_folder_chooser_dialog_title"));
        setPositiveButtonText(textBundle.getString("encrypted_folder_chooser_dialog_ok_button_text"));
        setNegativeButtonText(textBundle.getString("encrypted_folder_chooser_dialog_cancel_button_text"));
        this.resources = resources;
        this.roots = roots;
        this.expandedFolderOnStart = expandedFolderOnStart;
        selectedFolder = null;
    }

    /**
     * Returns the folder which was selected by the user.
     *
     * @return the folder which was selected by the user
     */
    public EncryptedDocument getSelectedFolder() {
        return selectedFolder;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent);

        final Label titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(textBundle.getString("encrypted_folder_chooser_dialog_message"));
        applyGridData(titleLabel).withHorizontalFill();

        final TreeViewer treeViewer = new TreeViewer(parent, SWT.V_SCROLL);
        applyGridData(treeViewer.getTree()).withFill();
        treeViewer.setContentProvider(new ITreeContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return ((List<EncryptedDocument>) inputElement).toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                List<EncryptedDocument> children;
                try {
                    if (null == parentElement) {
                        children = roots;
                    } else {
                        EncryptedDocument parent = (EncryptedDocument) parentElement;
                        children = parent.children(true);
                    }
                    return getRootsAndFolders(children).toArray();
                } catch (DatabaseConnectionClosedException e) {
                    LOG.error("Database is closed", e);
                    return new Object[0];
                }
            }

            @Override
            public Object getParent(Object element) {
                if (null != element) {
                    try {
                        return ((EncryptedDocument) element).parent();
                    } catch (DatabaseConnectionClosedException e) {
                        LOG.error("Database is closed", e);
                    }
                }
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                try {
                    return !getRootsAndFolders(((EncryptedDocument)element).children(true)).isEmpty();
                } catch (DatabaseConnectionClosedException e) {
                    LOG.error("Database is closed", e);
                }
                return false;
            }
        });

        treeViewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                EncryptedDocument document = (EncryptedDocument) element;
                if (document.isRoot()) {
                    if (document.isUnsynchronized()) {
                        return resources.loadImage(DesktopConstants.RESOURCES.IC_FOLDER);
                    } else {
                        return resources.loadImage(DesktopConstants.RESOURCES.IC_CLOUD);
                    }
                } else if (document.isFolder()) {
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_FOLDER);
                } else {
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_FILE);
                }
            }

            @Override
            public String getText(Object element) {
                EncryptedDocument document = (EncryptedDocument) element;
                if (document.isRoot()) {
                    return document.storageText();
                } else {
                    return document.getDisplayName();
                }
            }
        });

        treeViewer.setInput(roots);
        //treeViewer.expandAll();
        if (null!=expandedFolderOnStart) {
            EncryptedDocument document = expandedFolderOnStart;
            List<EncryptedDocument> expandedElements = new LinkedList<>();
            try {
                while (null != document) {
                    expandedElements.add(0, document);
                    document = document.parent();
                }
            } catch (DatabaseConnectionClosedException e) {
                LOG.error("Database is closed", e);
            }
            treeViewer.setExpandedElements(expandedElements.toArray());
            treeViewer.setSelection(new StructuredSelection(expandedFolderOnStart));
        }

        treeViewer.getTree().addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                selectedFolder = getFirstSelected(treeViewer);
            }
        });

        getShell().layout();
    }

    private List<EncryptedDocument> getRootsAndFolders(List<EncryptedDocument> documents) {
        List<EncryptedDocument> rootsAndFolders = new ArrayList<>();
        for (EncryptedDocument document : documents) {
            if (document.isRoot() || document.isFolder()) {
                rootsAndFolders.add(document);
            }
        }
        return rootsAndFolders;

    }

    private EncryptedDocument getFirstSelected(TreeViewer treeViewer) {
        IStructuredSelection selection = treeViewer.getStructuredSelection();
        if (selection.isEmpty()) {
            return null;
        } else {
            return (EncryptedDocument) selection.getFirstElement();
        }
    }

    private TreeItem getFirstSelectedItem(TreeViewer treeViewer) {
        final TreeItem[] selectedItems = treeViewer.getTree().getSelection();
        if (0==selectedItems.length) {
            return null;
        } else {
            return selectedItems[0];
        }
    }
}
