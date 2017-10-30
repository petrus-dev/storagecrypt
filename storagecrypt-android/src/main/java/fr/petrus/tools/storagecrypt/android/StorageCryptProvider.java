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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.OrderBy;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.EncryptedDataStream;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.lib.core.result.OnCompletedAnonListener;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.DocumentListChangeEvent;
import fr.petrus.lib.core.result.OnCompletedListener;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;

/**
 * The {@code DocumentsProvider} which provides access to this app {@link EncryptedDocument}s to other
 * apps.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class StorageCryptProvider extends DocumentsProvider {
    private static final String TAG = "StorageCryptProvider";

    private static final String[] DEFAULT_ROOT_PROJECTION =
            new String[]{
                    DocumentsContract.Root.COLUMN_ROOT_ID,
                    DocumentsContract.Root.COLUMN_MIME_TYPES,
                    DocumentsContract.Root.COLUMN_FLAGS,
                    DocumentsContract.Root.COLUMN_ICON,
                    DocumentsContract.Root.COLUMN_TITLE,
                    DocumentsContract.Root.COLUMN_SUMMARY,
                    DocumentsContract.Root.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
            };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION =
            new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_FLAGS,
                    DocumentsContract.Document.COLUMN_SIZE
            };

    private AppContext appContext = null;
    private Crypto crypto = null;
    private KeyManager keyManager = null;
    private FileSystem fileSystem = null;
    private EncryptedDocuments encryptedDocuments = null;

    @Override
    public boolean onCreate() {
        init();
        return true;
    }

    /**
     * Sets the dependencies for this {@code StorageCryptProvider} from the {@code Application}.
     */
    private boolean init() {
        Application application = Application.getInstance();
        if (null!=application) {
            appContext = application.getAppContext();
            if (null!= appContext) {
                crypto = appContext.getCrypto();
                keyManager = appContext.getKeyManager();
                fileSystem = appContext.getFileSystem();
                encryptedDocuments = appContext.getEncryptedDocuments();
                return true;
            }
        }
        return false;
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }
    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        // Create a cursor with either the requested fields, or the default
        // projection if "projection" is null.
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        // If user is not logged in, return an empty root cursor.  This removes our
        // provider from the list entirely.
        if (!isUserLoggedIn()) {
            return result;
        }

        Context context = getContext();
        if (null!=context) {
            MatrixCursor.RowBuilder row = result.newRow();
            row.add(DocumentsContract.Root.COLUMN_ROOT_ID, Constants.STORAGE.ROOT_ID);
            row.add(DocumentsContract.Root.COLUMN_TITLE,
                    context.getString(R.string.provider_root_title));
            row.add(DocumentsContract.Root.COLUMN_SUMMARY,
                    context.getString(R.string.provider_root_summary));
            row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher_48dp);
            int flags = DocumentsContract.Root.FLAG_SUPPORTS_CREATE;
            if (Build.VERSION.SDK_INT >= 21) {
                flags |= DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD;
            }
            row.add(DocumentsContract.Root.COLUMN_FLAGS, flags);
            row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, Constants.STORAGE.ROOT_PARENT_ID);
        }

        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        final long parentDocId = Long.parseLong(parentDocumentId);
        final long docId = Long.parseLong(documentId);

        try {
            EncryptedDocument document = encryptedDocuments.encryptedDocumentWithId(docId);
            if (null!=document) {
                return parentDocId == document.getParentId();
            }
        } catch (DatabaseConnectionClosedException e) {
            throw new IllegalStateException(e);
        }
        return false;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        Log.d(TAG, "queryDocument(documentId="+documentId+")");

        // Create a cursor with the requested projection, or the default projection.
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        if (!isUserLoggedIn()) {
            throw new FileNotFoundException("Keystore is locked");
        }
        Context context = getContext();
        if (null!=context) {
            try {
                if (documentId.equals(String.valueOf(Constants.STORAGE.ROOT_PARENT_ID))) {
                    MatrixCursor.RowBuilder row = result.newRow();
                    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            Constants.STORAGE.ROOT_PARENT_ID);
                    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            context.getString(R.string.provider_root_title));
                    row.add(DocumentsContract.Document.COLUMN_MIME_TYPE,
                            Constants.CONTENT_PROVIDER.DIRECTORY_MIME_TYPE);
                    Log.d(TAG, "found root : (documentId=" + documentId + ")");
                } else {
                    includeDocument(result, documentId);
                }
            } catch (DatabaseConnectionClosedException e) {
                Log.e(TAG, "Database is closed", e);
            }
        }
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection,
                                      String sortOrder) throws FileNotFoundException {
        Log.d(TAG, "queryChildDocuments(parentDocumentId="+parentDocumentId+")");

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        if (!isUserLoggedIn()) {
            throw new FileNotFoundException("Keystore is locked");
        }

        try {
            includeChildDocuments(result, parentDocumentId);
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        }
        return result;
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) {
        Log.d(TAG, "createDocument(parentDocumentId=" + parentDocumentId + ", mimeType=" + mimeType + ", displayName=" + displayName + ")");
        if (!isUserLoggedIn()) {
            return null;
        }

        try {
            long parentId = Long.parseLong(parentDocumentId);
            EncryptedDocument parentEncryptedDocument = encryptedDocuments.encryptedDocumentWithId(parentId);
            if (null!= parentEncryptedDocument) {
                EncryptedDocument newFile = parentEncryptedDocument.createChild(displayName,
                        getDocumentMimeType(mimeType), parentEncryptedDocument.getKeyAlias());
                if (!newFile.isUnsynchronized()) {
                    try {
                        appContext.getTask(DocumentsSyncTask.class).syncDocument(newFile);
                    } catch (TaskCreationException e) {
                        Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                    }
                }
                return String.valueOf(newFile.getId());
            }
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
        } catch (StorageCryptException e) {
            Log.e(TAG, "Error while creating document", e);
        }
        return null;
    }

    @Override
    public ParcelFileDescriptor openDocument(final String documentId,
                                             final String mode,
                                             CancellationSignal signal)
            throws FileNotFoundException {

        Log.d(TAG, "openDocument(documentId="+documentId+", mode="+mode+")");
        if (!isUserLoggedIn()) {
            throw new FileNotFoundException("Keystore is locked");
        }

        final long docId = Long.parseLong(documentId);
        try {
            EncryptedDocument encryptedDocument = encryptedDocuments.encryptedDocumentWithId(docId);
            if ("r".equals(mode)) {
                return openReadOnly(encryptedDocument);
            } else if ("w".equals(mode) || "wt".equals(mode)) {
                if (null== encryptedDocument) {
                    Log.w(TAG, "openDocument(documentId="+documentId+", mode="+mode+") : file does not exist");
                    return null;
                }
                return openWriteOnly(encryptedDocument, new OnCompletedListener<EncryptedDocument>() {
                    @Override
                    public void onSuccess(EncryptedDocument result) {
                        try {
                            DocumentsSyncTask documentsSyncTask =
                                    appContext.getTask(DocumentsSyncTask.class);
                            documentsSyncTask.restartCurrentSync(result);
                            documentsSyncTask.syncDocument(result);
                        } catch (TaskCreationException e) {
                            Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
                        }
                        DocumentListChangeEvent.postSticky();
                    }
                });
            } else {
                throw new IllegalArgumentException("Unsupported mode: " + mode);
            }
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG, "Database is closed", e);
            throw new IllegalStateException(e);
        } catch (CryptoException e) {
            Log.e(TAG, "Error while opening document", e);
            throw new IllegalStateException(e);
        } catch (IOException e) {
            Log.e(TAG, "Error while opening document", e);
            throw new IllegalStateException(e);
        }
    }

    private boolean isUserLoggedIn() {
        if (null==keyManager) {
            if (!init()) {
                return false;
            }
        }
        return null!=keyManager && keyManager.isKeyStoreUnlocked();
    }

    private void includeDocument(MatrixCursor result, EncryptedDocument encryptedDocument) {
        if (null!= encryptedDocument) {
            final MatrixCursor.RowBuilder row = result.newRow();

            int flags = DocumentsContract.Document.FLAG_SUPPORTS_WRITE
                    | DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
            if (Build.VERSION.SDK_INT >= 21) {
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
            }
            if (encryptedDocument.isRoot() || encryptedDocument.isFolder()) {
                flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
            }

            String displayName;
            if (encryptedDocument.isRoot()) {
                displayName = encryptedDocument.storageText();
            } else {
                displayName =  encryptedDocument.getDisplayName();
            }

            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, encryptedDocument.getId());
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName);
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, getProviderMimeType(encryptedDocument.getMimeType()));
            row.add(DocumentsContract.Document.COLUMN_SIZE, encryptedDocument.getSize());
            row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        }
    }

    private void includeDocument(MatrixCursor result, String documentId)
            throws DatabaseConnectionClosedException {
        try {
            EncryptedDocument encryptedDocument =
                    encryptedDocuments.encryptedDocumentWithId(Long.parseLong(documentId));
            if (null != encryptedDocument) {
                Log.d(TAG, "includeDocument(documentId=" + documentId + ") : found");
                includeDocument(result, encryptedDocument);
            } else {
                Log.d(TAG, "includeDocument(documentId=" + documentId + ") : not found");
            }
        } catch(NumberFormatException e) {
            Log.d(TAG, "Number format error", e);
        }
    }

    private void includeChildDocuments(MatrixCursor result, String parentDocumentId)
            throws DatabaseConnectionClosedException {
        if (parentDocumentId.equals(String.valueOf(Constants.STORAGE.ROOT_PARENT_ID))) {
            encryptedDocuments.updateRoots();
            List<EncryptedDocument> rootEncryptedDocuments = encryptedDocuments.roots();
            for (EncryptedDocument encryptedDocument : rootEncryptedDocuments) {
                Log.d(TAG, "found root : (documentId="+encryptedDocument.getId()+")");
                includeDocument(result, encryptedDocument);
            }
        } else {
            EncryptedDocument parent = encryptedDocuments.encryptedDocumentWithId(Long.parseLong(parentDocumentId));
            if (null != parent) {
                List<EncryptedDocument> documentsList = parent.children(true, OrderBy.NameAsc);
                Log.d(TAG, "includeChildDocuments(parentDocumentId=" + parentDocumentId + ")");
                for (EncryptedDocument encryptedDocument : documentsList) {
                    Log.d(TAG, "  documentId=" + encryptedDocument.getId() + ", displayName=" + encryptedDocument.getDisplayName());
                    includeDocument(result, encryptedDocument);
                }
            }
        }
    }

    private String getProviderMimeType(String mimeType) {
        if (Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE.equals(mimeType)) {
            return Constants.CONTENT_PROVIDER.DIRECTORY_MIME_TYPE;
        }
        return mimeType;
    }

    private String getDocumentMimeType(String mimeType) {
        if (Constants.CONTENT_PROVIDER.DIRECTORY_MIME_TYPE.equals(mimeType)) {
            return Constants.STORAGE.DEFAULT_FOLDER_MIME_TYPE;
        }
        return mimeType;
    }

    private ParcelFileDescriptor openReadOnly(EncryptedDocument encryptedDocument)
            throws DatabaseConnectionClosedException, IOException, CryptoException {
        File file = encryptedDocument.file();
        Context context = getContext();
        if (null!=context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String prefsCacheOpenedFiles = prefs.getString(
                    context.getString(R.string.pref_key_cache_opened_files), null);
            if (getContext().getString(R.string.pref_entryvalue_cache_opened_files_all).equals(prefsCacheOpenedFiles)) {
                return decryptAndStartRead(encryptedDocument, new FileInputStream(file));
            } else if (getContext().getString(R.string.pref_entryvalue_cache_opened_files_audio_and_video).equals(prefsCacheOpenedFiles)) {
                if (encryptedDocument.getMimeType().startsWith("video/")
                        || encryptedDocument.getMimeType().startsWith("audio/")) {
                    return decryptAndStartRead(encryptedDocument, new FileInputStream(file));
                } else {
                    return startRead(encryptedDocument, new FileInputStream(file));
                }
            } else if (getContext().getString(R.string.pref_entryvalue_cache_opened_files_none).equals(prefsCacheOpenedFiles)) {
                return startRead(encryptedDocument, new FileInputStream(file));
            }
        }
        return startRead(encryptedDocument, new FileInputStream(file));
    }

    private ParcelFileDescriptor openWriteOnly(final EncryptedDocument encryptedDocument,
                                               final OnCompletedListener<EncryptedDocument> onCompletedListener)
            throws DatabaseConnectionClosedException, IOException, CryptoException {
        File file = encryptedDocument.file();
        encryptedDocument.updateLocalModificationTime(System.currentTimeMillis());
        return startWrite(encryptedDocument,
                new FileOutputStream(file), new OnCompletedAnonListener() {
                    @Override
                    public void onSuccess() {
                        try {
                            encryptedDocument.updateFileSize();
                            if (!encryptedDocument.isUnsynchronized()) {
                                encryptedDocument.updateSyncState(SyncAction.Upload, State.Planned);
                                if (null != onCompletedListener) {
                                    onCompletedListener.onSuccess(encryptedDocument);
                                }
                            }
                        } catch (DatabaseConnectionClosedException e) {
                            Log.e(TAG, "Database is closed", e);
                        }
                    }

                    public void onFailed(StorageCryptException e) {
                        if (null != onCompletedListener) {
                            onCompletedListener.onFailed(e);
                        }
                    }
                });
    }

    private ParcelFileDescriptor startRead(final EncryptedDocument encryptedDocument,
                                           final InputStream inputStream)
            throws DatabaseConnectionClosedException, IOException {

        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
        ParcelFileDescriptor readEnd = pipe[0];
        final ParcelFileDescriptor writeEnd = pipe[1];

        final OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(writeEnd);

        try {
            final EncryptedDataStream encryptedDataStream =
                    new EncryptedDataStream(crypto, keyManager.getKeys(encryptedDocument.getKeyAlias()));
            new Thread() {
                @Override
                public void run() {
                    try {
                        encryptedDataStream.decrypt(inputStream, outputStream, null);
                    } catch (CryptoException e) {
                        Log.d(TAG, "Error while decrypting", e);
                    } finally {
                        if (null != inputStream) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error while closing input stream", e);
                            }
                        }
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error while closing output stream", e);
                        }
                    }
                }
            }.start();
        } catch (CryptoException e) {
            Log.e(TAG, "Failed to get key "+ encryptedDocument.getKeyAlias());
        }
        return readEnd;
    }

    private ParcelFileDescriptor decryptAndStartRead(final EncryptedDocument encryptedDocument,
                                                     final InputStream inputStream)
            throws DatabaseConnectionClosedException, IOException {

        fileSystem.removeCacheFiles();

        InputStream in = null;
        OutputStream out = null;

        try {
            Date now = new Date();

            File tempFile = new File(fileSystem.getCacheFilesDir(),
                    String.format(Locale.getDefault(), "%d_%s", now.getTime(),
                            encryptedDocument.getFileName()));

            File srcFile = encryptedDocument.file();
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(tempFile);

            try {
                EncryptedDataStream encryptedDataStream =
                        new EncryptedDataStream(crypto, keyManager.getKeys(encryptedDocument.getKeyAlias()));
                encryptedDataStream.decrypt(inputStream, out, null);
            } catch (CryptoException e) {
                Log.d(TAG, "Error while decrypting", e);
                throw new IOException("Error while decrypting", e);
            }

            return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error while closing input stream", e);
                }
            }
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error while closing output stream", e);
                }
            }
        }
    }

    /**
     * Kick off a thread to handle a write request for the given document.
     * Internally creates a pipe and returns the write end for returning to a
     * remote process.
     */
    private ParcelFileDescriptor startWrite(final EncryptedDocument encryptedDocument,
                                            final OutputStream outputStream,
                                            final OnCompletedAnonListener onCloseListener)
            throws IOException {
        Log.d(TAG, "startWrite()");
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
        final ParcelFileDescriptor readEnd = pipe[0];
        final ParcelFileDescriptor writeEnd = pipe[1];

        final InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readEnd);

        Log.d(TAG, "init encryption");
        try {
            final EncryptedDataStream encryptedDataStream =
                    new EncryptedDataStream(crypto, keyManager.getKeys(encryptedDocument.getKeyAlias()));
            new Thread() {
                @Override
                public void run() {
                    try {
                        encryptedDataStream.encrypt(inputStream, outputStream, null);
                        if (null != onCloseListener) {
                            onCloseListener.onSuccess();
                        }
                    } catch (CryptoException e) {
                        Log.d(TAG, "Error while encrypting", e);
                        if (null != onCloseListener) {
                            onCloseListener.onFailed(new StorageCryptException("Error while encrypting",
                                    StorageCryptException.Reason.EncryptionError, e));
                        }
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error while closing input stream", e);
                        }
                        if (null != outputStream) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error while closing output stream", e);
                            }
                        }
                    }
                }
            }.start();
        } catch (CryptoException e) {
            Log.e(TAG, "Failed to get key "+ encryptedDocument.getKeyAlias());
        }
        return writeEnd;
    }
}
