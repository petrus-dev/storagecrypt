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

import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.DocumentsMoveProcess;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.AndroidConstants;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.DocumentsMoveDoneEvent;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.fragments.dialog.ProgressDialogFragment;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsMoveTask;

/**
 * The {@code ThreadService} which handles documents move.
 *
 * @see DocumentsMoveTask
 *
 * @author Pierre Sagne
 * @since 12.05.2017
 */
public class DocumentsMoveService extends ThreadService<DocumentsMoveService> {
    private static final String TAG = "DocumentsMoveSvc";

    /**
     * The argument used to pass the paths of the documents to move.
     */
    public static final String SRC_DOCUMENT_IDS = "srcDocumentIds";

    /**
     * The argument used to pass the ID of the destination folder where to encrypt the files.
     */
    public static final String DST_FOLDER_ID = "dstFolderId";

    private DocumentsMoveProcess documentsMoveProcess = null;

    /**
     * Creates a new {@code DocumentsMoveService} instance.
     */
    public DocumentsMoveService() {
        super(TAG, AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_MOVE_PROGRESS_DIALOG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final TaskProgressEvent taskProgressEvent = new TaskProgressEvent(
                AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_MOVE_PROGRESS_DIALOG, 2);
        documentsMoveProcess = new DocumentsMoveProcess(
                appContext.getCrypto(),
                appContext.getKeyManager(),
                appContext.getTextI18n(),
                appContext.getFileSystem());
        documentsMoveProcess.setProgressListener(new ProgressListener() {
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
    }

    @Override
    protected void runIntent(int command, Bundle parameters) {
        setProcess(documentsMoveProcess);
        if (null!=parameters) {
            try {
                long[] srcDocumentIds = parameters.getLongArray(SRC_DOCUMENT_IDS);
                long dstFolderId = parameters.getLong(DST_FOLDER_ID, -1);

                if (null!=srcDocumentIds && srcDocumentIds.length>0 && -1!=dstFolderId) {
                    final List<EncryptedDocument> srcDocuments = new ArrayList<>();
                    for (long documentId : srcDocumentIds) {
                        EncryptedDocument encryptedDocument = appContext
                                .getEncryptedDocuments().encryptedDocumentWithId(documentId);
                        if (null != encryptedDocument) {
                            srcDocuments.add(encryptedDocument);
                        }
                    }

                    EncryptedDocument dstFolder = appContext
                            .getEncryptedDocuments().encryptedDocumentWithId(dstFolderId);

                    new ShowDialogEvent(new ProgressDialogFragment.Parameters()
                            .setDialogId(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_MOVE_PROGRESS_DIALOG)
                            .setTitle(getString(R.string.progress_text_moving_documents))
                            .setCancelButton(true).setPauseButton(true)
                            .setProgresses(new Progress(false), new Progress(false))).postSticky();

                    documentsMoveProcess.moveDocuments(srcDocuments, dstFolder);
                }
                new DismissProgressDialogEvent(AndroidConstants.MAIN_ACTIVITY.DOCUMENTS_MOVE_PROGRESS_DIALOG)
                        .postSticky();
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
            }
            new DocumentsMoveDoneEvent(documentsMoveProcess.getResults()).postSticky();
        }
        setProcess(null);
    }
}
