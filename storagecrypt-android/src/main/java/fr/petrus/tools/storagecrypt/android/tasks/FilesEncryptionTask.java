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
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.tools.storagecrypt.android.services.FilesEncryptionService;

/**
 * The {@code Task} which handles files encryption.
 *
 * @see FilesEncryptionService
 *
 * @author Pierre Sagne
 * @since 13.09.2015
 */
public class FilesEncryptionTask extends ServiceTask<FilesEncryptionService> {
    /**
     * Creates a new {@code FilesEncryptionTask} instance.
     *
     * @param appContext the application context
     * @param context the Android context
     */
    public FilesEncryptionTask(AppContext appContext, Context context) {
        super(appContext, context, FilesEncryptionService.class);
    }

    /**
     * Starts the encryption service in the background for the files with the given {@code srcFileUris}.
     *
     * @param srcFileUris  the URis of the files to encrypt
     * @param dstFolder    the destination folder where to store the encrypted files
     * @param dstKeyAlias  the alias of the key to encrypt the files with
     */
    public void encrypt(List<Uri> srcFileUris, EncryptedDocument dstFolder, String dstKeyAlias) {
        Bundle parameters = new Bundle();
        parameters.putParcelableArrayList(FilesEncryptionService.SRC_FILE_URIS, new ArrayList<>(srcFileUris));
        parameters.putLong(FilesEncryptionService.DST_FOLDER_ID, dstFolder.getId());
        parameters.putString(FilesEncryptionService.DST_KEY_ALIAS, dstKeyAlias);
        start(FilesEncryptionService.COMMAND_START, parameters);
    }
}
