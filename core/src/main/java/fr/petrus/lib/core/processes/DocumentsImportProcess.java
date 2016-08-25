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

package fr.petrus.lib.core.processes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocumentMetadata;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.ParentNotFoundException;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.exceptions.NetworkException;
import fr.petrus.lib.core.NotFoundException;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.FailedResult;
import fr.petrus.lib.core.processes.results.SourceDestinationResult;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The {@code Process} which imports documents from the filesystem or from the remote account.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DocumentsImportProcess extends AbstractProcess<DocumentsImportProcess.Results> {
    private static Logger LOG = LoggerFactory.getLogger(DocumentsImportProcess.class);

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     */
    public static class Results extends BaseProcessResults<SourceDestinationResult<String, EncryptedDocument>, String> {

        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super (textI18n, true, true, true);
        }

        @Override
        public int getResultsColumnsCount(ResultsType resultsType) {
            if (null!=resultsType) {
                switch (resultsType) {
                    case Success:
                        return 1;
                    case Skipped:
                        return 1;
                    case Errors:
                        return 2;
                }
            }
            return 0;
        }

        @Override
        public String[] getResultColumns(ResultsType resultsType, int i) {
            String[] result;
            if (null==resultsType) {
                result = new String[0];
            } else {
                switch (resultsType) {
                    case Success:
                        try {
                            result = new String[] { success.get(i).getDestination().logicalPath() };
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            result = new String[] { success.get(i).getDestination().getDisplayName() };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[] { success.get(i).getDestination().getDisplayName() };
                        }
                        break;
                    case Skipped:
                        try {
                            result = new String[] { skipped.get(i).getDestination().logicalPath() };
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            result = new String[] { skipped.get(i).getDestination().getDisplayName() };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[] { skipped.get(i).getDestination().getDisplayName() };
                        }
                        break;
                    case Errors:
                        result = new String[] {
                                errors.get(i).getElement(),
                                textI18n.getExceptionDescription(errors.get(i).getException())
                        };
                        break;
                    default:
                        result = new String[0];
                }
            }
            return result;
        }

        /**
         * Returns the successfully imported documents as a list.
         *
         * @return the list of successfully imported documents
         */
        public List<EncryptedDocument> getSuccessfulyImportedDocuments() {
            List<EncryptedDocument> results = new ArrayList<>();
            for (SourceDestinationResult<String, EncryptedDocument> result : success) {
                results.add(result.getDestination());
            }
            return results;
        }
    }

    private Crypto crypto;
    private KeyManager keyManager;
    private TextI18n textI18n;
    private ConcurrentLinkedQueue<EncryptedDocument> importRoots = new ConcurrentLinkedQueue<>();
    private List<SourceDestinationResult<String, EncryptedDocument>> successfulImports = new ArrayList<>();
    private List<SourceDestinationResult<String, EncryptedDocument>> existingDocuments = new ArrayList<>();
    private LinkedHashMap<String, FailedResult<String>> failedImports = new LinkedHashMap<>();
    private ProgressListener progressListener;
    private int numRootsToProcess;
    private int numProcessedDocuments;
    private int numFoundDocuments;

    /**
     * Creates a new {@code DocumentsImportProcess}, providing its dependencies.
     *
     * @param crypto             a {@code Crypto} instance
     * @param keyManager         a {@code KeyManager} instance
     * @param textI18n           a {@code TextI18n} instance
     * @param accounts           an {@code Accounts} instance
     * @param encryptedDocuments an {@code EncryptedDocuments} instance
     */
    public DocumentsImportProcess(Crypto crypto, KeyManager keyManager, TextI18n textI18n,
                                  Accounts accounts, EncryptedDocuments encryptedDocuments) {
        super(new Results(textI18n));
        this.crypto = crypto;
        this.keyManager = keyManager;
        this.textI18n = textI18n;
        progressListener = null;
        numRootsToProcess = 0;
        numProcessedDocuments = 0;
        numFoundDocuments = 0;
    }

    /**
     * Sets the {@code ProgressListener} which this process will report its progress to.
     *
     * @param progressListener the {@code ProgressListener} which this process will report its progress to
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Enqueues the given {@code importRoot} in the list of folders to import.
     *
     * <p>If the given {@code rootFolder} has a "Unsynchronized" {@code StorageType} files are imported from
     * the local filesystem, otherwise they are imported from the associated {@code RemoteStorage}.
     *
     * @param rootFolder the root folder which contents will be imported.
     */
    public void importDocuments(EncryptedDocument rootFolder) {
        importRoots.offer(rootFolder);
        numRootsToProcess++;
    }

    /**
     * Enqueues the given {@code importRoots} in the list of folders to import.
     *
     * @see #importDocuments(EncryptedDocument)
     *
     * @param rootFolders the root folders which contents will be imported.
     */
    public void importDocuments(List<EncryptedDocument> rootFolders) {
        for (EncryptedDocument rootFolder : rootFolders) {
            importRoots.offer(rootFolder);
        }
        numRootsToProcess += rootFolders.size();
    }

    /**
     * Imports the contents of the enqueued root folders.
     *
     * <p>If a root folder has a "Unsynchronized" {@code StorageType} files are imported from
     * the local filesystem, otherwise they are imported from the associated {@code RemoteStorage}.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void run()
            throws DatabaseConnectionClosedException {
        try {
            start();
            while (!importRoots.isEmpty()) {
                if (null != progressListener) {
                    progressListener.onSetMax(0, numRootsToProcess);
                    progressListener.onProgress(0, numRootsToProcess - importRoots.size());
                }
                EncryptedDocument rootFolder = importRoots.poll();
                if (null != progressListener) {
                    try {
                        progressListener.onMessage(0, rootFolder.logicalPath());
                    } catch (ParentNotFoundException e) {
                        LOG.error("Database is closed", e);
                        progressListener.onMessage(0, rootFolder.storageText());
                    }
                }
                try {
                    if (rootFolder.isRoot() && !rootFolder.isUnsynchronizedRoot()) {
                        rootFolder.checkRemoteRoot();
                    }
                    if (rootFolder.isUnsynchronized()) {
                        importLocalDocuments(rootFolder);
                    } else {
                        importRemoteDocuments(rootFolder);
                    }
                } catch (NetworkException | StorageCryptException e) {
                    LOG.error("Error while refreshing root document", e);
                }
            }
        } finally {
            getResults().addResults(successfulImports, existingDocuments, failedImports.values());
        }
    }

    /**
     * Imports the contents of the given {@code folder} from the local storage of the application.
     *
     * @param folder the folder which contents will be imported.
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void importLocalDocuments(EncryptedDocument folder)
            throws DatabaseConnectionClosedException {
        pauseIfNeeded();
        if (isCanceled()) {
            return;
        }
        File document = folder.file();
        if (document.isDirectory()) {
            File[] children = document.listFiles();
            if (children != null) {
                numFoundDocuments += children.length;
                if (null != progressListener) {
                    progressListener.onSetMax(1, numFoundDocuments);
                }
                for (File child : children) {
                    importLocalDocumentsTree(folder, child);
                }
            }
        }
    }

    /**
     * Imports the given {@code document}, and its contents if it is a folder.
     *
     * @param parent the parent {@code EncryptedDocument} of the given {@code document}
     * @param document the {@code File} to import
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void importLocalDocumentsTree(EncryptedDocument parent, File document)
            throws DatabaseConnectionClosedException {
        pauseIfNeeded();
        if (isCanceled()) {
            return;
        }
        numProcessedDocuments++;
        if (null!= progressListener) {
            progressListener.onProgress(1, numProcessedDocuments);
        }

        try {
            EncryptedDocumentMetadata encryptedDocumentMetadata =
                    new EncryptedDocumentMetadata(crypto, keyManager);
            encryptedDocumentMetadata.decrypt(document.getName());
            if (null!= progressListener) {
                progressListener.onMessage(1, encryptedDocumentMetadata.getDisplayName());
            }
            EncryptedDocument encryptedDocument = parent.child(encryptedDocumentMetadata.getDisplayName());
            if (null == encryptedDocument) {
                try {
                    encryptedDocument = parent.createChild(encryptedDocumentMetadata, document);
                    successfulImports.add(new SourceDestinationResult<>(
                            encryptedDocument.storageText() + " : " + document.getName(),
                            encryptedDocument));
                } catch (StorageCryptException e) {
                    failedImports.put(document.getAbsolutePath(), new FailedResult<>(document.getName(), e));
                }
            } else {
                existingDocuments.add(new SourceDestinationResult<>(
                        textI18n.getStorageTypeText(StorageType.Unsynchronized) + " : " + document.getAbsolutePath(),
                        encryptedDocument));
            }
            importLocalDocuments(encryptedDocument);
        } catch (StorageCryptException e) {
            LOG.error("Failed to find key matching document name {}", document.getName());
            failedImports.put(document.getAbsolutePath(), new FailedResult<>(
                    textI18n.getStorageTypeText(StorageType.Unsynchronized) + " : " + document.getAbsolutePath(), e));
        }
    }

    /**
     * Imports the contents of the given {@code folder} from its associated {@code RemoteStorage}.
     *
     * @param folder the folder which contents will be imported.
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void importRemoteDocuments(EncryptedDocument folder)
            throws DatabaseConnectionClosedException {
        pauseIfNeeded();
        if (isCanceled()) {
            return;
        }
        RemoteDocument document = null;
        try {
            document = folder.remoteDocument();
        } catch (NotFoundException | NetworkException | StorageCryptException e) {
            LOG.error("Failed to access remote document {}", folder.getDisplayName(), e);
            String documentPath;
            try {
                documentPath = folder.logicalPath();
            } catch (ParentNotFoundException de) {
                LOG.error("Parent not found", de);
                documentPath = folder.getDisplayName();
            } catch (DatabaseConnectionClosedException de) {
                LOG.error("Database is closed", de);
                documentPath = folder.getDisplayName();
            }
            failedImports.put(documentPath, new FailedResult<>(
                    folder.storageText() + " : " + documentPath, e));
        }
        if (null != document && document.isFolder()) {
            try {
                List<RemoteDocument> children = getRemoteChildren(document, new ProcessProgressAdapter() {
                    @Override
                    public boolean isCanceled() {
                        return DocumentsImportProcess.this.isCanceled();
                    }

                    @Override
                    public void pauseIfNeeded() {
                        DocumentsImportProcess.this.pauseIfNeeded();
                    }

                    @Override
                    public void onProgress(int i, int progress) {
                        if (null != progressListener) {
                            if (0 == i) {
                                progressListener.onSetMax(1, numFoundDocuments + progress);
                            }
                        }
                    }
                });
                LOG.debug("Found {} children", children.size());
                numFoundDocuments += children.size();
                if (null != progressListener) {
                    progressListener.onSetMax(1, numFoundDocuments);
                }
                for (RemoteDocument child : children) {
                    //ignore .metadata file
                    if (!Constants.STORAGE.FOLDER_METADATA_FILE_NAME.equals(child.getName())) {
                        importRemoteDocumentsTree(folder, child);
                    }
                }
            } catch (NetworkException | RemoteException e) {
                LOG.error("Failed to list remote folder children {}", folder.getDisplayName(), e);
                String documentPath;
                try {
                    documentPath = folder.logicalPath();
                } catch (ParentNotFoundException de) {
                    LOG.error("Parent not found", de);
                    documentPath = folder.getDisplayName();
                } catch (DatabaseConnectionClosedException de) {
                    LOG.error("Database is closed", de);
                    documentPath = folder.getDisplayName();
                }
                failedImports.put(documentPath, new FailedResult<>(
                        folder.storageText() + " : " + document.getName(),
                        new StorageCryptException(
                                "Failed to list remote folder children",
                                StorageCryptException.Reason.GetRemoteFolderError, e)));
            }
        }
    }

    // This method is only here to suppress the "unchecked conversion" warning
    @SuppressWarnings("unchecked")
    private List<RemoteDocument> getRemoteChildren(RemoteDocument remoteFolder,
                                                   ProcessProgressListener listener)
            throws DatabaseConnectionClosedException, RemoteException, NetworkException {
        return remoteFolder.childDocuments(listener);
    }

    /**
     * Imports the given {@code document}, and its contents if it is a folder.
     *
     * @param parent the parent {@code EncryptedDocument} of the given {@code document}
     * @param document the {@code RemoteDocument} to import
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void importRemoteDocumentsTree(EncryptedDocument parent, RemoteDocument document)
            throws DatabaseConnectionClosedException {
        pauseIfNeeded();
        if (isCanceled()) {
            return;
        }
        numProcessedDocuments++;
        if (null!= progressListener) {
            progressListener.onProgress(1, numProcessedDocuments);
        }

        String encryptedMetadata = null;

        if (document.isFolder()) {
            try {
                RemoteDocument metadataDocument = document.childDocument(Constants.STORAGE.FOLDER_METADATA_FILE_NAME);
                byte[] data = metadataDocument.downloadData();
                encryptedMetadata = crypto.encodeUrlSafeBase64(data);
            } catch (NetworkException | RemoteException e) {
                LOG.error("Failed to access remote folder metadata {}", document.getName(), e);
                failedImports.put(document.getName(), new FailedResult<>(document.getName(),
                        new StorageCryptException(
                                "Failed to access remote folder metadata",
                                StorageCryptException.Reason.FailedToGetMetadata, e)));
            }
        } else {
            encryptedMetadata = document.getName();
        }

        try {
            EncryptedDocumentMetadata encryptedDocumentMetadata =
                    new EncryptedDocumentMetadata(crypto, keyManager);
            encryptedDocumentMetadata.decrypt(encryptedMetadata);

            if (null!= progressListener) {
                progressListener.onMessage(1, encryptedDocumentMetadata.getDisplayName());
            }
            EncryptedDocument encryptedDocument = parent.child(encryptedDocumentMetadata.getDisplayName());
            if (null == encryptedDocument) {
                try {
                    encryptedDocument = parent.createChild(encryptedDocumentMetadata, encryptedMetadata, document);
                    successfulImports.add(new SourceDestinationResult<>(
                            encryptedDocument.storageText() + " : " + document.getName(),
                            encryptedDocument));
                } catch (StorageCryptException e) {
                    failedImports.put(document.getName(), new FailedResult<>(document.getName(), e));
                }
            } else {
                existingDocuments.add(new SourceDestinationResult<>(
                        encryptedDocument.storageText() + " : " + document.getName(),
                        encryptedDocument));
            }
            importRemoteDocuments(encryptedDocument);
        } catch (StorageCryptException e) {
            LOG.error("Failed to find key matching document name {}", document.getName());
            failedImports.put(document.getName(), new FailedResult<>(document.getName(), e));
        }
    }
}
