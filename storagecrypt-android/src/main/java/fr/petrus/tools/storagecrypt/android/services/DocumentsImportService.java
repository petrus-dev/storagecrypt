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

import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.lib.core.processes.DocumentsImportProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.lib.core.Progress;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.DocumentsImportDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ProgressDialogFragment;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsImportTask;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;

/**
 * The {@code ThreadService} which imports documents from the filesystem or from the remote account.
 *
 * @see DocumentsImportTask
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DocumentsImportService extends ThreadService<DocumentsImportService> {
    private static final String TAG = "DocumentsImportSvc";

    /**
     * The argument used to pass the ID of the "top level" folder to import the documents.
     */
    public static final String ROOT_ID = "rootId";

    /**
     * The argument used to pass the IDs of the "top level" folders to import the documents.
     */
    public static final String ROOT_IDS = "rootIds";

    private String storageName = null;
    private DocumentsImportProcess documentsImportProcess = null;

    /**
     * Creates a new {@code DocumentsImportService} instance.
     */
    public DocumentsImportService() {
        super(TAG, AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_IMPORT_PROGRESS_DIALOG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final TaskProgressEvent progressEvent = new TaskProgressEvent(
                AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_IMPORT_PROGRESS_DIALOG, 2);
        documentsImportProcess = new DocumentsImportProcess(
                appContext.getCrypto(),
                appContext.getKeyManager(),
                appContext.getTextI18n(),
                appContext.getAccounts(),
                appContext.getEncryptedDocuments());
        documentsImportProcess.setProgressListener(new ProgressListener() {
            @Override
            public void onMessage(int i, String message) {
                switch (i) {
                    case 0:
                        storageName = message;
                        break;
                    case 1:
                        progressEvent.setMessage(storageName + " : " + message).postSticky();
                        break;
                }
            }

            @Override
            public void onProgress(int i, int progress) {
                progressEvent.setProgress(i, progress).postSticky();
            }

            @Override
            public void onSetMax(int i, int max) {
                progressEvent.setMax(i, max).postSticky();
            }
        });
    }

    @Override
    protected void runIntent(int command, Bundle parameters) {
        setProcess(documentsImportProcess);
        if (null!=parameters) {
            try {
                long importRootId = parameters.getLong(ROOT_ID, -1);
                long[] importRootIds = parameters.getLongArray(ROOT_IDS);
                if (importRootId > 0) {
                    importDocuments(importRootId);
                } else if (null!=importRootIds) {
                    importDocuments(importRootIds);
                }
                appContext.getTask(DocumentsSyncTask.class).start();
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
            } catch (TaskCreationException e) {
                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
            }
        }
        setProcess(null);
    }

    private void importDocuments(long importRootId) throws DatabaseConnectionClosedException {
        EncryptedDocument importRoot = appContext.getEncryptedDocuments()
                .encryptedDocumentWithId(importRootId);

        if (null!= importRoot) {
            new ShowDialogEvent(new ProgressDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_IMPORT_PROGRESS_DIALOG)
                    .setTitle(getString(R.string.progress_text_importing_documents))
                    .setMessage(getString(R.string.progress_message_importing_documents,
                            importRoot.storageText()))
                    .setCancelButton(true).setPauseButton(true)
                    .setProgresses(new Progress(false), new Progress(false))).postSticky();
            documentsImportProcess.importDocuments(importRoot);
            documentsImportProcess.run();
            DocumentListChangeEvent.postSticky();
            new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_IMPORT_PROGRESS_DIALOG)
                    .postSticky();
        }
        new DocumentsImportDoneEvent(documentsImportProcess.getResults()).postSticky();
    }

    private void importDocuments(long[] importRootIds) throws DatabaseConnectionClosedException {
        List<EncryptedDocument> importRoots = appContext.getEncryptedDocuments()
                .encryptedDocumentsWithIds(importRootIds);

        if (null!= importRoots && !importRoots.isEmpty()) {
            new ShowDialogEvent(new ProgressDialogFragment.Parameters()
                    .setDialogId(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_IMPORT_PROGRESS_DIALOG)
                    .setTitle(getString(R.string.progress_text_importing_documents))
                    .setCancelButton(true).setPauseButton(true)
                    .setProgresses(new Progress(false), new Progress(false))).postSticky();
            documentsImportProcess.importDocuments(importRoots);
            documentsImportProcess.run();
            DocumentListChangeEvent.postSticky();
            new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_IMPORT_PROGRESS_DIALOG)
                    .postSticky();
        }
        new DocumentsImportDoneEvent(documentsImportProcess.getResults()).postSticky();
    }
}
