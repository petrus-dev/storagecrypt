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

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.utils.StringUtils;
import fr.petrus.tools.storagecrypt.desktop.DesktopConstants;
import fr.petrus.tools.storagecrypt.desktop.DocumentAction;
import fr.petrus.tools.storagecrypt.desktop.Resources;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.swt.TextShortener;

/**
 * A custom SWT Table containing {@code EncryptedDocuments}, with context menus, drag and drop and
 * a few other convenient things.
 *
 * @author Pierre Sagne
 * @since 19.04.2016
 */
public class DocumentsTable {
    private static Logger LOG = LoggerFactory.getLogger(DocumentsTable.class);

    private static final int NUM_SURROUNDING_SPACES = 2;

    /**
     * The interface by which the {@code DocumentsTable} interacts with its owner.
     */
    public interface DocumentsTableListener
            extends DocumentContextMenuAction.DocumentContextMenuActionListener {

        /**
         * Returns true if the current folder is the "top level" folder of all documents.
         *
         * @return true if the current folder is the "top level" folder of all documents
         */
        boolean isCurrentFolderRoot();

        /**
         * Returns the current folder.
         *
         * @return the current folder
         */
        EncryptedDocument getCurrentFolder();

        /**
         * Returns the current folder children, sorted using the given {@code orderBy} criterion.
         *
         * @param orderBy the criterion used to sort the current folder children list
         * @return a list containing the current folder children, sorted using the given {@code orderBy}
         *         criterion
         */
        List<EncryptedDocument> getCurrentFolderChildren(OrderBy orderBy);

        /**
         * Requests an update of the current folder.
         */
        void update();

        /**
         * Opens the given {@code encryptedDocument}.
         *
         * <p>If the given {@code encryptedDocument} is a folder, show its contents in the
         * {@code DocumentsTable}.
         *
         * <p>If the given {@code encryptedDocument} is a file, try to open it with the appropriate
         * program.
         *
         * @param encryptedDocument the encrypted document to open
         * @throws DatabaseConnectionClosedException if the database connection is closed
         */
        void openDocument(EncryptedDocument encryptedDocument)
                throws DatabaseConnectionClosedException;

        /**
         * Deletes the given {@code encryptedDocuments}.
         *
         * @param encryptedDocuments the encrypted documents to delete
         * @return the number of successfully deleted documents
         * @throws DatabaseConnectionClosedException if the database connection closed
         */
        int deleteDocuments(List<EncryptedDocument> encryptedDocuments)
                throws DatabaseConnectionClosedException;

        /**
         * Proceed with encryption of the given {@code documentsToEncrypt}, after checking for
         * existing files and asking which ones to overwrite.
         *
         * @param destinationFolder  the destination folder where the documents will be encrypted
         * @param documentsToEncrypt the documents to encrypt
         */
        void encryptDocuments(EncryptedDocument destinationFolder, String[] documentsToEncrypt);
    }

    private DocumentsTableListener listener = null;
    private Resources resources = null;

    private OrderBy orderBy = OrderBy.NameAsc;

    private TextShortener textShortener = null;

    private TableViewer tableViewer = null;
    private TableViewerColumn nameColumn = null;
    private TableViewerColumn mimeTypeColumn = null;
    private TableViewerColumn sizeColumn = null;
    private TableViewerColumn syncOrDownloadColumn = null;
    private TableViewerColumn uploadColumn = null;
    private TableViewerColumn deletionColumn = null;

    /**
     * Creates a new {@code DocumentsTable} instance.
     *
     * @param parent     the parent of this {@code DocumentsTable}
     * @param textBundle a {@code TextBundle} instance
     * @param resources  a {@code Resources} instance
     * @param listener   the {@code DocumentsTableListener} this {@code DocumentsTable} uses to
     *                   interact with its owner
     */
    public DocumentsTable(Composite parent, TextBundle textBundle, Resources resources,
                          DocumentsTableListener listener) {
        this.listener = listener;
        this.resources = resources;
        textShortener = new TextShortener(parent.getDisplay(), TextShortener.Mode.ELLIPSIZE);
        tableViewer = createTableViewer(parent, listener);
        registerContextMenu(tableViewer, listener, textBundle);
        setupColumns(tableViewer, textBundle);
        setupDragDrop(tableViewer, listener);
        setupContentProvider(tableViewer, nameColumn, mimeTypeColumn, sizeColumn,
                syncOrDownloadColumn, uploadColumn,deletionColumn);
    }

