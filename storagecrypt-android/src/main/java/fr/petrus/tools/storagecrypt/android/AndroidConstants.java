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

package fr.petrus.tools.storagecrypt.android;

/**
 * This interface contains the constants used by the classes of the Android module.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class AndroidConstants {

    public interface MAIN_ACTIVITY {
        String PREF_DATABASE_ENCRYPTION_PASSWORD = "database_encryption_password";

        int INTENT_PICK_ENCRYPTION_SOURCE_FILE             = 1;
        int INTENT_CREATE_DECRYPTION_DESTINATION_FILE      = 2;
        int INTENT_SELECT_DECRYPTION_DESTINATION_FOLDER    = 3;
        int INTENT_PICK_KEYSTORE_IMPORT_FILE               = 4;
        int INTENT_CREATE_KEYSTORE_EXPORT_FILE             = 5;
        int INTENT_SELECT_ENCRYPTION_MULTIPLE_SOURCE_FILES = 6;

        int REQUEST_PERMISSION_READ_WRITE_EXTERNAL_STORAGE  = 1;

        int NON_EMPTY_PROVIDER_SUPPRESSION_DIALOG                      =  1;
        int NON_EMPTY_FOLDER_SUPPRESSION_DIALOG                        =  2;
        int KEY_SUPPRESSION_DIALOG                                     =  3;

        int NEW_KEY_ALIAS_TEXT_INPUT_DIALOG                            =  4;
        int RENAME_KEY_ALIAS_TEXT_INPUT_DIALOG                         =  5;
        int IMPORT_KEYSTORE_PASSWORD_TEXT_INPUT_DIALOG                 =  6;
        int EXPORT_KEYSTORE_PASSWORD_TEXT_INPUT_DIALOG                 =  7;

        int ENCRYPT_DOCUMENT_KEY_SELECTION_LIST_DIALOG                 =  8;
        int ENCRYPT_MULTIPLE_DOCUMENTS_KEY_SELECTION_LIST_DIALOG       =  9;
        int ENCRYPT_QUEUED_FILES_KEY_SELECTION_LIST_DIALOG             = 10;
        int SELECT_ROOT_DEFAULT_KEY_SELECTION_LIST_DIALOG              = 11;

        int ADD_ACCOUNT_PROGRESS_DIALOG                                = 12;
        int DOCUMENTS_ENCRYPTION_PROGRESS_DIALOG                       = 13;
        int DOCUMENTS_DECRYPTION_PROGRESS_DIALOG                       = 14;
        int DOCUMENTS_IMPORT_PROGRESS_DIALOG                           = 15;
        int DOCUMENTS_UPDATES_PUSH_PROGRESS_DIALOG                     = 16;
        int CHANGES_SYNC_PROGRESS_DIALOG                               = 17;
        int DOCUMENTS_SYNC_PROGRESS_DIALOG                             = 18;
        int FILE_DECRYPTION_PROGRESS_DIALOG                            = 19;
        int FILES_ENCRYPTION_PROGRESS_DIALOG                           = 20;

        int WRONG_PASSWORD_ALERT_DIALOG                                = 21;
        int DATABASE_UNLOCK_ERROR_ALERT_DIALOG                         = 22;

        int READ_WRITE_EXTERNAL_STORAGE_PERMISSION_EXPLANATION_DIALOG  = 23;
        int READ_WRITE_EXTERNAL_STORAGE_PERMISSION_REFUSED_DIALOG      = 24;
    }

    public interface CONTENT_PROVIDER {
        String AUTHORITY = "fr.petrus.tools.storagecrypt.documents";
        String BASE_DOCUMENT_URI = "content://"+AUTHORITY+"/document/";
    }

    public interface SERVICE {
        int FOREGROUND_SERVICE_NOTIFICATION_ID = 888;
        String NOTIFICATION_ACTION  = "fr.petrus.tools.storagecrypt.action.notification";
    }
}
