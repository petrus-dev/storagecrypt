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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.tools.storagecrypt.android.services.DocumentsDecryptionService;

/**
 * The {@code Task} which handles documents decryption.
 *
 * @see DocumentsDecryptionService
 *
 * @author Pierre Sagne
 * @since 13.09.2015
 */
public class DocumentsDecryptionTask extends ServiceTask<DocumentsDecryptionService> {
    /**
     * Creates a new {@code DocumentsDecryptionTask} instance.
     *
     * @param appContext the application context
     * @param context the Android context
     */
    public DocumentsDecryptionTask(AppContext appContext, Context context) {
        super(appContext, context, DocumentsDecryptionService.class);
    }

    /**
     * Starts the decryption task in the background for the given {@code srcEncryptedDocument}.
     *
     * <p>If the {@code srcEncryptedDocument} is a folder, all its contents will be decrypted too
     *
     * @param srcEncryptedDocument the document to decrypt
     * @param dstFolder            the path of the destination folder where to store the decrypted
     *                             document
     */
    public void decrypt(EncryptedDocument srcEncryptedDocument, String dstFolder) {
        List<EncryptedDocument> srcEncryptedDocuments = new ArrayList<>();
        srcEncryptedDocuments.add(srcEncryptedDocument);
        decrypt(srcEncryptedDocuments, dstFolder);
    }

    /**
     * Starts the decryption task in the background for the given {@code srcEncryptedDocuments}.
     *
     * <p>If some of the {@code srcEncryptedDocuments} are folders, all their contents will be
     * decrypted too
     *
     * @param srcEncryptedDocuments the documents to decrypt
     * @param dstFolder             the path of the destination folder where to store the decrypted
     *                              documents
     */
    public void decrypt(Collection<EncryptedDocument> srcEncryptedDocuments, String dstFolder) {
        Bundle parameters = new Bundle();
        ArrayList<Long> documentIdList = new ArrayList<>();
        for (EncryptedDocument encryptedDocument : srcEncryptedDocuments) {
            if (null!= encryptedDocument) {
                documentIdList.add(encryptedDocument.getId());
            }
        }
        long[] documentIds = new long[documentIdList.size()];
        for (int i = 0; i<documentIds.length; i++) {
            documentIds[i] = documentIdList.get(i);
        }
        parameters.putLongArray(DocumentsDecryptionService.SRC_DOCUMENT_IDS, documentIds);
        parameters.putString(DocumentsDecryptionService.DST_FOLDER, dstFolder);
        start(DocumentsDecryptionService.COMMAND_START, parameters);
    }
}