    /**
     * Returns the SWT Table.
     *
     * @return the SWT Table
     */
    public Table getTable() {
        return tableViewer.getTable();
    }

    /**
     * Updates this {@code DocumentsTable} contents when the source of the documents is locked,
     * displaying nothing.
     */
    public void updateLocked() {
        tableViewer.getTable().removeAll();
    }

    /**
     * Updates the contents of this {@code DocumentsTable}.
     *
     * @param folderChanged if true, acts as if we changed to a new folder, scrolling back to the
     *                      beginning of the list, otherwise stays at the same position in the list.
     */
    public void update(boolean folderChanged) {
        if (listener.isCurrentFolderRoot()) {
            syncOrDownloadColumn.getColumn().setImage(
                    resources.loadImage(DesktopConstants.RESOURCES.IC_SYNC_BLACK));
            uploadColumn.getColumn().setImage(null);
            deletionColumn.getColumn().setImage(null);
        } else {
            syncOrDownloadColumn.getColumn().setImage(
                    resources.loadImage(DesktopConstants.RESOURCES.IC_DOWNLOAD_BLACK));
            uploadColumn.getColumn().setImage(
                    resources.loadImage(DesktopConstants.RESOURCES.IC_UPLOAD_BLACK));
            deletionColumn.getColumn().setImage(
                    resources.loadImage(DesktopConstants.RESOURCES.IC_DELETE_BLACK));
        }
        tableViewer.setInput(listener.getCurrentFolderChildren(orderBy));
        if (folderChanged) {
            tableViewer.getTable().setTopIndex(0);
        }
        setColumnsLayout(listener.isCurrentFolderRoot());
    }

