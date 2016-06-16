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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;

import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The window which displays a summary of the results of an executed task
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class ResultsWindow extends Window {
    private AppWindow appWindow = null;
    private TextBundle textBundle = null;
    private String title = null;
    private String headerText = null;
    private BaseProcessResults results = null;
    private HashMap<BaseProcessResults.ResultsType, ResultsListWindow> resultsListWindows = null;

    /**
     * Creates a new {@code ResultsWindow} displaying the given {@code results} summary.
     *
     * @param appWindow the application window
     * @param results   the results to display
     */
    public ResultsWindow(AppWindow appWindow, BaseProcessResults results) {
        super(appWindow);
        this.appWindow = appWindow;
        this.textBundle = appWindow.getTextBundle();
        setShellStyle(SWT.TITLE | SWT.CLOSE);
        this.results = results;
        resultsListWindows = new HashMap<>();
    }

    /**
     * Sets the text to display in this window title bar.
     *
     * @param title the text to display in this window title bar
     * @return this {@code ResultsWindow} for further configuration
     */
    public ResultsWindow setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the text to display in this window header.
     *
     * @param headerText the text to display in this window header
     * @return this {@code ResultsWindow} for further configuration
     */
    public ResultsWindow setHeaderText(String headerText) {
        this.headerText = headerText;
        return this;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected Control createContents(Composite parent) {
        parent.setLayout(new FillLayout());

        Composite contents = new Composite(parent, SWT.NONE);
        applyGridLayout(contents).numColumns(2);

        if (null!=headerText) {
            Label headerLabel = new Label(contents, SWT.NONE);
            applyGridData(headerLabel).horizontalSpan(2).withHorizontalFill();
            headerLabel.setText(headerText);
        }

        if (null!=results.getSuccessResultsList()) {
            buildSuccessLine(contents);
        }

        if (null!=results.getSkippedResultsList()) {
            buildSkippedLine(contents);
        }

        if (null!=results.getErrorResultsList()) {
            buildErrorsLine(contents);
        }

        return contents;
    }

    private Button buildSuccessLine(Composite parent) {
        Button button = buildResultLine(parent, textBundle.getString("results_dialog_success_message"));
        button.setText(String.valueOf(results.getSuccessResultsList().size()));
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                openResultsList(BaseProcessResults.ResultsType.Success);
            }
        });
        if (results.getSuccessResultsList().isEmpty()) {
            button.setEnabled(false);
        }
        return button;
    }

    private Button buildSkippedLine(Composite parent) {
        Button button = buildResultLine(parent, textBundle.getString("results_dialog_skipped_message"));
        button.setText(String.valueOf(results.getSkippedResultsList().size()));
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                openResultsList(BaseProcessResults.ResultsType.Skipped);
            }
        });
        if (results.getSkippedResultsList().isEmpty()) {
            button.setEnabled(false);
        }
        return button;
    }

    private Button buildErrorsLine(Composite parent) {
        Button button = buildResultLine(parent, textBundle.getString("results_dialog_errors_message"));
        button.setText(String.valueOf(results.getErrorResultsList().size()));
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                openResultsList(BaseProcessResults.ResultsType.Errors);
            }
        });
        if (results.getErrorResultsList().isEmpty()) {
            button.setEnabled(false);
        }
        return button;
    }

    private Button buildResultLine(Composite parent, String headerText) {
        Label headerLabel = new Label(parent, SWT.NONE);
        headerLabel.setText(headerText);
        applyGridData(headerLabel).withHorizontalFill().horizontalAlignment(SWT.BEGINNING);
        Button button = new Button(parent, SWT.PUSH);
        applyGridData(button).withHorizontalFill();
        return button;
    }

    /**
     * Opens a new {@code ResultsListWindow} to display the given {@code resultType} for the results
     * of this {@code ResultsWindow}.
     *
     * @param resultsType the results type ti display
     */
    public void openResultsList(BaseProcessResults.ResultsType resultsType) {
        ResultsListWindow resultsListWindow = resultsListWindows.get(resultsType);
        if (null==resultsListWindow) {
            resultsListWindow = new ResultsListWindow(appWindow, title, results, resultsType);
            resultsListWindows.put(resultsType, resultsListWindow);
        }
        resultsListWindow.open();
    }
}
