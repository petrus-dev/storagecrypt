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

package fr.petrus.lib.core.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.Constants;

/**
 * The abstract implementation of the {@code FileSystem} interface.
 *
 * <p>The real implementations should extend this abstract class.
 *
 * @author Pierre Sagne
 * @since 23.03.2016
 */
public abstract class AbstractFileSystem implements FileSystem {
    private static Logger LOG = LoggerFactory.getLogger(AbstractFileSystem.class);

    @Override
    public String getAppDirPath() {
        try {
            return getAppDir().getCanonicalPath();
        } catch (IOException e) {
            LOG.error("Failed to get canonical path", e);
        }
        return null;
    }

    @Override
    public boolean createAppDir() {
        File appDir = getAppDir();
        return appDir.exists() || appDir.mkdirs();
    }

    @Override
    public File getTempFilesDir() {
        File tempFilesDir = new File(getAppDir(), Constants.FILE.TEMP_FILES_DIR_NAME);
        if (!tempFilesDir.exists()) {
            tempFilesDir.mkdirs();
        }
        return tempFilesDir;
    }

    @Override
    public File getLocalFilesDir() {
        File localFilesDir = new File(getAppDir(), Constants.FILE.LOCAL_FILES_DIR_NAME);
        if (!localFilesDir.exists()) {
            localFilesDir.mkdirs();
        }
        return localFilesDir;
    }

    @Override
    public File getGDriveDir() {
        return new File(getAppDir(), Constants.FILE.GDRIVE_FILES_DIR_NAME);
    }

    @Override
    public File getDropboxDir() {
        return new File(getAppDir(), Constants.FILE.DROPBOX_FILES_DIR_NAME);
    }

    @Override
    public File getBoxDir() {
        return new File(getAppDir(), Constants.FILE.BOX_FILES_DIR_NAME);
    }

    @Override
    public File getHubicDir() {
        return new File(getAppDir(), Constants.FILE.HUBIC_FILES_DIR_NAME);
    }

    @Override
    public File getOneDriveDir() {
        return new File(getAppDir(), Constants.FILE.ONEDRIVE_FILES_DIR_NAME);
    }

    @Override
    public File getGDriveFilesDir(String accountName) {
        File gDriveFilesDir = new File(getGDriveDir(), accountName);
        if (!gDriveFilesDir.exists()) {
            gDriveFilesDir.mkdirs();
        }
        return gDriveFilesDir;
    }

    @Override
    public File getDropboxFilesDir(String accountName) {
        File dropboxFilesDir = new File(getDropboxDir(), accountName);
        if (!dropboxFilesDir.exists()) {
            dropboxFilesDir.mkdirs();
        }
        return dropboxFilesDir;
    }

    @Override
    public File getBoxFilesDir(String accountName) {
        File boxFilesDir = new File(getBoxDir(), accountName);
        if (!boxFilesDir.exists()) {
            boxFilesDir.mkdirs();
        }
        return boxFilesDir;
    }

    @Override
    public File getHubicFilesDir(String accountName) {
        File hubicFilesDir = new File(getHubicDir(), accountName);
        if (!hubicFilesDir.exists()) {
            hubicFilesDir.mkdirs();
        }
        return hubicFilesDir;
    }

    @Override
    public File getOneDriveFilesDir(String accountName) {
        File oneDriveFilesDir = new File(getOneDriveDir(), accountName);
        if (!oneDriveFilesDir.exists()) {
            oneDriveFilesDir.mkdirs();
        }
        return oneDriveFilesDir;
    }

    @Override
    public List<String> getRecursiveDocumentsList(String document) {
        List<String> documents = new ArrayList<>();
        documents.add(document);
        File file = new File(document);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (null!=children) {
                for (File child : children) {
                    try {
                        documents.addAll(getRecursiveDocumentsList(child.getCanonicalPath()));
                    } catch (IOException e) {
                        LOG.error("Failed to get recursive documents list", e);
                    }
                }
            }
        }
        return documents;
    }

    @Override
    public List<String> getFoldersList(List<String> documents) {
        List<String> folders = new ArrayList<>();
        for (String document : documents) {
            File file = new File(document);
            if (file.isDirectory()) {
                folders.add(document);
            }
        }
        return folders;
    }

    @Override
    public List<String> getFilesList(List<String> documents) {
        List<String> files = new ArrayList<>();
        for (String document : documents) {
            File file = new File(document);
            if (!file.isDirectory()) {
                files.add(document);
            }
        }
        return files;
    }

    @Override
    public void deleteFolderContent(File folder) {
        File[] files = folder.listFiles();
        if (files!=null) {
            for (File f: files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
    }

    @Override
    public void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files!=null) {
            for (File f: files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    @Override
    public void deleteCloudSyncFolders() {
        deleteFolder(getGDriveDir());
        deleteFolder(getDropboxDir());
        deleteFolder(getBoxDir());
        deleteFolder(getHubicDir());
        deleteFolder(getOneDriveDir());
    }

    @Override
    public void copyFile(File sourceFile, File destinationFile) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(sourceFile).getChannel();
            outputChannel = new FileOutputStream(destinationFile).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            if (null!=inputChannel) {
                inputChannel.close();
            }
            if (null!=outputChannel) {
                outputChannel.close();
            }
        }
    }
}
