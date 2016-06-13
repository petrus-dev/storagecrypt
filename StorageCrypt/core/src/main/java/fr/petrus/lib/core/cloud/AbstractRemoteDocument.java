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

package fr.petrus.lib.core.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProcessProgressListener;

/**
 * This abstract class implements the methods which are the same for all implementations of the
 * {@code RemoteDocument} interface.
 *
 * @param <S> the {@link RemoteStorage} implementation
 * @param <D> the {@link RemoteDocument} implementation
 * @author Pierre Sagne
 * @since 18.02.2015
 */
public abstract class AbstractRemoteDocument
        <S extends RemoteStorage<S, D>, D extends RemoteDocument<S, D>>
        implements RemoteDocument<S, D> {
    private static Logger LOG = LoggerFactory.getLogger(AbstractRemoteDocument.class);

    private String accountName;
    private String name;
    private long size;
    private boolean folder;
    private long version;
    private long modificationTime;

    /**
     * The RemoteStorage corresponding to this type of RemoteDocument.
     */
    protected S storage;

    /**
     * Returns the name of a documment, extracted from its path.
     *
     * @param path the path of the document on the {@link RemoteStorage}
     * @return the name of the document
     */
    public static String getNameFromPath(String path) {
        if (null==path) {
            return null;
        }
        return new File(path).getName();
    }

    /**
     * Returns the path of the parent folder, extracted from a child path.
     *
     * @param path the path of the child
     * @return the parent path
     */
    public static String getParentPath(String path) {
        if (null==path) {
            return null;
        }
        return new File(path).getParent();
    }

    /**
     * Returns the path of a child, from the path of its parent and the name of the child.
     *
     * @param parentPath the path of the parent folder
     * @param name       the name of the child document
     * @return the path of the child document
     */
    public static String getChildPath(String parentPath, String name) {
        if (null==parentPath && null==name) {
            return null;
        }
        if (null==parentPath) {
            return name;
        }
        if (null==name) {
            return parentPath;
        }
        return new File(new File(parentPath), name).getAbsolutePath();
    }

    /**
     * Creates a new AbstractRemoteDocument.
     *
     * @param storage he RemoteStorage corresponding to this type of RemoteDocument
     */
    public AbstractRemoteDocument(S storage) {
        this.storage = storage;
        accountName = null;
        name = null;
        size = 0;
        folder = false;
        version = -1;
        modificationTime = -1;
    }

    @Override
    public S getStorage() {
        return storage;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public boolean isFolder() {
        return folder;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public long getModificationTime() {
        return modificationTime;
    }

    @Override
    public D createChildFolderWithMetadata(String name, byte[] metadata)
            throws DatabaseConnectionClosedException, RemoteException {
        D folder = createChildFolder(name);
        try {
            folder.uploadNewChildData(Constants.STORAGE.FOLDER_METADATA_FILE_NAME,
                    Constants.STORAGE.DEFAULT_BINARY_MIME_TYPE,
                    Constants.STORAGE.FOLDER_METADATA_FILE_NAME,
                    metadata);
        } catch (RemoteException e) {
            try {
                storage.deleteFolder(folder.getAccountName(), folder.getId());
            } catch (RemoteException e2) {
                LOG.debug("Failed to remove folder metadata", e2);
            }
            throw e;
        }
        return folder;
    }

    @Override
    public void getRecursiveChanges(final RemoteChanges changes,
                                    final ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {

        List<D> children = childDocuments(new ProcessProgressAdapter() {
            @Override
            public boolean isCanceled() {
                return null!=listener && listener.isCanceled();
            }

            @Override
            public void pauseIfNeeded() {
                if (null!=listener) {
                    listener.pauseIfNeeded();
                }
            }

            @Override
            public void onProgress(int i, int progress) {
                if (null!=listener) {
                    listener.onProgress(i, changes.getChanges().size() + progress);
                }
            }
        });

        for (RemoteDocument child : children) {
            changes.putChange(child.getId(), RemoteChange.modification(child));
        }

        for (RemoteDocument child : children) {
            if (child.isFolder()) {
                child.getRecursiveChanges(changes, listener);
            }
        }
    }

    @Override
    public void getRecursiveChildren(final List<D> documents,
                                     final ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException {

        List<D> children = childDocuments(new ProcessProgressAdapter() {
            @Override
            public boolean isCanceled() {
                return null!=listener && listener.isCanceled();
            }

            @Override
            public void pauseIfNeeded() {
                if (null!=listener) {
                    listener.pauseIfNeeded();
                }
            }

            @Override
            public void onProgress(int i, int progress) {
                if (null!=listener) {
                    listener.onProgress(i, documents.size() + progress);
                }
            }
        });

        documents.addAll(children);
        for (RemoteDocument<S, D> child : children) {
            if (child.isFolder()) {
                child.getRecursiveChildren(documents, listener);
            }
        }
    }

    @Override
    public void delete() throws DatabaseConnectionClosedException, RemoteException {
        if (isFolder()) {
            getStorage().deleteFolder(getAccountName(), getId());
        } else {
            getStorage().deleteFile(getAccountName(), getId());
        }
    }
}
