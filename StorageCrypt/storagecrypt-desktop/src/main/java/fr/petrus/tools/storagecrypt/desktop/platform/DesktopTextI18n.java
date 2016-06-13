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

package fr.petrus.tools.storagecrypt.desktop.platform;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.i18n.AbstractTextI18n;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;

/**
 * The {@link TextI18n} implementation for the "Desktop" platform.
 *
 * @author Pierre Sagne
 * @since 03.07.2015
 */
public class DesktopTextI18n extends AbstractTextI18n {

    private TextBundle textBundle;

    /**
     * Creates a new {@code DesktopTextI18n} instance.
     *
     * @param textBundle the text bundle where to lookup the I18n text definitions
     */
    DesktopTextI18n(TextBundle textBundle) {
        this.textBundle = textBundle;
    }

    @Override
    public String getTimeText(long time) {
        DateTimeFormatter formatter = DateTimeFormat.mediumDateTime().withLocale(Locale.getDefault());
        //DateTimeFormatter formatter = DateTimeFormat.longDateTime().withLocale(Locale.getDefault());
        //DateTimeFormatter formatter = DateTimeFormat.fullDateTime().withLocale(Locale.getDefault());
        return formatter.print(time);
    }

    @Override
    public String getStorageTypeText(StorageType storageType) {
        switch(storageType) {
            case Unsynchronized:
                return textBundle.getString("storage_type_description_unsynchronized");
            case GoogleDrive:
                return textBundle.getString("storage_type_description_gdrive");
            case Dropbox:
                return textBundle.getString("storage_type_description_dropbox");
            case Box:
                return textBundle.getString("storage_type_description_box");
            case HubiC:
                return textBundle.getString("storage_type_description_hubic");
            case OneDrive:
                return textBundle.getString("storage_type_description_onedrive");
        }
        return null;
    }

    @Override
    public String getExceptionDescription(StorageCryptException exception) {
        if (null!=exception && null!=exception.getReason()) {
            switch (exception.getReason()) {
                case GetRemoteAppFolderError:
                    return textBundle.getString("error_message_StorageCryptException_GetRemoteAppFolderError");
                case GetRemoteDocumentError:
                    return textBundle.getString("error_message_StorageCryptException_GetRemoteDocumentError");
                case GetRemoteFolderError:
                    return textBundle.getString("error_message_StorageCryptException_GetRemoteFolderError");
                case GetRemoteFileError:
                    return textBundle.getString("error_message_StorageCryptException_GetRemoteFileError");
                case CreationError:
                    return textBundle.getString("error_message_StorageCryptException_CreationError");
                case DocumentExists:
                    return textBundle.getString("error_message_StorageCryptException_DocumentExists");
                case ParentNotFound:
                    return textBundle.getString("error_message_StorageCryptException_ParentNotFound");
                case KeyStoreIsLocked:
                    return textBundle.getString("error_message_StorageCryptException_KeyStoreIsLocked");
                case EncryptionError:
                    return textBundle.getString("error_message_StorageCryptException_EncryptionError");
                case KeyNotFound:
                    return textBundle.getString("error_message_StorageCryptException_KeyNotFound");
                case DatabaseUnlockError:
                    return textBundle.getString("error_message_StorageCryptException_DatabaseUnlockError");
                case EncryptedNameSignatureVerificationError:
                    return textBundle.getString("error_message_StorageCryptException_EncryptedNameSignatureVerificationError");
                case EncryptedNameDecryptionError:
                    return textBundle.getString("error_message_StorageCryptException_EncryptedNameDecryptionError");
                case DecryptedNameParsingError:
                    return textBundle.getString("error_message_StorageCryptException_DecryptedNameParsingError");
                case DecryptedNameParsingErrorBadHeader:
                    return textBundle.getString("error_message_StorageCryptException_DecryptedNameParsingErrorBadHeader");
                case RemoteCreationError:
                    return textBundle.getString("error_message_StorageCryptException_RemoteCreationError");
                case RemoteCreationErrorFailedToGetParent:
                    return textBundle.getString("error_message_StorageCryptException_RemoteCreationErrorFailedToGetParent");
                case UploadError:
                    return textBundle.getString("error_message_StorageCryptException_UploadError");
                case FoldersNotAllowed:
                    return textBundle.getString("error_message_StorageCryptException_FoldersNotAllowed");
                case FailedToGetMetadata:
                    return textBundle.getString("error_message_StorageCryptException_FailedToGetMetadata");
                case DownloadError:
                    return textBundle.getString("error_message_StorageCryptException_DownloadError");
                case DeletionError:
                    return textBundle.getString("error_message_StorageCryptException_DeletionError");
                case TokenRevocationError:
                    return textBundle.getString("error_message_StorageCryptException_TokenRevocationError");
                case FileNotFound:
                    return textBundle.getString("error_message_StorageCryptException_FileNotFound");
                case BadPassword:
                    return textBundle.getString("error_message_StorageCryptException_BadPassword");
                case UnrecoverableKey:
                    return textBundle.getString("error_message_StorageCryptException_UnrecoverableKey");
                case KeyStoreCreationError:
                    return textBundle.getString("error_message_StorageCryptException_KeyStoreCreationError");
                case KeyStoreAddKeyError:
                    return textBundle.getString("error_message_StorageCryptException_KeyStoreAddKeyError");
                case KeyStoreSaveError:
                    return textBundle.getString("error_message_StorageCryptException_KeyStoreSaveError");
                case SourceFileOpenError:
                    return textBundle.getString("error_message_StorageCryptException_SourceFileOpenError");
                case DestinationFileOpenError:
                    return textBundle.getString("error_message_StorageCryptException_DestinationFileOpenError");
                case DecryptionError:
                    return textBundle.getString("error_message_StorageCryptException_DecryptionError");
            }
        }
        return textBundle.getString("error_message_StorageCryptException_UnknownError");
    }
}
