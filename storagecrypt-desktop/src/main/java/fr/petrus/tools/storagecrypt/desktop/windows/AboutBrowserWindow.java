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
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.petrus.tools.storagecrypt.desktop.TextBundle;

/**
 * The window which displays information about the application
 *
 * @author Pierre Sagne
 * @since 28.07.2015
 */
public class AboutBrowserWindow extends Window {
    private static Logger LOG = LoggerFactory.getLogger(AboutBrowserWindow.class);

    private AppWindow appWindow = null;
    private String htmlContent = null;
    private TextBundle textBundle = null;

    /**
     * Creates a new {@code AboutBrowserWindow} instance.
     *
     * @param htmlContent the HTML content to display
     * @param appWindow   the application window
     */
    public AboutBrowserWindow(AppWindow appWindow, String htmlContent) {
        super(appWindow);
        this.appWindow = appWindow;
        this.htmlContent = htmlContent;
        this.textBundle = appWindow.getTextBundle();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(textBundle.getString("about_window_title"));
    }

    @Override
    protected Point getInitialSize() {
        Rectangle displayBounds = getShell().getDisplay().getBounds();
        return new Point(displayBounds.width/2, displayBounds.height*3/4);
    }

    @Override
    protected Control createContents(Composite parent) {
        parent.setLayout(new FillLayout());
        try {
            Browser browser = new Browser(parent, SWT.NONE);
            browser.setText(htmlContent);
            return browser;
        } catch (SWTError e) {
            LOG.error("The browser cannot be initialized.", e);
            appWindow.showErrorMessage(textBundle.getString("error_message_browser_init_error"));
        }
        return null;
    }
}
