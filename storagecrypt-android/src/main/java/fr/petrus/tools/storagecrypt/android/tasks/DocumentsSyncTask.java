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

package fr.petrus.tools.storagecrypt.android.tasks;

import android.content.Context;
import android.os.Bundle;

import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.tools.storagecrypt.android.services.DocumentsSyncService;

/**
 * The {@code Task} which synchronizes the local documents on remote storages.
 *
 * @see DocumentsSyncService
 *
 * @author Pierre Sagne
 * @since 13.09.2015
 */
public class DocumentsSyncTask extends ServiceTask<DocumentsSyncService> {

    /**
     * Creates a new {@code DocumentsSyncTask} instance.
     *
     * @param appContext the application context
     * @param context the Android context
     */
    public DocumentsSyncTask(AppContext appContext, Context context) {
        super(appContext, context, DocumentsSyncService.class);
    }

    @Override
    public void start() {
        if (appContext.getCloudAppKeys().found()) {
            super.start();
        }
    }

    /**
     * Starts the service in the background if it is not currently running, or adds the given
     * {@code encryptedDocument} to the running process queue.
     *
     * @param encryptedDocument the document to be synchronized
     */
    public void syncDocument(EncryptedDocument encryptedDocument) {
        if (appContext.getCloudAppKeys().found()) {
            Bundle parameters = new Bundle();
            parameters.putLong(DocumentsSyncService.DOCUMENT_ID, encryptedDocument.getId());
            start(DocumentsSyncService.COMMAND_ENQUEUE_DOCUMENT, parameters);
        }
    }

    /**
     * Starts the service in the background if it is not currently running, or adds the given
     * {@code encryptedDocuments} to the running process queue.
     *
     * @param encryptedDocuments the documents to be synchronized
     */
    public void syncDocuments(List<EncryptedDocument> encryptedDocuments) {
        if (appContext.getCloudAppKeys().found()) {
            Bundle parameters = new Bundle();
            parameters.putLongArray(DocumentsSyncService.DOCUMENT_IDS,
                    EncryptedDocuments.getIdsArray(encryptedDocuments));
            start(DocumentsSyncService.COMMAND_ENQUEUE_DOCUMENTS, parameters);
        }
    }

    /**
     * Restarts the synchronization of the currently processed document if it is the given
     * {@code encryptedDocument}.
     *
     * @param encryptedDocument the encrypted document to restart synchronizing
     */
    public void restartCurrentSync(EncryptedDocument encryptedDocument) {
        if (appContext.getCloudAppKeys().found()) {
            Bundle parameters = new Bundle();
            parameters.putLong(DocumentsSyncService.DOCUMENT_ID, encryptedDocument.getId());
            start(DocumentsSyncService.COMMAND_RESTART_CURRENT_SYNC, parameters);
        }
    }
}
