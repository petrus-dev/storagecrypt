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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

import java.io.File;

import fr.petrus.tools.storagecrypt.desktop.platform.DesktopFileSystem;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

/**
 * The dialog used to let the user pick an document.
 *
 * @author Pierre Sagne
 * @since 28.07.2015
 */
public class DocumentChooserDialog {
    private AppWindow appWindow;

    /**
     * Creates a new {@code DocumentChooserDialog} instance.
     *
     * @param appWindow the application window
     */
    public DocumentChooserDialog(AppWindow appWindow) {
        this.appWindow = appWindow;
    }

    /**
     * Opens this dialog with the given {@code title} to choose a new file to save, with the given
     * default {@code fileName}.
     *
     * @param title    the title displayed in this dialog title bar
     * @param fileName the default file name
     * @return the path of the new file
     */
    public String saveFile(String title, String fileName) {
        FileDialog dialog = new FileDialog(appWindow.getShell(), SWT.SAVE);
        dialog.setText(title);
        dialog.setFilterPath(DesktopFileSystem.getUserHomePath());
        dialog.setFileName(fileName);
        if (null!=dialog.open()) {
            String resultFileName = dialog.getFileName();
            if (null != resultFileName && !resultFileName.isEmpty()) {
                File file = new File(dialog.getFilterPath(), resultFileName);
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Opens this dialog with the given {@code title} to choose an existing file to load.
     *
     * @param title the title displayed in this dialog title bar
     * @return the path of the chosen file
     */
    public String chooseFile(String title) {
        return chooseFile(title, (String[])null);
    }

    /**
     * Opens this dialog with the given {@code title} to choose an existing file to load, filtered
     * by the given {@code filterExtensions}.
     *
     * @param title the title displayed in this dialog title bar
     * @param filterExtensions the list of file extensions to choose from
     * @return the path of the chosen file
     */
    public String chooseFile(String title, String... filterExtensions) {
        FileDialog dialog = new FileDialog(appWindow.getShell(), SWT.OPEN);
        dialog.setText(title);
        if (null!=filterExtensions) {
            dialog.setFilterExtensions(filterExtensions);
        }
        dialog.setFilterPath(DesktopFileSystem.getUserHomePath());
        if (null!=dialog.open()) {
            String fileName = dialog.getFileName();
            if (null != fileName && !fileName.isEmpty()) {
                File file = new File(dialog.getFilterPath(), fileName);
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Opens this dialog to choose an existing folder.
     *
     * @return the path of the chosen folder
     */
    public String chooseFolder() {
        DirectoryDialog dialog = new DirectoryDialog(appWindow.getShell());
        dialog.setFilterPath(DesktopFileSystem.getUserHomePath());
        return dialog.open();
    }
}
