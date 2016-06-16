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

package fr.petrus.lib.core.db;

/**
 * The constants used for accessing the database.
 *
 * <p>Here are defined the table and column names.
 *
 * @author Pierre Sagne
 * @since 18.07.2015
 */
public interface DatabaseConstants {
    String DATABASE_INFO_TABLE = "database_info_table";
    String DATABASE_INFO_ID = "_id";
    String DATABASE_INFO_VERSION = "version";

    String ACCOUNTS_TABLE = "accounts";
    String ACCOUNT_COLUMN_ID = "_id";
    String ACCOUNT_COLUMN_STORAGE_TYPE = "storage_type";
    String ACCOUNT_COLUMN_NAME = "name";
    String ACCOUNT_COLUMN_ACCESS_TOKEN = "access_token";
    String ACCOUNT_COLUMN_EXPIRATION_TIME = "expiration_time";
    String ACCOUNT_COLUMN_REFRESH_TOKEN = "refresh_token";
    String ACCOUNT_COLUMN_LAST_TOO_MANY_REQUESTS_ERROR_TIME = "last_too_many_requests_error_time";
    String ACCOUNT_COLUMN_NEXT_RETRY_DELAY = "next_retry_delay";
    String ACCOUNT_COLUMN_ROOT_FOLDER_ID = "root_folder_id";
    String ACCOUNT_COLUMN_LAST_REMOTE_CHANGE_ID = "last_remote_change_id";
    String ACCOUNT_COLUMN_QUOTA_AMOUNT = "quota_amount";
    String ACCOUNT_COLUMN_QUOTA_USED = "quota_used";
    String ACCOUNT_COLUMN_ESTIMATED_QUOTA_USED = "estimated_quota_used";
    String ACCOUNT_COLUMN_CHANGES_SYNC_STATE = "changes_sync_state";

    String ACCOUNT_COLUMN_OPENSTACK_ACCESS_TOKEN = "openstack_access_token";
    String ACCOUNT_COLUMN_OPENSTACK_ACCESS_TOKEN_EXPIRATION_TIME = "openstack_access_token_expiration_time";
    String ACCOUNT_COLUMN_OPENSTACK_ENDPOINT = "openstack_endpoint";
    String ACCOUNT_COLUMN_OPENSTACK_ACCOUNT = "openstack_account";

    String ENCRYPTED_DOCUMENTS_TABLE = "encrypted_documents";
    String ENCRYPTED_DOCUMENT_COLUMN_ID = "_id";
    String ENCRYPTED_DOCUMENT_COLUMN_DISPLAY_NAME = "display_name";
    String ENCRYPTED_DOCUMENT_COLUMN_MIME_TYPE = "mime_type";
    String ENCRYPTED_DOCUMENT_COLUMN_PARENT_ID = "parent_id";
    String ENCRYPTED_DOCUMENT_COLUMN_FILE_NAME = "file_name";
    String ENCRYPTED_DOCUMENT_COLUMN_SIZE = "size";
    String ENCRYPTED_DOCUMENT_COLUMN_KEY_ALIAS = "key_alias";
    String ENCRYPTED_DOCUMENT_COLUMN_LOCAL_MODIFICATION_TIME = "local_modification_time";
    String ENCRYPTED_DOCUMENT_COLUMN_REMOTE_MODIFICATION_TIME = "remote_modification_time";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_TYPE = "back_storage_type";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_STORAGE_ACCOUNT = "back_storage_account";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_ID = "back_entry_id";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_VERSION = "back_entry_version";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_FOLDER_ID = "back_entry_folder_id";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_FOLDER_LAST_SUBFOLDER_ID = "back_entry_folder_last_subfolder_id";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_UPLOAD_STATE = "back_entry_upload_state";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DOWNLOAD_STATE = "back_entry_download_state";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_DELETION_STATE = "back_entry_deletion_state";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_NUM_SYNC_FAILURES = "back_entry_num_sync_failures";
    String ENCRYPTED_DOCUMENT_COLUMN_BACK_ENTRY_LAST_SYNC_FAILURE_TIME = "back_entry_last_sync_failure_time";
}