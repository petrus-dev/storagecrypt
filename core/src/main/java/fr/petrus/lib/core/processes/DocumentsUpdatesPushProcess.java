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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import fr.petrus.lib.core.ParentNotFoundException;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.FailedResult;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The {@code Process} which sends local documents modifications to the remote account.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DocumentsUpdatesPushProcess extends AbstractProcess<DocumentsUpdatesPushProcess.Results> {
    private static Logger LOG = LoggerFactory.getLogger(DocumentsUpdatesPushProcess.class);

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     */
    public static class Results extends BaseProcessResults<EncryptedDocument, EncryptedDocument> {

        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super (textI18n, true, false, true);
        }

        @Override
        public int getResultsColumnsCount(ResultsType resultsType) {
            if (null!=resultsType) {
                switch (resultsType) {
                    case Success:
                        return 1;
                    case Skipped:
                        return 0;
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
                            result = new String[] { success.get(i).logicalPath() };
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            result = new String[] { success.get(i).getDisplayName() };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[] { success.get(i).getDisplayName() };
                        }
                        break;
                    case Skipped:
                        result = new String[0];
                        break;
                    case Errors:
                        try {
                            result = new String[]{
                                    errors.get(i).getElement().logicalPath(),
                                    textI18n.getExceptionDescription(errors.get(i).getException())
                            };
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            result = new String[]{
                                    errors.get(i).getElement().getDisplayName(),
                                    textI18n.getExceptionDescription(errors.get(i).getException())
                            };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[]{
                                    errors.get(i).getElement().getDisplayName(),
                                    textI18n.getExceptionDescription(errors.get(i).getException())
                            };
                        }
                        break;
                    default:
                        result = new String[0];
                }
            }
            return result;
        }
    }

    private Network network;
    private ConcurrentLinkedQueue<EncryptedDocument> updatesPushRoots = new ConcurrentLinkedQueue<>();
    private List<EncryptedDocument> successfulPushedUpdates = new ArrayList<>();
    private LinkedHashMap<Long, FailedResult<EncryptedDocument>> failedUpdates = new LinkedHashMap<>();
    private ProgressListener progressListener;
    private int numRootsToProcess;

    /**
     * Creates a new {@code DocumentsPushUpdatesProcess}, providing its dependencies.
     *
     * @param textI18n  a {@code TextI18n} instance
     * @param network            a {@code Network} instance
     */
    public DocumentsUpdatesPushProcess(TextI18n textI18n, Network network) {
        super(new Results(textI18n));
        this.network = network;
        progressListener = null;
        numRootsToProcess = 0;
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
     * Enqueues the given {@code rootFolder} in the list of folders to push updates for.
     *
     * @param rootFolder the root folder which updates will be pushed
     */
    public void pushUpdates(EncryptedDocument rootFolder) {
        updatesPushRoots.offer(rootFolder);
        numRootsToProcess++;
    }

    /**
     * Enqueues the given {@code rootFolders} in the list of folders to push updates for.
     *
     * @param rootFolders the root folders which updates will be pushed
     */
    public void pushUpdates(List<EncryptedDocument> rootFolders) {
        for (EncryptedDocument rootFolder : rootFolders) {
            updatesPushRoots.offer(rootFolder);
        }
        numRootsToProcess += rootFolders.size();
    }

    /**
     * Sends the modifications of the documents contained in the queue to the remote accounts.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void run() throws DatabaseConnectionClosedException {
        try {
            start();
            while (!updatesPushRoots.isEmpty()) {
                if (null != progressListener) {
                    progressListener.onSetMax(0, numRootsToProcess);
                    progressListener.onProgress(0, numRootsToProcess - updatesPushRoots.size());
                }
                EncryptedDocument updatesPushRoot = updatesPushRoots.poll();
                List<EncryptedDocument> encryptedDocuments = updatesPushRoot.unfoldAsList(true);
                encryptedDocuments.remove(0);
                if (null != progressListener) {
                    try {
                        progressListener.onMessage(1, updatesPushRoot.logicalPath());
                    } catch (ParentNotFoundException e) {
                        LOG.error("Database is closed", e);
                        progressListener.onMessage(1, updatesPushRoot.storageText());
                    }
                    progressListener.onSetMax(1, encryptedDocuments.size());
                }
                for (int i = 0; i < encryptedDocuments.size(); i++) {
                    pauseIfNeeded();
                    if (!network.isConnected() || isCanceled()) {
                        return;
                    }
                    EncryptedDocument encryptedDocument = encryptedDocuments.get(i);
                    if (null != progressListener) {
                        try {
                            progressListener.onMessage(1, encryptedDocument.logicalPath());
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            progressListener.onMessage(1, encryptedDocument.getDisplayName());
                        }
                        progressListener.onProgress(1, i);
                    }

                    RemoteDocument document = null;
                    try {
                        document = encryptedDocument.remoteDocument();
                    } catch (StorageCryptException e) {
                        RemoteException remoteException = e.getRemoteException();
                        if (null == remoteException || remoteException.getReason() != RemoteException.Reason.NotFound) {
                            LOG.error("Failed to access remote document {}", encryptedDocument.getDisplayName(), e);
                            failedUpdates.put(encryptedDocument.getId(),
                                    new FailedResult<>(encryptedDocument, e));
                            return;
                        }
                    }
                    if (null == document) {
                        encryptedDocument.updateSyncState(SyncAction.Upload, State.Planned);
                    } else {
                        if (document.getVersion() < encryptedDocument.getBackEntryVersion()) {
                            encryptedDocument.updateSyncState(SyncAction.Upload, State.Planned);
                        } else if (document.getVersion() > encryptedDocument.getBackEntryVersion()) {
                            encryptedDocument.updateSyncState(SyncAction.Download, State.Planned);
                        }
                    }
                    successfulPushedUpdates.add(encryptedDocument);
                }
            }
        } finally {
            getResults().addResults(successfulPushedUpdates, failedUpdates.values());
        }
    }
}