    private TableViewer createTableViewer(Composite parent, final DocumentsTableListener listener) {
        TableViewer tableViewer =
                new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        table.addListener(SWT.MeasureItem, new Listener() {
            public void handleEvent(Event event) {
                event.height = Math.max(event.gc.getFontMetrics().getHeight(),
                        resources.loadImage(DesktopConstants.RESOURCES.IC_CLOUD).getBounds().height);
            }
        });

        table.addListener(SWT.Resize, new Listener () {
            public void handleEvent (Event e) {
                setColumnsLayout(listener.isCurrentFolderRoot());
            }
        });

        table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                switch (event.keyCode) {
                    case SWT.BS:
                    case SWT.DEL:
                        List<EncryptedDocument> selected = getSelected();
                        if (!selected.isEmpty()) {
                            try {
                                listener.deleteDocuments(selected);
                            } catch (DatabaseConnectionClosedException e) {
                                LOG.error("Database is closed", e);
                            }
                        }
                        break;
                }
            }
        });

        table.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent event) {
                if (event.detail == SWT.TRAVERSE_RETURN) {
                    List<EncryptedDocument> selected = getSelected();
                    if (selected.size()==1) {
                        try {
                            listener.openDocument(selected.get(0));
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                        }
                    }
                }
            }
        });

        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent doubleClickEvent) {
                List<EncryptedDocument> selected = getSelected();
                if (selected.size()==1) {
                    try {
                        listener.openDocument(selected.get(0));
                    } catch (DatabaseConnectionClosedException e) {
                        LOG.error("Database is closed", e);
                    }
                }
            }
        });

        return tableViewer;
    }

    private void registerContextMenu(final TableViewer tableViewer,
                                     final DocumentsTableListener listener,
                                     final TextBundle textBundle) {

        final MenuManager contextMenuManager = new MenuManager();
        contextMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                final List<EncryptedDocument> selectedDocuments = getSelected();
                int numRoots = 0;
                int numUnsynchronizedRoots = 0;
                int numFolders = 0;
                int numFiles = 0;
                for (EncryptedDocument document : selectedDocuments) {
                    if (document.isUnsynchronizedRoot()) {
                        numUnsynchronizedRoots++;
                    } else if (document.isRoot()) {
                        numRoots++;
                    } else if (document.isFolder()) {
                        numFolders++;
                    } else {
                        numFiles++;
                    }
                }

                if (selectedDocuments.size()==1) {
                    contextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_details"),
                            DocumentAction.Details,
                            selectedDocuments, listener));

                    contextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_open"),
                            DocumentAction.Open,
                            selectedDocuments, listener));

                    if (1==numRoots || 1==numFolders) {
                        contextMenuManager.add(new DocumentContextMenuAction(
                                textBundle.getString("document_context_menu_select_default_key"),
                                DocumentAction.SelectDefaultKey,
                                selectedDocuments, listener));
                    }
                }

                if (0==numUnsynchronizedRoots) {
                    contextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_delete"),
                            DocumentAction.Delete,
                            selectedDocuments, listener));
                }

                if (selectedDocuments.size() == numRoots) {
                    contextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_push_updates"),
                            DocumentAction.PushUpdates,
                            selectedDocuments, listener));

                    contextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_sync_remote_changes"),
                            DocumentAction.ChangesSync,
                            selectedDocuments, listener));
                }

                if (selectedDocuments.size() == numUnsynchronizedRoots) {
                    contextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_import_existing"),
                            DocumentAction.Import,
                            selectedDocuments, listener));
                }

                if (0==numRoots && 0==numUnsynchronizedRoots) {
                    contextMenuManager.add(new DocumentContextMenuAction(
                            textBundle.getString("document_context_menu_decrypt"),
                            DocumentAction.Decrypt,
                            selectedDocuments, listener));
                }
            }
        });

        contextMenuManager.setRemoveAllWhenShown(true);
        tableViewer.getControl().setMenu(
                contextMenuManager.createContextMenu(tableViewer.getControl()));
    }

    private List<EncryptedDocument> getSelected() {
        List<EncryptedDocument> selected = new ArrayList<>();

        IStructuredSelection selection = tableViewer.getStructuredSelection();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object element = it.next();
            if (null!=element) {
                selected.add((EncryptedDocument) element);
            }
        }

        return selected;
    }

    private void setupColumns(TableViewer tableViewer, TextBundle textBundle) {
        nameColumn = createColumn(tableViewer, SWT.LEFT,
                textBundle.getString("document_list_name_column_text"));
        mimeTypeColumn = createColumn(tableViewer, SWT.LEFT,
                textBundle.getString("document_list_mime_type_column_text"));
        sizeColumn = createColumn(tableViewer, SWT.LEFT,
                textBundle.getString("document_list_size_column_text"));

        syncOrDownloadColumn = createColumn(tableViewer, SWT.CENTER,
                resources.loadImage(DesktopConstants.RESOURCES.IC_DOWNLOAD_BLACK));
        uploadColumn = createColumn(tableViewer, SWT.CENTER,
                resources.loadImage(DesktopConstants.RESOURCES.IC_UPLOAD_BLACK));
        deletionColumn = createColumn(tableViewer, SWT.CENTER,
                resources.loadImage(DesktopConstants.RESOURCES.IC_DELETE_BLACK));

        initColumnsLayout();

        setupColumnOrderBy(nameColumn,
                OrderBy.NameAsc,
                OrderBy.NameDesc);

        setupColumnOrderBy(mimeTypeColumn,
                OrderBy.MimeTypeAsc,
                OrderBy.MimeTypeDesc);

        setupColumnOrderBy(sizeColumn,
                OrderBy.SizeAsc,
                OrderBy.SizeDesc);
    }

    private TableViewerColumn createColumn(TableViewer tableViewer, int style, String text) {
        TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, style);
        TableColumn tableColumn = tableViewerColumn.getColumn();
        tableColumn.setResizable(false);
        tableColumn.setText(text);
        return tableViewerColumn;
    }

    private TableViewerColumn createColumn(TableViewer tableViewer, int style, Image image) {
        TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, style);
        TableColumn tableColumn = tableViewerColumn.getColumn();
        tableColumn.setResizable(false);
        tableColumn.setImage(image);
        return tableViewerColumn;
    }

    private void setupColumnOrderBy(TableViewerColumn tableViewerColumn,
                                    final OrderBy asc,
                                    final OrderBy desc) {
        tableViewerColumn.getColumn().addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (asc == orderBy) {
                    setOrderBy(desc);
                } else {
                    setOrderBy(asc);
                }
            }
        });
    }

    private void setupDragDrop(TableViewer tableViewer, final DocumentsTableListener listener) {
        final FileTransfer fileTransfer = FileTransfer.getInstance();

        final DropTarget target = new DropTarget(tableViewer.getTable(),
                DND.DROP_DEFAULT | DND.DROP_COPY);
        target.setTransfer(new Transfer[] { fileTransfer } );
        target.addDropListener(new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetEvent dropTargetEvent) {
                if (listener.isCurrentFolderRoot()) {
                    dropTargetEvent.detail = DND.DROP_NONE;
                } else {
                    if (dropTargetEvent.detail == DND.DROP_DEFAULT) {
                        dropTargetEvent.detail = DND.DROP_COPY;
                    } else {
                        dropTargetEvent.detail = DND.DROP_NONE;
                    }
                }
            }

            @Override
            public void dragOperationChanged(DropTargetEvent dropTargetEvent) {
                //files should only be copied
                if (dropTargetEvent.detail != DND.DROP_COPY) {
                    dropTargetEvent.detail = DND.DROP_NONE;
                }
            }

            @Override
            public void dragOver(DropTargetEvent dropTargetEvent) {
                dropTargetEvent.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
            }

            @Override
            public void drop(DropTargetEvent dropTargetEvent) {
                if (fileTransfer.isSupportedType(dropTargetEvent.currentDataType)) {
                    String[] files = (String[]) dropTargetEvent.data;
                    if (null != files && files.length > 0) {
                        if (null!=dropTargetEvent.item) {
                            TableItem item = (TableItem) dropTargetEvent.item;
                            if (null != item.getData() && item.getData() instanceof EncryptedDocument) {
                                EncryptedDocument destinationDocument = (EncryptedDocument) item.getData();
                                if (destinationDocument.isRoot() || destinationDocument.isFolder()) {
                                    listener.encryptDocuments(destinationDocument, files);
                                    return;
                                }
                            }
                        }
                        if (!listener.isCurrentFolderRoot()) {
                            listener.encryptDocuments(listener.getCurrentFolder(), files);
                        }
                    }
                }
            }
        });
    }

    private void setupContentProvider(final TableViewer tableViewer,
                                      final TableViewerColumn nameColumn,
                                      final TableViewerColumn mimeTypeColumn,
                                      final TableViewerColumn sizeColumn,
                                      final TableViewerColumn syncOrDownloadColumn,
                                      final TableViewerColumn uploadColumn,
                                      final TableViewerColumn deletionColumn) {

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        nameColumn.setLabelProvider(new OwnerDrawLabelProvider() {
            @Override
            protected void measure(Event event, Object element) {
                EncryptedDocument document = (EncryptedDocument) element;
                Image image;
                if (document.isRoot()) {
                    if (document.isUnsynchronized()) {
                        image = resources.loadImage(DesktopConstants.RESOURCES.IC_FOLDER);
                    } else {
                        image = resources.loadImage(DesktopConstants.RESOURCES.IC_CLOUD);
                    }
                } else if (document.isFolder()) {
                    image = resources.loadImage(DesktopConstants.RESOURCES.IC_FOLDER);
                } else {
                    image = resources.loadImage(DesktopConstants.RESOURCES.IC_FILE);
                }
                String text;
                if (document.isRoot()) {
                    text = StringUtils.surroundWithSpaces(document.storageText(), NUM_SURROUNDING_SPACES);
                } else {
                    text = StringUtils.surroundWithSpaces(document.getDisplayName(), NUM_SURROUNDING_SPACES);
                }
                int widthLeftForText = nameColumn.getColumn().getWidth() - image.getBounds().width;
                event.setBounds(getImageAndTextBounds(event.x, event.y, image, event.gc,
                        textShortener.shortenText(event.gc, text, widthLeftForText)));
            }

            @Override
            protected void paint(Event event, Object element) {
                EncryptedDocument document = (EncryptedDocument) element;
                Image image;
                if (document.isRoot()) {
                    if (document.isUnsynchronized()) {
                        image = resources.loadImage(DesktopConstants.RESOURCES.IC_FOLDER);
                    } else {
                        image = resources.loadImage(DesktopConstants.RESOURCES.IC_CLOUD);
                    }
                } else if (document.isFolder()) {
                    image = resources.loadImage(DesktopConstants.RESOURCES.IC_FOLDER);
                } else {
                    image = resources.loadImage(DesktopConstants.RESOURCES.IC_FILE);
                }
                String text;
                if (document.isRoot()) {
                    text = StringUtils.surroundWithSpaces(document.storageText(), NUM_SURROUNDING_SPACES);
                } else {
                    text = StringUtils.surroundWithSpaces(document.getDisplayName(), NUM_SURROUNDING_SPACES);
                }
                Rectangle bounds = event.getBounds();
                event.gc.drawImage(image, bounds.x + 4,
                        bounds.y + ( bounds.height - image.getBounds().height ) / 2);

                int widthLeftForText = nameColumn.getColumn().getWidth() - image.getBounds().width;

                // if the text is too long, shorten it
                text = textShortener.shortenText(event.gc, text, widthLeftForText);
                Point textSize = event.gc.textExtent(text, textShortener.getDrawFlags());
                event.gc.drawText(text, bounds.x + 8 + image.getBounds().width,
                        bounds.y + ( bounds.height - textSize.y ) / 2,
                        textShortener.getDrawFlags() | SWT.DRAW_TRANSPARENT);
            }
        });

        mimeTypeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                EncryptedDocument document = (EncryptedDocument) element;
                if (document.isRoot() || document.isFolder()) {
                    return null;
                } else {
                    return StringUtils.surroundWithSpaces(document.getMimeType(), NUM_SURROUNDING_SPACES);
                }
            }
        });

        sizeColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                EncryptedDocument document = (EncryptedDocument) element;
                if (document.isRoot()) {
                    return StringUtils.surroundWithSpaces(document.getBackStorageQuotaText(), NUM_SURROUNDING_SPACES);
                } else if (document.isFolder()){
                    return null;
                } else {
                    return StringUtils.surroundWithSpaces(document.getSizeText(), NUM_SURROUNDING_SPACES);
                }
            }
        });

        syncOrDownloadColumn.setLabelProvider(new OwnerDrawLabelProvider() {
            @Override
            protected void measure(Event event, Object element) {
                Image image = getSyncOrDownloadStateImage((EncryptedDocument) element);
                if (null!=image) {
                    int columnWidth = syncOrDownloadColumn.getColumn().getWidth();
                    event.setBounds(getCenteredImageBounds(event.x, event.y,
                            columnWidth, event.height, image));
                }
            }

            @Override
            protected void paint(Event event, Object element) {
                Image image = getSyncOrDownloadStateImage((EncryptedDocument) element);
                if (null!=image) {
                    int columnWidth = syncOrDownloadColumn.getColumn().getWidth();
                    Rectangle bounds = getCenteredImageBounds(event.x, event.y,
                            columnWidth, event.height, image);
                    event.gc.drawImage(image, bounds.x, bounds.y);
                }
            }
        });

        uploadColumn.setLabelProvider(new OwnerDrawLabelProvider() {
            @Override
            protected void measure(Event event, Object element) {
                Image image = getUploadStateImage((EncryptedDocument) element);
                if (null!=image) {
                    int columnWidth = uploadColumn.getColumn().getWidth();
                    event.setBounds(getCenteredImageBounds(event.x, event.y,
                            columnWidth, event.height, image));
                }
            }

            @Override
            protected void paint(Event event, Object element) {
                Image image = getUploadStateImage((EncryptedDocument) element);
                if (null!=image) {
                    int columnWidth = uploadColumn.getColumn().getWidth();
                    Rectangle bounds = getCenteredImageBounds(event.x, event.y,
                            columnWidth, event.height, image);
                    event.gc.drawImage(image, bounds.x, bounds.y);
                }
            }
        });

        deletionColumn.setLabelProvider(new OwnerDrawLabelProvider() {
            @Override
            protected void measure(Event event, Object element) {
                Image image = getDeletionStateImage((EncryptedDocument) element);
                if (null!=image) {
                    int columnWidth = deletionColumn.getColumn().getWidth();
                    event.setBounds(getCenteredImageBounds(event.x, event.y,
                            columnWidth, event.height, image));
                }
            }

            @Override
            protected void paint(Event event, Object element) {
                Image image = getDeletionStateImage((EncryptedDocument) element);
                if (null!=image) {
                    int columnWidth = deletionColumn.getColumn().getWidth();
                    Rectangle bounds = getCenteredImageBounds(event.x, event.y,
                            columnWidth, event.height, image);
                    event.gc.drawImage(image, bounds.x, bounds.y);
                }
            }
        });
    }

    private Rectangle getImageAndTextBounds(int x, int y, Image image, GC gc, String text) {
        Point textSize = gc.textExtent(text);
        Rectangle imageBounds = image.getBounds();
        return new Rectangle(x, y,
                imageBounds.width + textSize.x + 8,
                Math.max(imageBounds.height, textSize.y + 8));
    }

    private Rectangle getCenteredImageBounds(int x, int y, int width, int height, Image image) {
        Rectangle imageBounds = image.getBounds();
        return new Rectangle(
                x + ( width  - imageBounds.width  ) / 2,
                y + ( height - imageBounds.height ) / 2,
                imageBounds.width,
                imageBounds.height);
    }

    private Image getSyncOrDownloadStateImage(EncryptedDocument document) {
        if (document.isUnsynchronizedRoot()) {
            return null;
        } else if (document.isRoot()) {
            if (null==document.getBackStorageAccount().getChangesSyncState()) {
                return null;
            } else {
                switch (document.getBackStorageAccount().getChangesSyncState()) {
                    case Planned:
                        return resources.loadImage(DesktopConstants.RESOURCES.IC_SYNC_VIOLET);
                    case Running:
                        return resources.loadImage(DesktopConstants.RESOURCES.IC_SYNC_GREEN);
                    default:
                        return null;
                }
            }
        } else {
            switch (document.getSyncState(SyncAction.Download)) {
                case Planned:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_DOWNLOAD_VIOLET);
                case Running:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_DOWNLOAD_GREEN);
                case Failed:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_DOWNLOAD_RED);
                default:
                    return null;
            }
        }
    }

    private Image getUploadStateImage(EncryptedDocument document) {
        if (document.isRoot()) {
            return null;
        } else {
            switch (document.getSyncState(SyncAction.Upload)) {
                case Planned:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_UPLOAD_VIOLET);
                case Running:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_UPLOAD_GREEN);
                case Failed:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_UPLOAD_RED);
                default:
                    return null;
            }
        }
    }

    private Image getDeletionStateImage(EncryptedDocument document) {
        if (document.isRoot()) {
            return null;
        } else {
            switch (document.getSyncState(SyncAction.Deletion)) {
                case Planned:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_DELETE_VIOLET);
                case Running:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_DELETE_GREEN);
                case Failed:
                    return resources.loadImage(DesktopConstants.RESOURCES.IC_DELETE_RED);
                default:
                    return null;
            }
        }
    }

    private void initColumnsLayout() {
        setColumnsLayout(false);
    }

    private void setColumnsLayout(boolean root) {
        int remainingWidth = tableViewer.getTable().getClientArea().width;

        tableViewer.getTable().setRedraw(false);

        nameColumn.getColumn().setWidth(100);
        mimeTypeColumn.getColumn().setWidth(100);
        sizeColumn.getColumn().setWidth(100);
        syncOrDownloadColumn.getColumn().setWidth(100);
        uploadColumn.getColumn().setWidth(100);
        deletionColumn.getColumn().setWidth(100);

        if (root) {
            mimeTypeColumn.getColumn().setWidth(0);
            uploadColumn.getColumn().setWidth(0);
            deletionColumn.getColumn().setWidth(0);

            sizeColumn.getColumn().pack();
            remainingWidth -= sizeColumn.getColumn().getWidth();
            syncOrDownloadColumn.getColumn().pack();
            remainingWidth -= syncOrDownloadColumn.getColumn().getWidth();
        } else {
            mimeTypeColumn.getColumn().pack();
            remainingWidth -= mimeTypeColumn.getColumn().getWidth();
            sizeColumn.getColumn().pack();
            remainingWidth -= sizeColumn.getColumn().getWidth();
            syncOrDownloadColumn.getColumn().pack();
            remainingWidth -= syncOrDownloadColumn.getColumn().getWidth();
            uploadColumn.getColumn().pack();
            remainingWidth -= uploadColumn.getColumn().getWidth();
            deletionColumn.getColumn().pack();
            remainingWidth -= deletionColumn.getColumn().getWidth();
        }
        nameColumn.getColumn().setWidth(Math.max(100, remainingWidth));

        tableViewer.getTable().setRedraw(true);
    }

    private void setOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        listener.update();
    }
}
