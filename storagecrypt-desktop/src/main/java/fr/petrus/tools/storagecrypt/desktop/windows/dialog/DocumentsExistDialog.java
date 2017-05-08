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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.filesystem.tree.PathNode;
import fr.petrus.lib.core.filesystem.tree.PathTree;
import fr.petrus.tools.storagecrypt.desktop.DesktopConstants;
import fr.petrus.tools.storagecrypt.desktop.Resources;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;

/**
 * The dialog used to select existing documents for overwriting.
 *
 * @author Pierre Sagne
 * @since 25.09.2016
 */
public class DocumentsExistDialog extends CustomDialog<DocumentsExistDialog> {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentsExistDialog.class);

    private Resources resources;
    private final List<String> existingDocuments = new ArrayList<>();
    private final Set<String> selectedDocuments = new HashSet<>();

    /**
     * Creates a new {@code DocumentsExistDialog} instance for the given {@code documents}.
     *
     * @param appWindow         the application window
     * @param resources         a {@code Resources} instance
     * @param existingDocuments the list of documents to select
     */
    public DocumentsExistDialog(AppWindow appWindow, Resources resources,
                                List<String> existingDocuments) {
        super(appWindow);
        setClosable(true);
        setResizable(true);
        setTitle(textBundle.getString("documents_exist_dialog_title"));
        setPositiveButtonText(textBundle.getString("documents_exist_dialog_ok_button_text"));
        setNegativeButtonText(textBundle.getString("documents_exist_dialog_cancel_button_text"));
        this.resources = resources;
        this.existingDocuments.addAll(existingDocuments);
    }

    /**
     * Returns the documents which were selected by the user.
     *
     * @return the documents which were selected by the user
     */
    public List<String> getSelectedDocuments() {
        return new ArrayList<>(selectedDocuments);
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent);

        final Label titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(textBundle.getString("documents_exist_dialog_message"));
        applyGridData(titleLabel).withHorizontalFill();

        final PathTree existingDocumentsTree = PathTree.buildTree(existingDocuments);

        final TreeViewer treeViewer = new TreeViewer(parent, SWT.CHECK | SWT.V_SCROLL);
        applyGridData(treeViewer.getTree()).withFill();
        treeViewer.setContentProvider(new ITreeContentProvider() {
            @Override
            public Object[] getElements(Object inputElement) {
                return ((List<PathNode>) inputElement).toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                PathNode parent = (PathNode) parentElement;
                if (parent.isDirectory()) {
                    return parent.getChildren().toArray();
                } else {
                    return new Object[0];
                }
            }

            @Override
            public Object getParent(Object element) {
                return ((PathNode)element).getParent();
            }

            @Override
            public boolean hasChildren(Object element) {
                return ((PathNode)element).isDirectory();
            }
        });

        treeViewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                PathNode pathNode = (PathNode) element;
                File file = new File(pathNode.getFilePath());
                if (file.isDirectory()) {
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_FOLDER);
                } else {
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_FILE);
                }
            }

            @Override
            public String getText(Object element) {
                PathNode pathNode = (PathNode) element;
                return pathNode.getFileName();
            }
        });
        treeViewer.setInput(existingDocumentsTree.getRoots());
        treeViewer.expandAll();

        treeViewer.getTree().addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                if (SWT.CHECK == event.detail) {
                    if (null != event.item) {
                        final TreeItem item = (TreeItem) event.item;
                        if (item.getChecked()) {
                            selectItem(item);
                        } else {
                            deselectItem(item);
                        }
                    }
                }
            }
        });

        registerContextMenu(treeViewer, textBundle);

        getShell().layout();
    }

    private void selectItem(TreeItem item) {
        PathNode pathNode = (PathNode)item.getData();
        selectedDocuments.add(pathNode.getFilePath());
        LOG.debug("Selected {}", pathNode.getFilePath());
        selectParents(item);
    }

    private void deselectItem(TreeItem item) {
        PathNode pathNode = (PathNode)item.getData();
        selectedDocuments.remove(pathNode.getFilePath());
        LOG.debug("Deselected {}", pathNode.getFilePath());
        deselectChildren(item);
    }

    private void selectParents(TreeItem item) {
        TreeItem parentItem = item.getParentItem();
        if (null!=parentItem) {
            if (!parentItem.getChecked()) {
                parentItem.setChecked(true);
                selectItem(parentItem);
            }
        }
    }

    private void deselectChildren(TreeItem parentItem) {
        TreeItem[] children = parentItem.getItems();
        if (null!=children) {
            for (TreeItem child : children) {
                if (child.getChecked()) {
                    child.setChecked(false);
                    deselectItem(child);
                }
            }
        }
    }

    private void selectChildrenRecursively(TreeItem parentItem) {
        TreeItem[] children = parentItem.getItems();
        if (null!=children) {
            for (TreeItem child : children) {
                if (!child.getChecked()) {
                    child.setChecked(true);
                    selectItem(child);
                }
                selectChildrenRecursively(child);
            }
        }
    }

    private void selectAll(TreeViewer treeViewer) {
        for (TreeItem item : treeViewer.getTree().getItems()) {
            if (!item.getChecked()) {
                item.setChecked(true);
                selectItem(item);
            }
            selectChildrenRecursively(item);
        }
    }

    private void deselectAll(TreeViewer treeViewer) {
        for (TreeItem item : treeViewer.getTree().getItems()) {
            if (item.getChecked()) {
                item.setChecked(false);
                deselectItem(item);
            }
        }
    }

    private void registerContextMenu(final TreeViewer treeViewer, final TextBundle textBundle) {

        final MenuManager contextMenuManager = new MenuManager();
        contextMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                final PathNode selectedPathNode = getFirstSelected(treeViewer);
                contextMenuManager.add(new Action(textBundle.getString("documents_exist_context_menu_select_all")) {
                    @Override
                    public void run() {
                        selectAll(treeViewer);
                    }
                });

                contextMenuManager.add(new Action(textBundle.getString("documents_exist_context_menu_deselect_all")) {
                    @Override
                    public void run() {
                        deselectAll(treeViewer);
                    }
                });
                if (null!= selectedPathNode && selectedPathNode.isDirectory()) {
                    final TreeItem selectedItem = getFirstSelectedItem(treeViewer);
                    contextMenuManager.add(new Action(textBundle.getString("documents_exist_context_menu_select_children")) {
                        @Override
                        public void run() {
                            selectChildrenRecursively(selectedItem);
                        }
                    });

                    contextMenuManager.add(new Action(textBundle.getString("documents_exist_context_menu_deselect_children")) {
                        @Override
                        public void run() {
                            deselectChildren(selectedItem);
                        }
                    });
                }
            }
        });

        contextMenuManager.setRemoveAllWhenShown(true);
        treeViewer.getControl().setMenu(contextMenuManager.createContextMenu(treeViewer.getControl()));
    }

    private PathNode getFirstSelected(TreeViewer treeViewer) {
        IStructuredSelection selection = treeViewer.getStructuredSelection();
        if (selection.isEmpty()) {
            return null;
        } else {
            return (PathNode)selection.getFirstElement();
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
