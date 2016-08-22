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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import fr.petrus.lib.core.DocumentHashQueue;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.ParentNotFoundException;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProcessProgressListener;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The {@code Process} which synchronizes the local documents on remote storages.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DocumentsSyncProcess extends AbstractProcess<DocumentsSyncProcess.Results> {

    private static Logger LOG = LoggerFactory.getLogger(DocumentsSyncProcess.class);

    /**
     * The interface used by this process for communicating with its caller.
     */
    public interface SyncActionListener {
        /**
         * The method called when the given {@code syncAction} starts for the given {@code encryptedDocument}.
         *
         * @param syncAction        the {@code SyncAction} which is taking place
         * @param encryptedDocument the {@code EncryptedDocument} being synchronized
         */
        void onSyncActionStart(SyncAction syncAction, EncryptedDocument encryptedDocument);

        /**
         * The method called to notify that the state of given {@code encryptedDocument} has changed.
         *
         * @param encryptedDocument the {@code EncryptedDocument} which state has changed
         */
        void onDocumentChanged(EncryptedDocument encryptedDocument);
    }

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     * <p/>
     * <p>This process returns no results by this means so this implementation holds nothing
     */
    public static class Results extends BaseProcessResults<Void, Void> {
        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super(textI18n, false, false, false);
        }
    }

    private Accounts accounts;
    private EncryptedDocuments encryptedDocuments;
    private Network network;
    private volatile EncryptedDocument currentSyncedDocument;
    private volatile boolean restartCurrentDocumentSync;
    private DocumentHashQueue syncQueue;
    private int numDocumentsToSync;
    private HashSet<Long> syncAccountsHistory;
    private ProgressListener progressListener;
    private SyncActionListener syncActionListener;

    /**
     * Creates a new {@code DocumentsSyncProcess}, providing its dependencies.
     *
     * @param textI18n           a {@code TextI18n} instance
     * @param network            a {@code Network} instance
     * @param accounts           an {@code Accounts} instance
     * @param encryptedDocuments an {@code EncryptedDocuments} instance
     */
    public DocumentsSyncProcess(TextI18n textI18n, Network network, Accounts accounts,
                                EncryptedDocuments encryptedDocuments) {
        super(new Results(textI18n));
        this.network = network;
        this.accounts = accounts;
        this.encryptedDocuments = encryptedDocuments;
        progressListener = null;
        syncActionListener = null;
        syncQueue = new DocumentHashQueue();
        numDocumentsToSync = 0;
        syncAccountsHistory = new HashSet<>();

        currentSyncedDocument = null;
        restartCurrentDocumentSync = false;
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
     * Sets the {@code SyncActionListener} which this process will report its progress to.
     *
     * @param syncActionListener the {@code SyncActionListener} which this process will report its progress to
     */
    public void setSyncActionListener(SyncActionListener syncActionListener) {
        this.syncActionListener = syncActionListener;
    }

    /**
     * Updates the synchronization queue by scanning all {@code EncryptedDocument}s and adding those
     * which synchronization state is either planned or failed.
     *
     * @return the number of {@code EncryptedDocument}s added to the synchronization queue
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public synchronized int updateSyncQueue() throws DatabaseConnectionClosedException {
        int numEnqueuedDocuments = 0;
        numEnqueuedDocuments += updateSyncQueue(SyncAction.Deletion);
        numEnqueuedDocuments += updateSyncQueue(SyncAction.Upload);
        numEnqueuedDocuments += updateSyncQueue(SyncAction.Download);
        return numEnqueuedDocuments;
    }

    /**
     * Updates the synchronization queue by scanning all {@code EncryptedDocument}s and adding those
     * for which the state of the given {@code syncAction} is either planned or failed.
     *
     * @param syncAction the {@code SyncAction} to check
     * @return the number of {@code EncryptedDocument}s added to the synchronization queue
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private int updateSyncQueue(SyncAction syncAction) throws DatabaseConnectionClosedException {
        int numEnqueuedDocuments = 0;
        if (network.isNetworkReadyForSyncAction(syncAction)) {
            for (EncryptedDocument encryptedDocument :
                    encryptedDocuments.encryptedDocumentsWithSyncState(syncAction, State.Planned)) {
                if (!encryptedDocument.hasTooManyFailures() && !encryptedDocument.hasTooManyRequests()) {
                    syncQueue.offer(encryptedDocument);
                    numEnqueuedDocuments++;
                }
            }
            for (EncryptedDocument encryptedDocument :
                    encryptedDocuments.encryptedDocumentsWithSyncState(syncAction, State.Failed)) {
                if (!encryptedDocument.hasTooManyFailures() && !encryptedDocument.hasTooManyRequests()) {
                    syncQueue.offer(encryptedDocument);
                    numEnqueuedDocuments++;
                }
            }
        }
        return numEnqueuedDocuments;
    }

    /**
     * Adds the given {@code encryptedDocuments} to the synchronization queue.
     *
     * @param encryptedDocuments the {@code EncryptedDocument}s to add to the synchronization queue
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public synchronized void enqueueDocuments(Collection<EncryptedDocument> encryptedDocuments)
            throws DatabaseConnectionClosedException {
        for (EncryptedDocument encryptedDocument : encryptedDocuments) {
            enqueueDocument(encryptedDocument);
        }
    }

    /**
     * Adds the given {@code encryptedDocument} to the synchronization queue.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to add to the synchronization queue
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public synchronized void enqueueDocument(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        encryptedDocument.refresh();
        if (!encryptedDocument.hasTooManyFailures() && !encryptedDocument.hasTooManyRequests()) {
            if (syncQueue.offer(encryptedDocument)) {
                numDocumentsToSync++;
                if (null != progressListener) {
                    progressListener.onSetMax(0, numDocumentsToSync);
                }
            }
        }
    }

    /**
     * Restarts the current document synchronization if the document being processed has the given
     * {@code documentId}.
     *
     * @param documentId the id of the {@code EncryptedDocument} to restart processing.
     */
    public synchronized void restartIfCurrent(long documentId) {
        if (null!=currentSyncedDocument && currentSyncedDocument.getId() == documentId) {
            restartCurrentDocumentSync = true;
        }
    }

    /**
     * Starts processing the documents in the synchronization queue.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void run() throws DatabaseConnectionClosedException {
        start();
        cleanupSyncStates();
        while (network.isConnected()) {
            if (0 == updateSyncQueue()) {
                break;
            }
            syncDocuments();
            pauseIfNeeded();
            if (isCanceled()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.debug("DocumentsSyncProcess interrupted, exiting", e);
                break;
            }
        }
        cleanupSyncStates();
        if (network.isConnected()) {
            refreshQuotas();
        }
    }

    /**
     * This method is called when finishing this process execution, to mark the documents for which
     * one of the synchronization states remain for some reason on the "Running" state, as "Done"
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void cleanupSyncStates() throws DatabaseConnectionClosedException {
        currentSyncedDocument = null;
        restartCurrentDocumentSync = false;
        cleanupSyncState(SyncAction.Deletion);
        cleanupSyncState(SyncAction.Upload);
        cleanupSyncState(SyncAction.Download);
    }

    /**
     * Synchronized the quota information of all accounts for which at least 1 document was processed.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void refreshQuotas() throws DatabaseConnectionClosedException {
        for (long accountId : syncAccountsHistory) {
            pauseIfNeeded();
            if (isCanceled()) {
                break;
            }
            Account account = accounts.accountWithId(accountId);
            if (null!=account) {
                LOG.debug("Refreshing account quota for {}", account.storageText());
                try {
                    account.refreshQuota();
                } catch (RemoteException e) {
                    LOG.error("Failed to refresh account quota for {}", account.storageText(), e);
                }
            }
        }
    }

    /**
     * Marks the given {@code syncAction} as "Done" for the documents which documents {@code syncAction}
     * remains for some reason on the "Running" state.
     *
     * @param syncAction the {@code SyncAction} state to cleanup
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void cleanupSyncState(SyncAction syncAction) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> documentsList =
                encryptedDocuments.encryptedDocumentsWithSyncState(syncAction, State.Running);
        for (EncryptedDocument encryptedDocument : documentsList) {
            encryptedDocument.updateSyncState(syncAction, State.Planned);
        }
    }

    /**
     * Processes all the documents in the synchronization queue.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void syncDocuments() throws DatabaseConnectionClosedException {
        int syncsCount = 0;
        numDocumentsToSync = syncQueue.size();
        if (null != progressListener) {
            progressListener.onProgress(0, syncsCount);
            progressListener.onSetMax(0, numDocumentsToSync);
        }
        while (network.isConnected() && !syncQueue.isEmpty()) {
            pauseIfNeeded();
            if (isCanceled()) {
                break;
            }
            if (!restartCurrentDocumentSync) {
                currentSyncedDocument = syncQueue.poll();
            }
            restartCurrentDocumentSync = false;
            if (null!=currentSyncedDocument) {
                syncAccountsHistory.add(currentSyncedDocument.getBackStorageAccount().getId());
                syncDocument(currentSyncedDocument);
                syncsCount++;
                if (null != progressListener) {
                    progressListener.onProgress(0, syncsCount);
                }
            }
            if (!restartCurrentDocumentSync) {
                currentSyncedDocument = null;
            }
        }
    }

    /**
     * Processes the given {@code encryptedDocument}.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to process
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private boolean syncDocument(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        encryptedDocument.refresh();
        boolean result = false;
        switch (encryptedDocument.getSyncState(SyncAction.Deletion)) {
            case Planned:
            case Failed:
                result = syncDocument(SyncAction.Deletion, encryptedDocument);
                break;
            default:
                boolean plannedUpload = false;
                switch (encryptedDocument.getSyncState(SyncAction.Upload)) {
                    case Planned:
                    case Failed:
                        plannedUpload = true;
                        break;
                }

                boolean plannedDownload = false;
                switch (encryptedDocument.getSyncState(SyncAction.Download)) {
                    case Planned:
                    case Failed:
                        plannedDownload = true;
                        break;
                }

                try {
                    //if both sync states Upload and Download : determine which version is more recent.
                    if (plannedUpload && plannedDownload) {
                        if (null == encryptedDocument.getBackEntryId()) {
                            LOG.debug("Document {} conflict : upload", encryptedDocument.getDisplayName());
                            // cancel download
                            plannedDownload = false;
                            encryptedDocument.updateSyncState(SyncAction.Download, State.Done);
                        } else {
                            RemoteDocument remoteDocument = encryptedDocument.remoteDocument();
                            if (encryptedDocument.getBackEntryVersion() < remoteDocument.getVersion()) {
                                LOG.debug("Document {} conflict : download", encryptedDocument.getDisplayName());
                                // cancel upload
                                plannedUpload = false;
                                encryptedDocument.updateSyncState(SyncAction.Upload, State.Done);
                            } else  {
                                LOG.debug("Document {} conflict : upload", encryptedDocument.getDisplayName());
                                // cancel download
                                plannedDownload = false;
                                encryptedDocument.updateSyncState(SyncAction.Download, State.Done);
                            }
                        }
                    }
                } catch (StorageCryptException e) {
                    LOG.error("Failed to get document {} version", encryptedDocument.getDisplayName(), e);
                    LOG.debug("Document {} conflict : cannot decide", encryptedDocument.getDisplayName());
                    return false;
                }

                if (plannedUpload) {
                    if (syncDocument(SyncAction.Upload, encryptedDocument)) {
                        result = true;
                    }
                } else if (plannedDownload) {
                    if (syncDocument(SyncAction.Download, encryptedDocument)) {
                        result = true;
                    }
                }
                break;
        }
        return result;
    }

    /**
     * Processes the given {@code syncAction} for the given {@code encryptedDocument}.
     *
     * @param syncAction        the {@code SyncAction} to process
     * @param encryptedDocument the {@code EncryptedDocument} to process
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private boolean syncDocument(SyncAction syncAction, EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        pauseIfNeeded();
        if (isCanceled()) {
            return false;
        }

        if (network.isNetworkReadyForSyncAction(syncAction)) {
            if (null!=progressListener) {
                progressListener.onMessage(0, syncAction.name());
                try {
                    progressListener.onMessage(1, encryptedDocument.logicalPath());
                } catch (ParentNotFoundException e) {
                    LOG.error("Database is closed", e);
                    progressListener.onMessage(1, encryptedDocument.getDisplayName());
                }
            }
            if (null!= syncActionListener) {
                syncActionListener.onSyncActionStart(syncAction, encryptedDocument);
            }
            switch (syncAction) {
                case Deletion:
                    if (null == encryptedDocument.getBackEntryId()) {
                        encryptedDocument.delete();
                        return true;
                    } else {
                        return deleteDocument(encryptedDocument);
                    }
                case Upload:
                    if (encryptedDocument.getSyncState(SyncAction.Deletion) == State.Done) {
                        return uploadDocument(encryptedDocument);
                    }
                    break;
                case Download:
                    if (encryptedDocument.getSyncState(SyncAction.Deletion) == State.Done) {
                        return downloadDocument(encryptedDocument);
                    }
                    break;
            }
        }
        return false;
    }

    /**
     * Deletes the remote document referenced by the given {@code encryptedDocument}.
     *
     * @param encryptedDocument the {@code EncryptedDocument} whose remote document to delete
     * @return true if the remote document was successfully deleted
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private boolean deleteDocument(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        LOG.trace("Deleting document {}", encryptedDocument.getDisplayName());
        List<EncryptedDocument> children = encryptedDocument.children(false, OrderBy.NameAsc);
        if (!children.isEmpty()) {
            for (EncryptedDocument child : children) {
                if (null!=child && child.getSyncState(SyncAction.Deletion)!= State.Done) {
                    return false;
                }
            }
        }

        if (null==encryptedDocument.getBackEntryId()) {
            LOG.debug("Document {} remote id is null, try to get it", encryptedDocument.getDisplayName());
            try {
                encryptedDocument.tryToRecoverBackEntryId();
            } catch (StorageCryptException e) {
                LOG.error("Failed to recover remote id", e);
                return false;
            }
        }

        if (null==encryptedDocument.getBackEntryId()) {
            LOG.debug("Document {} remote id is null, ignore it", encryptedDocument.getDisplayName());
            encryptedDocument.updateSyncState(SyncAction.Deletion, State.Failed);
            return false;
        }

        LOG.trace("Deleting remote file : {}", encryptedDocument.getDisplayName());
        if (null!=progressListener) {
            progressListener.onSetMax(1, 1);
            progressListener.onProgress(1, 0);
        }
        encryptedDocument.updateSyncState(SyncAction.Deletion, State.Running);
        if (null != syncActionListener) {
            syncActionListener.onDocumentChanged(encryptedDocument);
        }
        try {
            encryptedDocument.deleteRemote();
            if (null != progressListener) {
                progressListener.onProgress(1, 1);
            }
            return true;
        } catch (StorageCryptException e) {
            LOG.error("Error while deleting remote file", e);
            encryptedDocument.updateSyncState(SyncAction.Deletion, State.Failed);
        }
        return false;
    }

    /**
     * Downloads the given {@code encryptedDocument}.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to download
     * @return true if the remote document was successfully downloaded
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private boolean downloadDocument(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        LOG.trace("Downloading document {}", encryptedDocument.getDisplayName());
        EncryptedDocument parentEncryptedDocument = encryptedDocuments.encryptedDocumentWithId(encryptedDocument.getParentId());
        if (null== parentEncryptedDocument || parentEncryptedDocument.getSyncState(SyncAction.Download)!= State.Done) {
            return false;
        }

        if (null==encryptedDocument.getBackEntryId()) {
            LOG.debug("Document {} remote id is null, try to get it", encryptedDocument.getDisplayName());
            try {
                encryptedDocument.tryToRecoverBackEntryId();
            } catch (StorageCryptException e) {
                LOG.error("Failed to recover remote id", e);
                return false;
            }
        }

        if (null==encryptedDocument.getBackEntryId()) {
            LOG.debug("Document {} remote id is null, ignore it", encryptedDocument.getDisplayName());
            encryptedDocument.updateSyncState(SyncAction.Download, State.Failed);
            return false;
        }

        LOG.trace("Downloading remote file : {}", encryptedDocument.getDisplayName());
        if (null!=progressListener) {
            progressListener.onSetMax(1, (int) encryptedDocument.getSize());
            progressListener.onProgress(1, 0);
        }

        encryptedDocument.updateSyncState(SyncAction.Download, State.Running);
        if (null != syncActionListener) {
            syncActionListener.onDocumentChanged(encryptedDocument);
        }
        if (encryptedDocument.isFolder()) {
            encryptedDocument.file().mkdirs();
            encryptedDocument.updateLocalModificationTime(System.currentTimeMillis());
            encryptedDocument.updateSyncState(SyncAction.Download, State.Done);
            return true;
        } else {
            try {
                encryptedDocument.download(new ProcessProgressAdapter() {
                    @Override
                    public void onProgress(int i, int progress) {
                        if (null != progressListener) {
                            if (0 == i) {
                                progressListener.onProgress(1, progress);
                            }
                        }
                    }

                    @Override
                    public void onSetMax(int i, int max) {
                        if (null != progressListener) {
                            if (0 == i) {
                                progressListener.onSetMax(1, max);
                            }
                        }
                    }

                    @Override
                    public boolean isCanceled() {
                        return restartCurrentDocumentSync || DocumentsSyncProcess.this.isCanceled();
                    }

                    @Override
                    public void pauseIfNeeded() {
                        DocumentsSyncProcess.this.pauseIfNeeded();
                    }
                });
                if (null != progressListener) {
                    progressListener.onProgress(1, (int) encryptedDocument.getSize());
                }
                encryptedDocument.updateSyncState(SyncAction.Download, State.Done);
                return true;
            } catch (StorageCryptException e) {
                LOG.error("Error while downloading remote file", e);
                encryptedDocument.updateSyncState(SyncAction.Download, State.Failed);
            }
        }
        return false;
    }

    /**
     * Uploads the given {@code encryptedDocument}.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to upload
     * @return true if the remote document was successfully uploaded
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private boolean uploadDocument(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException {
        LOG.trace("Uploading document {}", encryptedDocument.getDisplayName());
        EncryptedDocument parentEncryptedDocument = encryptedDocument.parent();
        if (null== parentEncryptedDocument || parentEncryptedDocument.getSyncState(SyncAction.Upload)!= State.Done) {
            return false;
        }

        LOG.trace("Uploading remote file : {}", encryptedDocument.getDisplayName());
        if (null!=progressListener) {
            progressListener.onSetMax(1, (int) encryptedDocument.getSize());
            progressListener.onProgress(1, 0);
        }

        State previousState = encryptedDocument.getSyncState(SyncAction.Upload);

        encryptedDocument.updateSyncState(SyncAction.Upload, State.Running);
        if (null != syncActionListener) {
            syncActionListener.onDocumentChanged(encryptedDocument);
        }
        try {
            ProcessProgressListener uploadProgressListener = new ProcessProgressAdapter() {
                @Override
                public void onProgress(int i, int progress) {
                    if (null != progressListener) {
                        if (0 == i) {
                            progressListener.onProgress(1, progress);
                        }
                    }
                }

                @Override
                public void onSetMax(int i, int max) {
                    if (null != progressListener) {
                        if (0 == i) {
                            progressListener.onSetMax(1, max);
                        }
                    }
                }

                @Override
                public boolean isCanceled() {
                    return restartCurrentDocumentSync || DocumentsSyncProcess.this.isCanceled();
                }

                @Override
                public void pauseIfNeeded() {
                    DocumentsSyncProcess.this.pauseIfNeeded();
                }
            };
            if (encryptedDocument.isFolder()) {
                encryptedDocument.uploadNew(uploadProgressListener);
            } else {
                if (null != encryptedDocument.getBackEntryId()) {
                    encryptedDocument.upload(uploadProgressListener);
                } else {
                    if (previousState != State.Failed) {
                        encryptedDocument.uploadNew(uploadProgressListener);
                    } else {
                        // try to retrieve remote document id if partially created
                        RemoteDocument parentRemoteDocument = parentEncryptedDocument.remoteDocument();
                        try {
                            RemoteDocument existingDocument = parentRemoteDocument.childDocument(
                                    encryptedDocument.getDisplayName());
                            // if the remote document was already created, get its ID
                            encryptedDocument.updateBackEntryId(existingDocument.getId());
                            // then upload content
                            encryptedDocument.upload(uploadProgressListener);
                        } catch (RemoteException ce) {
                            if (ce.getReason() == RemoteException.Reason.NotFound) {
                                //if the remote document doesn't exist, try to create it again
                                encryptedDocument.uploadNew(uploadProgressListener);
                            } else {
                                throw new StorageCryptException("Error while checking previously failed remote file",
                                        StorageCryptException.Reason.FileNotFound, ce);
                            }
                        }
                    }
                }
            }
            encryptedDocument.updateSyncState(SyncAction.Upload, State.Done);
            return true;
        } catch (StorageCryptException e) {
            LOG.error("Error while uploading remote file", e);
            encryptedDocument.updateSyncState(SyncAction.Upload, State.Failed);
        }
        return false;
    }
}
