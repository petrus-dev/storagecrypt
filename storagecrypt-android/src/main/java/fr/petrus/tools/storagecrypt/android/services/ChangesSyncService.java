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

package fr.petrus.tools.storagecrypt.android.services;

import android.os.Bundle;
import android.util.Log;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.ChangesSyncProcess;
import fr.petrus.lib.core.result.ProgressAdapter;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.android.events.ChangesSyncDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.ChangesSyncServiceEvent;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.tasks.ChangesSyncTask;

/**
 * The {@code ThreadService} which handles remote changes synchronization.
 *
 * @see ChangesSyncTask
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class ChangesSyncService extends ThreadService<ChangesSyncService> {
    private static final String TAG = "ChangesSyncSvc";

    /**
     * The argument used to pass the ID of the account to sync.
     */
    public static final String ACCOUNT_ID = "accountId";

    /**
     * The argument used to pass the IDs of the accounts to sync.
     */
    public static final String ACCOUNT_IDS = "accountIds";

    /**
     * The argument used to pass tell the service whether to display the results after completion.
     */
    public static final String SHOW_RESULT = "showResult";

    /**
     * The {@code COMMAND} value to sync an account.
     */
    public static final int COMMAND_SYNC_ACCOUNT = 10;

    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private TextI18n textI18n = null;
    private Network network = null;
    private Accounts accounts = null;
    private EncryptedDocuments encryptedDocuments = null;
    private ChangesSyncProcess changesSyncProcess = null;

    /**
     * Creates a new {@code ChangesSyncService} instance.
     */
    public ChangesSyncService() {
        super(TAG, AndroidConstants.MAIN_ACTIVITY.CHANGES_SYNC_PROGRESS_DIALOG);
    }

    @Override
    public void initDependencies() {
        super.initDependencies();
        crypto = appContext.getCrypto();
        keyManager = appContext.getKeyManager();
        textI18n = appContext.getTextI18n();
        network = appContext.getNetwork();
        accounts = appContext.getAccounts();
        encryptedDocuments = appContext.getEncryptedDocuments();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final TaskProgressEvent taskProgressEvent = new TaskProgressEvent(
                AndroidConstants.MAIN_ACTIVITY.CHANGES_SYNC_PROGRESS_DIALOG, 2);
        changesSyncProcess = new ChangesSyncProcess(crypto, keyManager, textI18n, network,
                accounts, encryptedDocuments);
        changesSyncProcess.setProgressListener(new ProgressAdapter() {
            @Override
            public void onMessage(int i, String message) {
                taskProgressEvent.setMessage(i, message).postSticky();
            }

            @Override
            public void onProgress(int i, int progress) {
                taskProgressEvent.setProgress(i, progress).postSticky();
            }

            @Override
            public void onSetMax(int i, int max) {
                taskProgressEvent.setMax(i, max).postSticky();
            }
        });
        changesSyncProcess.setSyncActionListener(new ChangesSyncProcess.SyncActionListener() {
            @Override
            public void onChangesSyncDone(EncryptedDocument rootEncryptedDocument) {
                DocumentListChangeEvent.postSticky();
            }
        });
    }

    @Override
    protected void runIntent(int command, Bundle parameters) {
        setProcess(changesSyncProcess);
        try {
            boolean showResults = true;
            if (null != parameters) {
                showResults = parameters.getBoolean(SHOW_RESULT, true);
            }
            switch (command) {
                case COMMAND_START:
                    ChangesSyncProcess.syncAll(accounts);
                    break;
                case COMMAND_SYNC_ACCOUNT:
                    if (null != parameters) {
                        Account account = accounts.accountWithId(parameters.getLong(ACCOUNT_ID, -1));
                        if (null != account) {
                            ChangesSyncProcess.sync(account);
                        }
                        long[] accountIds = parameters.getLongArray(ACCOUNT_IDS);
                        if (null != accountIds) {
                            ChangesSyncProcess.sync(accounts.accountsWithIds(accountIds));
                        }
                    }
                    break;
            }
            new ChangesSyncServiceEvent(State.Running).postSticky();
            if (showResults) {
                changesSyncProcess.showResults();
            }
            changesSyncProcess.run();
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }
        new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.CHANGES_SYNC_PROGRESS_DIALOG).postSticky();
        new ChangesSyncServiceEvent(State.Done).postSticky();
        new ChangesSyncDoneEvent(changesSyncProcess.getResults()).postSticky();
        DocumentListChangeEvent.postSticky();
        setProcess(null);
    }

    @Override
    protected void refreshIntent(int command, Bundle parameters) {
        boolean showResults = true;
        if (null != parameters) {
            showResults = parameters.getBoolean(SHOW_RESULT, true);
        }
        if (isRunning()) {
            if (showResults) {
                changesSyncProcess.showResults();
            }
            try {
                switch (command) {
                    case COMMAND_START:
                        ChangesSyncProcess.syncAll(accounts);
                        break;
                    case COMMAND_SYNC_ACCOUNT:
                        if (null != parameters) {
                            Account account = accounts.accountWithId(parameters.getLong(ACCOUNT_ID, -1));
                            if (null != account) {
                                ChangesSyncProcess.sync(account);
                            }
                            long[] accountIds = parameters.getLongArray(ACCOUNT_IDS);
                            if (null != accountIds) {
                                ChangesSyncProcess.sync(accounts.accountsWithIds(accountIds));
                            }
                        }
                        break;
                }
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
            }
        }
    }
}
