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

package fr.petrus.lib.core.platform;

import java.util.HashMap;

import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteStorage;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.cloud.implementations.box.BoxStorage;
import fr.petrus.lib.core.cloud.implementations.dropbox.DropboxStorage;
import fr.petrus.lib.core.cloud.implementations.gdrive.GoogleDriveStorage;
import fr.petrus.lib.core.cloud.implementations.hubic.HubicStorage;
import fr.petrus.lib.core.cloud.implementations.onedrive.OneDriveStorage;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.lib.core.tasks.Task;

/**
 * This class holds instances of various utility classes used by many other classes.
 *
 * <p>Most of the time it is passed as a dependency when creating or initializing objects.
 *
 * @author Pierre Sagne
 * @since 28.03.2016
 */
public class AppContext {
    private PlatformFactory factory = null;
    private Crypto crypto = null;
    private FileSystem fileSystem = null;
    private KeyManager keyManager = null;
    private CloudAppKeys cloudAppKeys = null;
    private Network network = null;
    private TextI18n textI18n = null;
    private Database database = null;
    private Accounts accounts = null;
    private EncryptedDocuments encryptedDocuments = null;
    private HashMap<StorageType, RemoteStorage> cloudStorages = new HashMap<>();
    private HashMap<Class, Task> tasks = new HashMap<>();

    /**
     * Creates a new {@code AppContext}.
     *
     * <p>The given {@code platformFactory} is used to create the platform dependant objects.
     *
     * @param platformFactory the {@code PlatformFactory} which will be used to create the platform
     *                        dependant instances
     */
    public AppContext(PlatformFactory platformFactory) {
        factory = platformFactory;

        crypto = platformFactory.crypto();
        crypto.initProvider();

        fileSystem = platformFactory.fileSystem();

        keyManager = platformFactory.keyManager(crypto);

        cloudAppKeys = platformFactory.cloudAppKeys(fileSystem);

        network = platformFactory.network();

        textI18n = platformFactory.textI18n();

        database = platformFactory.database(fileSystem, textI18n);

        accounts = new Accounts();
        encryptedDocuments = new EncryptedDocuments();

        /* All other objects have to be created before, because these two last calls will get them
           from this object. */
        accounts.setDependencies(this);
        encryptedDocuments.setDependencies(this);
    }

    /**
     * Returns the {@code PlatformFactory} instance which was passed when creating this {@code AppContext}.
     *
     * @return the {@code PlatformFactory} instance which was passed when creating this {@code AppContext}
     */
    public PlatformFactory getFactory() {
        return factory;
    }

    /**
     * Returns the {@code Crypto} instance.
     *
     * @return the {@code Crypto} instance
     */
    public Crypto getCrypto() {
        return crypto;
    }

    /**
     * Returns the {@code FileSystem} instance.
     *
     * @return the {@code FileSystem} instance
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * Returns the {@code KeyManager} instance.
     *
     * @return the {@code KeyManager} instance
     */
    public KeyManager getKeyManager() {
        return keyManager;
    }

    /**
     * Returns the {@code CloudAppKeys} instance.
     *
     * @return the {@code CloudAppKeys} instance
     */
    public CloudAppKeys getCloudAppKeys() {
        return cloudAppKeys;
    }

    /**
     * Returns the {@code Network} instance.
     *
     * @return the {@code Network} instance
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Returns the {@code TextI18n} instance.
     *
     * @return the {@code TextI18N} instance
     */
    public TextI18n getTextI18n() {
        return textI18n;
    }

    /**
     * Returns the {@code Database} instance.
     *
     * @return the {@code Database} instance
     */
    public Database getDatabase() {
        return database;
    }

    /**
     * Returns the {@code EncryptedDocuments} instance.
     *
     * @return the {@code EncryptedDocuments} instance
     */
    public EncryptedDocuments getEncryptedDocuments() {
        return encryptedDocuments;
    }

    /**
     * Returns the {@code Accounts} instance.
     *
     * @return the {@code Accounts} instance
     */
    public Accounts getAccounts() {
        return accounts;
    }

    /**
     * Returns the {@code CloudStorage} instance for the given {@code storageType}.
     *
     * @param storageType the {@code StorageType}
     * @return the {@code CloudStorage} instance for the given {@code storageType}
     */
    public RemoteStorage getCloudStorage(StorageType storageType) {
        RemoteStorage cloudStorage = cloudStorages.get(storageType);
        if (null==cloudStorage) {
            switch (storageType) {
                case GoogleDrive:
                    cloudStorage = new GoogleDriveStorage(
                            crypto, cloudAppKeys, accounts, encryptedDocuments);
                    break;
                case Dropbox:
                    cloudStorage = new DropboxStorage(crypto, cloudAppKeys, accounts);
                    break;
                case Box:
                    cloudStorage = new BoxStorage(crypto, cloudAppKeys, accounts);
                    break;
                case HubiC:
                    cloudStorage = new HubicStorage(crypto, cloudAppKeys, accounts);
                    break;
                case OneDrive:
                    cloudStorage = new OneDriveStorage(crypto, cloudAppKeys, accounts);
                    break;
            }
            cloudStorages.put(storageType, cloudStorage);
        }
        return cloudStorage;
    }

    /**
     * Returns the {@code Task} instance for the given {@code taskClass}.
     *
     * @param taskClass  the task class
     * @return the {@code Task} instance for the given {@code taskClass}
     * @throws TaskCreationException if an error occurs when creating the task
     */
    @SuppressWarnings("unchecked")
    public <T extends Task> T getTask(Class<T> taskClass) throws TaskCreationException {
        Task task = tasks.get(taskClass);
        if (null == task) {
            task = factory.task(this, taskClass);
            tasks.put(taskClass, task);
        }
        return (T) task;
    }
}
