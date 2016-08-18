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

package fr.petrus.tools.storagecrypt.desktop.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.ChangesSyncProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

/**
 * The {@code Task} which handles remote changes synchronization.
 *
 * @see ChangesSyncProcess
 *
 * @author Pierre Sagne
 * @since 20.08.2015
 */
public class ChangesSyncTask extends ProcessTask {

    private static Logger LOG = LoggerFactory.getLogger(ChangesSyncTask.class);

    /**
     * This class is used to report this {@code Task} state.
     */
    public static class SyncServiceState {
        /**
         * The progress among all the accounts.
         */
        public Progress accountsProgress = new Progress(0, 0);

        /**
         * The progress among the changes of the current account.
         */
        public Progress accountChangesProgress = new Progress(0, 0);

        /**
         * The name of the current account being processed.
         */
        public String currentAccountName = null;

        /**
         * The name of the current document being processed.
         */
        public String currentDocumentName = null;
    }

    private CloudAppKeys cloudAppKeys = null;
    private SyncServiceState syncState = new SyncServiceState();

    /**
     * Creates a new {@code ChangesSyncTask} instance.
     *
     * @param appWindow the application window
     */
    public ChangesSyncTask(AppWindow appWindow) {
        super(appWindow);
        cloudAppKeys = appContext.getCloudAppKeys();
    }

    /**
     * Marks all accounts for synchronization, then starts the synchronization process in the background.
     *
     * @param showResult if set to true, the results window will be shown when the process is done
     */
    public void syncAll(boolean showResult) throws DatabaseConnectionClosedException {
        if (cloudAppKeys.found()) {
            ChangesSyncProcess.syncAll(appContext.getAccounts());
            start(showResult);
        }
    }

    /**
     * Marks the given {@code account} for synchronization, then starts the synchronization process in
     * the background.
     *
     * @param account    the account to synchronize
     * @param showResult if set to true, the results window will be shown when the process is done
     */
    public void sync(Account account, boolean showResult)
            throws DatabaseConnectionClosedException {
        if (cloudAppKeys.found()) {
            ChangesSyncProcess.sync(account);
            start(showResult);
        }
    }

    /**
     * Marks the given {@code accounts} for synchronization, then starts the synchronization process
     * in the background.
     *
     * @param accounts   the accounts to synchronize
     * @param showResult if set to true, the results window will be shown when the process is done
     */
    public void sync(List<Account> accounts, boolean showResult)
            throws DatabaseConnectionClosedException {
        if (cloudAppKeys.found()) {
            for (Account account : accounts) {
                ChangesSyncProcess.sync(account);
            }
            start(showResult);
        }
    }

    /**
     * Starts the synchronization process in the background.
     *
     * @param showResult if set to true, the results window will be shown when the process is done
     */
    private void start(boolean showResult) {
        if (!hasProcess()) {
            final ChangesSyncProcess changesSyncProcess = new ChangesSyncProcess(
                    appContext.getCrypto(),
                    appContext.getKeyManager(),
                    appContext.getTextI18n(),
                    appContext.getNetwork(),
                    appContext.getAccounts(),
                    appContext.getEncryptedDocuments());
            if (showResult) {
                changesSyncProcess.showResults();
            }
            setProcess(changesSyncProcess);
            changesSyncProcess.setProgressListener(new ProgressListener() {
                @Override
                public void onMessage(int i, String message) {
                    switch (i) {
                        case 0:
                            syncState.currentAccountName = message;
                            break;
                        case 1:
                            syncState.currentDocumentName = message;
                            break;
                    }
                    appWindow.updateChangesSyncProgress(syncState);
                }

                @Override
                public void onProgress(int i, int progress) {
                    switch (i) {
                        case 0:
                            syncState.accountsProgress.setProgress(progress);
                            break;
                        case 1:
                            syncState.accountChangesProgress.setProgress(progress);
                    }
                    appWindow.updateChangesSyncProgress(syncState);
                }

                @Override
                public void onSetMax(int i, int max) {
                    switch (i) {
                        case 0:
                            syncState.accountsProgress.setMax(max);
                            break;
                        case 1:
                            syncState.accountChangesProgress.setMax(max);
                    }
                    appWindow.updateChangesSyncProgress(syncState);
                }
            });

            changesSyncProcess.setSyncActionListener(new ChangesSyncProcess.SyncActionListener() {
                @Override
                public void onChangesSyncDone(EncryptedDocument rootEncryptedDocument) {
                    appWindow.update(true);
                }
            });

            new Thread() {
                @Override
                public void run() {
                    try {
                        changesSyncProcess.run();
                    } catch (DatabaseConnectionClosedException e) {
                        LOG.error("Failed to decrypt file", e);
                    }
                    appWindow.resetChangesSyncProgress();
                    appWindow.onChangesSyncDone(changesSyncProcess.getResults());
                    setProcess(null);
                    LOG.debug("Exiting changes sync thread");
                }
            }.start();
        } else {
            if (showResult) {
                ChangesSyncProcess changesSyncProcess = (ChangesSyncProcess) getProcess();
                changesSyncProcess.showResults();
            }
        }
    }
}
