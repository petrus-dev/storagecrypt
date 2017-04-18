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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.ColumnType;
import fr.petrus.lib.core.processes.results.ProcessResults;
import fr.petrus.lib.core.utils.StringUtils;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.swt.TextShortener;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The window which displays a detailed list of one type of results for an executed task
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class ResultsListWindow extends Window {
    private static final int NUM_SURROUNDING_SPACES = 2;

    private String title = null;
    private TextBundle textBundle = null;
    private ProcessResults results = null;
    private BaseProcessResults.ResultsType resultsType = null;
    private TableViewer tableViewer = null;
    private List<TableViewerColumn> resultsTableColumns = new ArrayList<>();

    /**
     * Creates a new {@code ResultsListWindow} displaying the given {@code resultsType} of the given
     * {@code results}.
     *
     * @param appWindow   the application window
     * @param title       the text to display in the title bar
     * @param results     the results
     * @param resultsType the results type
     */
    public ResultsListWindow(AppWindow appWindow, String title, ProcessResults results,
                             BaseProcessResults.ResultsType resultsType) {
        super(appWindow);
        this.textBundle = appWindow.getTextBundle();
        this.title = title;
        setShellStyle(SWT.TITLE | SWT.RESIZE | SWT.CLOSE);
        this.results = results;
        this.resultsType = resultsType;
    }

    public boolean isDisposed() {
        Shell shell = getShell();
        return null==shell || shell.isDisposed();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected Point getInitialSize() {
        Rectangle displayBounds = getShell().getDisplay().getBounds();
        return new Point(displayBounds.width / 4, displayBounds.height / 4);
    }

    @Override
    protected Control createContents(Composite parent) {
        applyGridLayout(parent);

        Composite contents = new Composite(parent, SWT.NONE);
        applyGridData(contents).withFill();
        applyGridLayout(contents);

        Label headerLabel = new Label(contents, SWT.NONE);
        applyGridData(headerLabel).withHorizontalFill();

        tableViewer = new TableViewer(contents, SWT.FULL_SELECTION | SWT.BORDER);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        Table table = tableViewer.getTable();
        applyGridData(table).withFill();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        if (null!=results && null!=resultsType) {
            switch (results.getResultsColumnsCount(resultsType)) {
                case 1:
                    resultsTableColumns.add(new TableViewerColumn(tableViewer, SWT.LEFT));
                    break;
                case 2:
                    resultsTableColumns.add(new TableViewerColumn(tableViewer, SWT.LEFT));
                    resultsTableColumns.add(new TableViewerColumn(tableViewer, SWT.LEFT));
                    break;
            }

            switch (resultsType) {
                case Success:
                    headerLabel.setText(textBundle.getString("results_dialog_success_message"));
                    break;
                case Skipped:
                    headerLabel.setText(textBundle.getString("results_dialog_skipped_message"));
                    break;
                case Errors:
                    headerLabel.setText(textBundle.getString("results_dialog_errors_message"));
                    break;
            }

            final TextShortener textShortener = new TextShortener(parent.getDisplay(), TextShortener.Mode.ELLIPSIZE);

            for (int i = 0; i<resultsTableColumns.size(); i++) {
                setColumnHeader(i);

                final int index = i;
                resultsTableColumns.get(i).setLabelProvider(new OwnerDrawLabelProvider() {
                    @Override
                    protected void measure(Event event, Object element) {
                        int widthLeftForText = resultsTableColumns.get(index).getColumn().getWidth();
                        String[] columns = (String[]) element;
                        String text = textShortener.shortenText(event.gc,
                                StringUtils.surroundWithSpaces(columns[index], NUM_SURROUNDING_SPACES),
                                widthLeftForText);
                        Point textSize = event.gc.textExtent(text);
                        event.setBounds(new Rectangle(event.x, event.y, textSize.x, textSize.y));
                    }

                    @Override
                    protected void paint(Event event, Object element) {
                        int widthLeftForText = resultsTableColumns.get(index).getColumn().getWidth();
                        String[] columns = (String[]) element;
                        String text = textShortener.shortenText(event.gc, columns[index], widthLeftForText);
                        event.gc.drawText(StringUtils.surroundWithSpaces(text, NUM_SURROUNDING_SPACES),
                                event.x, event.y, textShortener.getDrawFlags() | SWT.DRAW_TRANSPARENT);
                    }
                });
            }

            tableViewer.setInput(results.getResultsTexts(resultsType));

            for (int i = 0; i<resultsTableColumns.size(); i++) {
                resultsTableColumns.get(i).getColumn().pack();
            }
        }

        Button closeButton = new Button(contents, SWT.PUSH);
        closeButton.setText(textBundle.getString("results_list_dialog_close_button_text"));
        applyGridData(closeButton).withHorizontalFill();
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                ResultsListWindow.this.close();
            }
        });

        return contents;
    }

    private void setColumnHeader(int columnIndex) {
        ColumnType[] columnTypes = results.getResultsColumnsTypes(resultsType);

        String columnHeaderText = null;
        switch (columnTypes[columnIndex]) {
            case Document:
                columnHeaderText = textBundle.getString("results_list_dialog_column_header_document");
                break;
            case Source:
                columnHeaderText = textBundle.getString("results_list_dialog_column_header_source");
                break;
            case Destination:
                columnHeaderText = textBundle.getString("results_list_dialog_column_header_destination");
                break;
            case Error:
                columnHeaderText = textBundle.getString("results_list_dialog_column_header_error");
                break;
        }
        if (null!=columnHeaderText) {
            resultsTableColumns.get(columnIndex).getColumn().setText(columnHeaderText);
        }
    }
}
