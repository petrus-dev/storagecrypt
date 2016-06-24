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

package fr.petrus.lib.core.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.Constants;

/**
 * This class handles all the operations related to the application main key store.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class KeyManager {

    private static Logger LOG = LoggerFactory.getLogger(KeyManager.class);

    private Crypto crypto;
    private File keyStoreFolder;
    private String keyStorePassword;
    private KeyStoreUber keyStoreUber;

    /**
     * Creates a new {@code KeyManager}.
     *
     * @param crypto         the {@code Crypto} instance used to perform all cryptographic operations
     * @param keyStoreFolder the file where this instance stores the main key store file
     */
    public KeyManager(Crypto crypto, File keyStoreFolder) {
        this.crypto = crypto;
        this.keyStoreFolder = keyStoreFolder;
        keyStorePassword = null;
        keyStoreUber = null;
    }

    /**
     * Returns the file where all the encryption keys are be stored.
     *
     * @return the main key store file
     */
    public File getMainKeyStoreFile() {
        return new File(keyStoreFolder, Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME);
    }

    /**
     * Checks if the provided {@code password} matches the one which unlocked the keystore.
     *
     * @param password the password to be checked
     * @return true if the password matches the reference password, false otherwise
     */
    public boolean checkKeyStorePassword(String password) {
        return null != keyStorePassword && keyStorePassword.equals(password);
    }

    /**
     * Encrypts the {@code clearText} string with the "database security" key stored in the main key store.
     *
     * @param clearText the clear text to encrypt
     * @return the encrypted data, encoded as an "Url Safe" Base64 String.
     * @throws CryptoException if any cryptographic error occurs
     */
    public String encryptWithDatabaseSecurityKey(String clearText) throws CryptoException {
        if (null==keyStoreUber) {
            return null;
        }
        SecretKeys dbSecurityKeys = keyStoreUber.getDatabaseSecurityKeys();
        if (null == dbSecurityKeys) {
            return null;
        }
        EncryptedDataChunk encryptedDataChunk = crypto.encrypt(
                dbSecurityKeys.getEncryptionKey(),
                crypto.decodeUtf8(clearText));
        encryptedDataChunk.sign(dbSecurityKeys.getSignatureKey());
        return crypto.encodeUrlSafeBase64(encryptedDataChunk.merge());
    }

    /**
     * Decrypts the "Url Safe" Base64 encoded {@code encryptedText} with the "database security"
     * key stored in the main key store.
     *
     * @param encryptedText "Url Safe" Base64 encoded encrypted data
     * @return the decrypted string (encoded in "UTF-8" charset)
     * @throws CryptoException if any cryptographic error occurs
     */
    public String decryptWithDatabaseSecurityKey(String encryptedText) throws CryptoException {
        if (null==keyStoreUber) {
            return null;
        }
        SecretKeys dbSecurityKeys = keyStoreUber.getDatabaseSecurityKeys();
        if (null==dbSecurityKeys) {
            return null;
        }
        EncryptedDataChunk encryptedDataChunk = new EncryptedDataChunk(crypto);
        if (!encryptedDataChunk.parseEncryptedData(crypto.decodeUrlSafeBase64(encryptedText))) {
            return null;
        }
        if (!encryptedDataChunk.verify(dbSecurityKeys.getSignatureKey())) {
            return null;
        }
        return crypto.encodeUtf8(crypto.decrypt(dbSecurityKeys.getEncryptionKey(), encryptedDataChunk));
    }

    /**
     * Saves the main key store, protecting it with the given {@code password}.
     *
     * @param password the new password
     * @throws CryptoException if any cryptographic error occurs
     */
    public void changeKeystorePassword(String password) throws CryptoException {
        if (null==keyStorePassword) {
            throw new CryptoException("The keystore is locked.");
        }
        File keyStoreFile = new File(keyStoreFolder, Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME);
        if (!keyStoreFile.exists()) {
            throw new CryptoException("Keystore file not found.");
        }
        try {
            KeyStoreUber keyStoreUber = KeyStoreUber.loadKeyStore(keyStoreFile, keyStorePassword);
            keyStoreUber.saveKeyStore(keyStoreFile, password);
            keyStorePassword = password;
        } catch (CryptoException e) {
            throw new CryptoException("Error while changing the keystore password.", e);
        } catch (IOException e) {
            throw new CryptoException("Error while changing the keystore password.", e);
        }
    }

    /**
     * Returns the default key alias from the main key store (the first one in the list).
     *
     * @return the default key alias from the main key store, or null if none
     */
    public String getDefaultKeyAlias() {
        List<String> keyAliases = getKeyAliases();
        if (keyAliases.isEmpty()) {
            return null;
        } else {
            return keyAliases.get(0);
        }
    }

    /**
     * Returns the key aliases of all the keys stored in the main key store.
     *
     * @return the list of key aliases
     */
    public List<String> getKeyAliases() {
        if (null==keyStoreUber) {
            return null;
        }
        return keyStoreUber.getKeyAliases();
    }

    /**
     * Returns the "database security" keys, which are used to encrypt the database password.
     *
     * @return the "database security" keys, used to encrypt the database password
     * @throws CryptoException if any cryptographic error occurs
     */
    public SecretKeys getDatabaseSecurityKeys() throws CryptoException {
        if (null==keyStoreUber) {
            return null;
        }
        return keyStoreUber.getDatabaseSecurityKeys();
    }

    /**
     * Returns the {@code SecretKeys} associated to a given {@code alias}.
     *
     * @param alias the alias of the {@code SecretKeys} to return
     * @return the {@code SecretKeys} matching the given alias
     * @throws CryptoException if any cryptographic error occurs
     */
    public SecretKeys getKeys(String alias) throws CryptoException {
        if (null==keyStoreUber) {
            return null;
        }
        return keyStoreUber.getKeys(alias);
    }

    /**
     * Renames the alias of a {@code SecretKeys} stored in the main key store.
     *
     * @param oldAlias the old alias
     * @param newAlias the new alias
     * @return true if the key was successfully renamed
     * @throws CryptoException if any cryptographic error occurs
     */
    public boolean renameKeys(String oldAlias, String newAlias) throws CryptoException {
        if (null!=keyStorePassword && null!=keyStoreUber && null!=newAlias && !newAlias.isEmpty() && !newAlias.contains(Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR)) {
            if (keyStoreUber.renameKeys(oldAlias, newAlias)) {
                try {
                    keyStoreUber.saveKeyStore(getMainKeyStoreFile(), keyStorePassword);
                } catch (IOException e) {
                    throw new CryptoException("Error while saving keystore.", e);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Locks the main key store.
     *
     * <p>The key store has to be unlocked again to do anything on it.
     */
    public void lockKeyStore() {
        keyStorePassword = null;
        keyStoreUber = null;
    }

    /**
     * Returns wether the main key store is unlocked.
     *
     * @return true if the main key store is unlocked, false otherwise
     */
    public boolean isKeyStoreUnlocked() {
        return null!=keyStoreUber;
    }

    /**
     * Returns whether the main key store file exists.
     *
     * @return true if the main key store file exists, false otherwise
     */
    public boolean isKeyStoreExisting() {
        File keyStoreFile = getMainKeyStoreFile();
        return keyStoreFile.exists();
    }

    /**
     * Creates a new main key store and saves it as a file with the given {@code keyStorePassword}.
     *
     * @param keyStorePassword the main key store file password
     * @return true if the main key store file was created successfully, false otherwise
     */
    public boolean createKeyStore(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        try {
            /* Generate "database security" keys */
            SecretKey dbSecurityEncryptionKey = crypto.generateEncryptionKey(256);
            SecretKey dbSecuritySignatureKey = crypto.generateSignatureKey(256);

            /* Create the KeyStore itself */
            keyStoreUber = KeyStoreUber.createKeyStore();
            keyStoreUber.addDatabaseSecurityKeys(new SecretKeys(dbSecurityEncryptionKey, dbSecuritySignatureKey));

            /* Save the KeyStore */
            keyStoreUber.saveKeyStore(getMainKeyStoreFile(), keyStorePassword);
            return true;
        } catch (CryptoException e) {
            LOG.error("Error while generating keys into KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
        } catch (IOException e) {
            LOG.error("Error while generating keys into KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
        }
        return false;
    }

    /**
     * Tries to unlock the main key store with the given {@code keyStorePassword}.
     *
     * @param keyStorePassword the main key store password
     * @return true if the provided password unlocked successfully the main key store, false otherwise.
     */
    public boolean unlockKeyStore(String keyStorePassword) {
        File keyStoreFile = getMainKeyStoreFile();
        if (keyStoreFile.exists()) {
            try {
                keyStoreUber = KeyStoreUber.loadKeyStore(keyStoreFile, keyStorePassword);
                this.keyStorePassword = keyStorePassword;
                return true;
            } catch (CryptoException e) {
                LOG.error("Error while loading keys from KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            } catch (IOException e) {
                LOG.error("Error while loading keys from KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            }
        }
        return false;
    }

    /**
     * Generates keys with the specified {@code alias}.
     *
     * @param alias the alias to save the generated keys under
     * @return true if the keys were successfully generated and inserted in the key store
     *         with the given alias, false otherwise
     */
    public boolean generateKeys(String alias) {
        if (null!=keyStorePassword && null!=keyStoreUber && null!=alias && !alias.isEmpty() && !alias.contains(Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR)) {
            try {
                SecretKey newEncryptionKey = crypto.generateEncryptionKey(256);
                SecretKey newSignatureKey = crypto.generateSignatureKey(256);
                keyStoreUber.addKeys(alias, new SecretKeys(newEncryptionKey, newSignatureKey));
                keyStoreUber.saveKeyStore(getMainKeyStoreFile(), keyStorePassword);
                return true;
            } catch (CryptoException e) {
                LOG.error("Error while generating keys into KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            } catch (IOException e) {
                LOG.error("Error while generating keys into KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            }
        }
        return false;
    }

    /**
     * Deletes keys referenced with the specified {@code alias} from the main key store.
     *
     * @param alias the alias of the keys to delete
     * @return true if the keys were successfully deleted, false otherwise.
     */
    public boolean deleteKeys(String alias) {
        if (null!=keyStorePassword && null!=keyStoreUber) {
            try {
                keyStoreUber.deleteKeys(alias);
                keyStoreUber.saveKeyStore(getMainKeyStoreFile(), keyStorePassword);
                return true;
            } catch (CryptoException e) {
                LOG.error("Error while loading keys from KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            } catch (IOException e) {
                LOG.error("Error while loading keys from KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            }
        }
        return false;
    }

    /**
     * Imports keys from the given {@code srcKeyStore}, into the main key store.
     *
     * <p>The {@code renamedKeys} map can be used to rename keys when importing them.
     * The "keys" of the map are the aliases of the keys in the {@code srcKeyStore}.
     * The "values" of the map are the aliases in the main key store.
     * Only the keys which alias is in the "keys" of the {@code renamedKeys} map will be imported.
     *
     * @param srcKeyStore the key store to import keys from
     * @param renamedKeys a map which links the source key names to the destination names
     * @return true if the import operation was successful, false otherwise
     * @throws CryptoException if any cryptographic error occurs
     */
    public boolean importKeys(KeyStoreUber srcKeyStore, Map<String, String> renamedKeys) throws CryptoException {
        if (null!=keyStorePassword && null!=keyStoreUber) {
            try {
                List<String> existingKeyAliases = keyStoreUber.getKeyAliases();
                for (Map.Entry<String, String> renamedKey : renamedKeys.entrySet()) {
                    String renamedKeyAlias = renamedKey.getValue();
                    if (null==renamedKeyAlias || renamedKeyAlias.isEmpty()) {
                        renamedKeyAlias = renamedKey.getKey();
                    }
                    if (existingKeyAliases.contains(renamedKeyAlias)) {
                        throw new CryptoException("A key named \""+renamedKeyAlias+"\" already exists.");
                    }
                }
                for (Map.Entry<String, String> renamedKey : renamedKeys.entrySet()) {
                    String renamedKeyAlias = renamedKey.getValue();
                    if (null==renamedKeyAlias || renamedKeyAlias.isEmpty()) {
                        renamedKeyAlias = renamedKey.getKey();
                    }
                    SecretKeys secretKeys = srcKeyStore.getKeys(renamedKey.getKey());
                    keyStoreUber.addKeys(renamedKeyAlias, secretKeys);
                }
                keyStoreUber.saveKeyStore(getMainKeyStoreFile(), keyStorePassword);
                return true;
            } catch (CryptoException e) {
                LOG.error("Error while loading keys from KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            } catch (IOException e) {
                LOG.error("Error while loading keys from KeyStore file {}", Constants.CRYPTO.KEY_STORE_UBER_FILE_NAME, e);
            }
        }
        return false;
    }

    /**
     * Exports keys from the main key store, and returns a key store containing the exported keys.
     *
     * <p>The {@code renamedKeys} map can be used to rename keys when exporting them.
     * The "keys" of the map are the aliases of the keys in the main key store.
     * The "values" of the map are the aliases in the returned key store.
     * Only the keys which alias is in the "keys" of the {@code renamedKeys} map will be exported.
     *
     * @param renamedKeys a map which links the source key names to the destination names
     * @return a key store containing the exported keys
     * @throws CryptoException if any cryptographic error occurs
     */
    public KeyStoreUber exportKeys(Map<String, String> renamedKeys) throws CryptoException {
        if (null!=keyStorePassword && null!=keyStoreUber) {
            try {
                KeyStoreUber exportedKeyStore = KeyStoreUber.createKeyStore();
                for (Map.Entry<String, String> renamedKey : renamedKeys.entrySet()) {
                    String renamedKeyAlias = renamedKey.getValue();
                    if (null==renamedKeyAlias || renamedKeyAlias.isEmpty()) {
                        renamedKeyAlias = renamedKey.getKey();
                    }
                    SecretKeys secretKeys = keyStoreUber.getKeys(renamedKey.getKey());
                    exportedKeyStore.addKeys(renamedKeyAlias, secretKeys);
                }
                return exportedKeyStore;
            } catch (CryptoException e) {
                LOG.error("Error while exporting keys to KeyStore", e);
            }
        }
        return null;
    }
}
