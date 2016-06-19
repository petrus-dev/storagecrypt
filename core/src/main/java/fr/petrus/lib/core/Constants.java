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

package fr.petrus.lib.core;

import java.nio.charset.StandardCharsets;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * This interface contains the constants used by the classes of the "core" module.
 *
 * @author Pierre Sagne
 * @since 29.12.14
 */
public class Constants {

    public interface CRYPTO {
        byte[] STREAM_PREFIX = "SCDS".getBytes(StandardCharsets.UTF_8);
        short STREAM_VERSION = 0;
        byte[] CHUNK_PREFIX = "SCDC".getBytes(StandardCharsets.UTF_8);
        short CHUNK_VERSION = 0;

        //int MAX_CHUNK_SIZE = 16 * 1024 * 1024; // 16MB
        int MAX_CHUNK_SIZE = 1024 * 1024; // 1MB

        String KEY_STORE_UBER_FILE_NAME = "StorageCrypt.ubr";
        String KEY_STORE_UBER_DEFAULT_EXPORT_FILE_NAME = "ExportedKeys.ubr";
        String KEY_STORE_ENCRYPTION_KEY_ALIAS = "EncryptionKey";
        String KEY_STORE_SIGNATURE_KEY_ALIAS = "SignatureKey";
        String KEY_STORE_ALIAS_SEPARATOR = ":";
        String KEY_STORE_DATABASE_SECURITY_KEY_ALIAS = "DatabaseSecurity";
        String KEY_STORE_DEFAULT_KEY_ALIAS = "default";
        String KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 _-@()[]";

        String AES_ENCRYPT_ALGO = "AES";
        String AES_FULL_ENCRYPT_ALGO = "AES/CBC/PKCS7Padding";
        String MAC_ALGO = "HmacSHA256";

        String ENCRYPTED_DOCUMENT_NAME_HEADER = "StorageCrypt";
        String ENCRYPTED_DOCUMENT_NAME_SEPARATOR = ":";
    }

    public interface STORAGE {
        long ROOT_PARENT_ID = -1;
        String ROOT_ID = "StorageCrypt";
        String DEFAULT_BINARY_MIME_TYPE = "application/octet-stream";
        String DEFAULT_FOLDER_MIME_TYPE = "application/directory";
        String FOLDER_METADATA_FILE_NAME = ".metadata";
        int CLOUD_SYNC_PROGRESS_UPDATE_DELTA = 1000;
        int CLOUD_SYNC_MAX_FAILURES = 5;
        int CLOUD_SYNC_FAILURE_RESET_DELAY_S = 60;
    }

    public interface RETROFIT {
        //HttpLoggingInterceptor.Level LOG_LEVEL = HttpLoggingInterceptor.Level.BODY;
        //HttpLoggingInterceptor.Level LOG_LEVEL = HttpLoggingInterceptor.Level.HEADERS;
        //HttpLoggingInterceptor.Level LOG_LEVEL = HttpLoggingInterceptor.Level.BASIC;
        HttpLoggingInterceptor.Level LOG_LEVEL = HttpLoggingInterceptor.Level.NONE;
    }

    public interface ORMLITE {
        //String LOG_LEVEL = "TRACE";
        //String LOG_LEVEL = "DEBUG";
        //String LOG_LEVEL = "INFO";
        //String LOG_LEVEL = "WARN";
        String LOG_LEVEL = "ERROR";
        //String LOG_LEVEL = "FATAL";
    }

    public interface GOOGLE_DRIVE {
        String API_BASE_URL = "https://www.googleapis.com/";
        String ACCOUNTS_API_BASE_URL = "https://accounts.google.com/";
        String DISCOVERY_URL = ACCOUNTS_API_BASE_URL+".well-known/openid-configuration";
        String OAUTH_URL = ACCOUNTS_API_BASE_URL+"o/oauth2/auth";
        String SCOPE = API_BASE_URL+"auth/drive.file";
        String RESPONSE_TYPE = "code";
        String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
        String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
        String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    }

    public interface DROPBOX {
        String API_BASE_URL = "https://api.dropboxapi.com/";
        String CONTENT_API_BASE_URL = "https://content.dropboxapi.com/";
        String OAUTH_URL = "https://www.dropbox.com/1/oauth2/authorize";
        String RESPONSE_TYPE = "code";
        String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
    }

    public interface BOX {
        String API_BASE_URL = "https://api.box.com/";
        String UPLOAD_API_BASE_URL = "https://upload.box.com/";
        String OAUTH_URL = API_BASE_URL+"oauth2/authorize";
        String RESPONSE_TYPE = "code";
        String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
        String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
        String ROOT_FOLDER_ID = "0";
        String DESCRIPTION_STRING = "StorageCrypt";
    }

    public interface HUBIC {
        String API_BASE_URL = "https://api.hubic.com/";
        String OAUTH_URL = API_BASE_URL+"oauth/auth";
        String SCOPE = "usage.r,account.r,credentials.r";
        String RESPONSE_TYPE = "code";
        String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
        String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
        String FOLDER_MIME_TYPE = STORAGE.DEFAULT_FOLDER_MIME_TYPE;
        String OPENSTACK_CONTAINER = "default";
        int CONNECT_TIMEOUT_S = 60;
        int READ_TIMEOUT_S = 60;
        int WRITE_TIMEOUT_S = 60;
    }

    public interface ONE_DRIVE {
        String OAUTH_BASE_URL = "https://login.live.com/";
        String LIVE_API_BASE_URL = "https://apis.live.net/";
        String API_BASE_URL = "https://api.onedrive.com/";
        String OAUTH_URL = OAUTH_BASE_URL+"oauth20_authorize.srf";
        String SCOPE = "wl.offline_access wl.signin wl.basic wl.emails wl.skydrive wl.skydrive_update onedrive.readwrite";
        String RESPONSE_TYPE = "code";
        String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
        String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
        String ROOT_FOLDER_ID = "root";
        String FOLDER_MIME_TYPE = STORAGE.DEFAULT_FOLDER_MIME_TYPE;
    }

    public interface FILE {
        String APP_DIR_NAME = "StorageCrypt";
        String TEMP_FILES_DIR_NAME = "tmp";
        String LOCAL_FILES_DIR_NAME = "Unsynchronized";
        String GDRIVE_FILES_DIR_NAME = "Drive";
        String DROPBOX_FILES_DIR_NAME = "Dropbox";
        String BOX_FILES_DIR_NAME = "Box";
        String HUBIC_FILES_DIR_NAME = "HubiC";
        String ONEDRIVE_FILES_DIR_NAME = "OneDrive";
        String EXTERNAL_APP_KEYS_FILE = "keys.json";
        int BUFFER_SIZE = 1024 * 64;
        //int BUFFER_SIZE = 1024;
        long QUOTA_USED_ESTIMATION_BEFORE_REFRESH = 1024 * 1024;
    }

    public interface CONTENT_PROVIDER {
        String DIRECTORY_MIME_TYPE = "vnd.android.document/directory";
    }

}
