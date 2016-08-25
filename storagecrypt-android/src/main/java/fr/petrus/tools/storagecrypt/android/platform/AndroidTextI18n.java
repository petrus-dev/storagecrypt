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

package fr.petrus.tools.storagecrypt.android.platform;

import android.content.Context;

import java.text.DateFormat;
import java.util.Date;

import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.cloud.exceptions.NetworkException;
import fr.petrus.lib.core.NotFoundException;
import fr.petrus.lib.core.i18n.AbstractTextI18n;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.R;

/**
 * The {@link TextI18n} implementation for the Android platform.
 *
 * @author Pierre Sagne
 * @since 03.07.2015
 */
public class AndroidTextI18n extends AbstractTextI18n {

    private Context context;

    /**
     * Creates a new {@code AndroidTextI18n} instance.
     *
     * @param context the Android context
     */
    AndroidTextI18n(Context context) {
        this.context = context;
    }

    @Override
    public String getTimeText(long time) {
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        Date date = new Date(time);
        return dateFormat.format(date) + " - " + timeFormat.format(date);
    }

    @Override
    public String getStorageTypeText(StorageType storageType) {
        switch(storageType) {
            case Unsynchronized:
                return context.getString(R.string.storage_type_description_unsynchronized);
            case GoogleDrive:
                return context.getString(R.string.storage_type_description_gdrive);
            case Dropbox:
                return context.getString(R.string.storage_type_description_dropbox);
            case Box:
                return context.getString(R.string.storage_type_description_box);
            case HubiC:
                return context.getString(R.string.storage_type_description_hubic);
            case OneDrive:
                return context.getString(R.string.storage_type_description_onedrive);
        }
        return null;
    }

    @Override
    public String getExceptionDescription(Exception exception) {
        if (null!=exception) {
            if (exception instanceof NetworkException) {
                return context.getString(R.string.error_message_NetworkException);
            } else if (exception instanceof NotFoundException) {
                return context.getString(R.string.error_message_NotFoundException);
            } else if (exception instanceof StorageCryptException) {
                StorageCryptException storageCryptException = (StorageCryptException) exception;
                if (null != storageCryptException.getReason()) {
                    switch (storageCryptException.getReason()) {
                        case GetRemoteAppFolderError:
                            return context.getString(R.string.error_message_StorageCryptException_GetRemoteAppFolderError);
                        case GetRemoteDocumentError:
                            return context.getString(R.string.error_message_StorageCryptException_GetRemoteDocumentError);
                        case GetRemoteFolderError:
                            return context.getString(R.string.error_message_StorageCryptException_GetRemoteFolderError);
                        case GetRemoteFileError:
                            return context.getString(R.string.error_message_StorageCryptException_GetRemoteFileError);
                        case CreationError:
                            return context.getString(R.string.error_message_StorageCryptException_CreationError);
                        case DocumentExists:
                            return context.getString(R.string.error_message_StorageCryptException_DocumentExists);
                        case ParentNotFound:
                            return context.getString(R.string.error_message_StorageCryptException_ParentNotFound);
                        case KeyStoreIsLocked:
                            return context.getString(R.string.error_message_StorageCryptException_KeyStoreIsLocked);
                        case EncryptionError:
                            return context.getString(R.string.error_message_StorageCryptException_EncryptionError);
                        case KeyNotFound:
                            return context.getString(R.string.error_message_StorageCryptException_KeyNotFound);
                        case DatabaseUnlockError:
                            return context.getString(R.string.error_message_StorageCryptException_DatabaseUnlockError);
                        case EncryptedNameSignatureVerificationError:
                            return context.getString(R.string.error_message_StorageCryptException_EncryptedNameSignatureVerificationError);
                        case EncryptedNameDecryptionError:
                            return context.getString(R.string.error_message_StorageCryptException_EncryptedNameDecryptionError);
                        case DecryptedNameParsingError:
                            return context.getString(R.string.error_message_StorageCryptException_DecryptedNameParsingError);
                        case DecryptedNameParsingErrorBadHeader:
                            return context.getString(R.string.error_message_StorageCryptException_DecryptedNameParsingErrorBadHeader);
                        case RemoteCreationError:
                            return context.getString(R.string.error_message_StorageCryptException_RemoteCreationError);
                        case RemoteCreationErrorFailedToGetParent:
                            return context.getString(R.string.error_message_StorageCryptException_RemoteCreationErrorFailedToGetParent);
                        case UploadError:
                            return context.getString(R.string.error_message_StorageCryptException_UploadError);
                        case FoldersNotAllowed:
                            return context.getString(R.string.error_message_StorageCryptException_FoldersNotAllowed);
                        case FailedToGetMetadata:
                            return context.getString(R.string.error_message_StorageCryptException_FailedToGetMetadata);
                        case DownloadError:
                            return context.getString(R.string.error_message_StorageCryptException_DownloadError);
                        case DeletionError:
                            return context.getString(R.string.error_message_StorageCryptException_DeletionError);
                        case TokenRevocationError:
                            return context.getString(R.string.error_message_StorageCryptException_TokenRevocationError);
                        case FileNotFound:
                            return context.getString(R.string.error_message_StorageCryptException_FileNotFound);
                        case BadPassword:
                            return context.getString(R.string.error_message_StorageCryptException_BadPassword);
                        case UnrecoverableKey:
                            return context.getString(R.string.error_message_StorageCryptException_UnrecoverableKey);
                        case KeyStoreCreationError:
                            return context.getString(R.string.error_message_StorageCryptException_KeyStoreCreationError);
                        case KeyStoreAddKeyError:
                            return context.getString(R.string.error_message_StorageCryptException_KeyStoreAddKeyError);
                        case KeyStoreSaveError:
                            return context.getString(R.string.error_message_StorageCryptException_KeyStoreSaveError);
                        case SourceFileOpenError:
                            return context.getString(R.string.error_message_StorageCryptException_SourceFileOpenError);
                        case DestinationFileOpenError:
                            return context.getString(R.string.error_message_StorageCryptException_DestinationFileOpenError);
                        case DecryptionError:
                            return context.getString(R.string.error_message_StorageCryptException_DecryptionError);
                    }
                }
            }
        }
        return context.getString(R.string.error_message_StorageCryptException_UnknownError);
    }
}
