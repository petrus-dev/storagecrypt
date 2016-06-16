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

import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.tools.storagecrypt.android.services.DocumentsUpdatesPushService;

/**
 * The {@code Task} which sends local documents modifications to the remote account.
 *
 * @see DocumentsUpdatesPushService
 *
 * @author Pierre Sagne
 * @since 13.09.2015
 */
public class DocumentsUpdatesPushTask extends ServiceTask<DocumentsUpdatesPushService> {
    /**
     * Creates a new {@code DocumentsUpdatesPushTask} instance.
     *
     * @param appContext the application context
     * @param context the Android context
     */
    public DocumentsUpdatesPushTask(AppContext appContext, Context context) {
        super(appContext, context, DocumentsUpdatesPushService.class);
    }

    /**
     * Enqueues {@code EncryptedDocument} with the given {@code rootId} in the list of folders for
     * which to push updates then starts the updates push service in the background if it is not
     * currently running.
     *
     * @param rootId the database ID of the folder which will be scanned on the local filesystem
     *               and which children updates will be pushed to the remote storage
     */
    public void pushUpdates(long rootId) {
        if (appContext.getCloudAppKeys().found()) {
            Bundle parameters = new Bundle();
            parameters.putLong(DocumentsUpdatesPushService.ROOT_ID, rootId);
            start(DocumentsUpdatesPushService.COMMAND_START, parameters);
        }
    }
}
