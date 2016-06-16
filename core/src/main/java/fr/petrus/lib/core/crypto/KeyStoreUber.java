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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.Constants;

/**
 * This class is used to perform operations on a "UBER" key store
 * <p/>
 * <p>"UBER" key stores are password protected, and can store AES keys.
 *
 * @author Pierre Sagne
 * @since 19.12.2014.
 */
public class KeyStoreUber {

    private static Logger LOG = LoggerFactory.getLogger(KeyStoreUber.class);

    /* The aliases for the special keys used to encrypt the database password */
    private static final String DB_SECURITY_ENCRYPTION_KEY_ALIAS =
            Constants.CRYPTO.KEY_STORE_DATABASE_SECURITY_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR
                    + Constants.CRYPTO.KEY_STORE_ENCRYPTION_KEY_ALIAS;

    private static final String DB_SECURITY_SIGNATURE_KEY_ALIAS =
            Constants.CRYPTO.KEY_STORE_DATABASE_SECURITY_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR
                    + Constants.CRYPTO.KEY_STORE_SIGNATURE_KEY_ALIAS;

    private KeyStore keyStore;
    private TreeMap<Integer, String> orderedKeyAliases;
    private HashMap<String, Integer> keyAliasesIndices;

    /**
     * Creates an empty {@code KeyStoreUber} instance
     */
    private KeyStoreUber() {
        keyStore = null;
        orderedKeyAliases = new TreeMap<>();
        keyAliasesIndices = new HashMap<>();
    }

    /**
     * Creates a new key store.
     *
     * @return the newly created key store
     * @throws CryptoException if any cryptographic error occurs
     */
    public static KeyStoreUber createKeyStore() throws CryptoException {
        try {
            KeyStoreUber keyStoreUber = new KeyStoreUber();
            keyStoreUber.keyStore = KeyStore.getInstance("UBER");
            keyStoreUber.keyStore.load(null);
            return keyStoreUber;
        } catch (CertificateException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Loads a key store from the given {@code file}, tries to unlock it with the given
     * {@code keyStorePassword} and returns the key store if successful.
     *
     * @param file             the key store file
     * @param keyStorePassword the key store password
     * @return the loaded key store
     * @throws CryptoException if any cryptographic error occurs
     * @throws IOException     if an error occurs when reading the key store file
     */
    public static KeyStoreUber loadKeyStore(File file, String keyStorePassword) throws CryptoException, IOException {
        InputStream storeInputStream = null;
        try {
            storeInputStream = new BufferedInputStream(new FileInputStream(file));
            return KeyStoreUber.loadKeyStore(storeInputStream, keyStorePassword);
        } finally {
            if (null != storeInputStream) {
                storeInputStream.close();
            }
        }
    }

    /**
     * Loads a key store from the given {@code inputStream}, tries to unlock it with the given
     * {@code keyStorePassword} and returns the key store if successful.
     *
     * @param inputStream      the input stream to read the key store from
     * @param keyStorePassword the key store password
     * @return the loaded key store
     * @throws CryptoException if any cryptographic error occurs
     * @throws IOException     if an error occurs when reading the key store
     */
    public static KeyStoreUber loadKeyStore(InputStream inputStream, String keyStorePassword) throws CryptoException, IOException {
        try {
            KeyStoreUber keyStoreUber = new KeyStoreUber();
            keyStoreUber.keyStore = KeyStore.getInstance("UBER");
            keyStoreUber.keyStore.load(inputStream, keyStorePassword.toCharArray());
            keyStoreUber.generateMaps();
            return keyStoreUber;
        } catch (CertificateException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Returns the next index to store a new key.
     *
     * <p>Indices are added in the alias of the keys to make sure they are loaded in the same order
     * they were saved.
     *
     * @return the next usable index
     */
    private int getNextKeyIndex() {
        if (orderedKeyAliases.isEmpty()) {
            return 1;
        }
        return orderedKeyAliases.lastKey()+1;
    }

    /**
     * Generates the internal key maps from the contents of this key store.
     * <p/>
     * <p>These maps are used internally to manages key indices
     *
     * @throws CryptoException if any cryptographic error occurs
     */
    public void generateMaps() throws CryptoException {
        try {
            if (null != keyStore) {
                orderedKeyAliases.clear();
                keyAliasesIndices.clear();
                for (String alias : Collections.list(keyStore.aliases())) {
                    LOG.debug("generateMaps alias : {}", alias);

                    /* only process the user keys */
                    if (alias.startsWith(Constants.CRYPTO.KEY_STORE_ENCRYPTION_KEY_ALIAS) ||
                            alias.startsWith(Constants.CRYPTO.KEY_STORE_SIGNATURE_KEY_ALIAS)) {
                        String[] splitAlias = alias.split(Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR);
                        if (Constants.CRYPTO.KEY_STORE_ENCRYPTION_KEY_ALIAS.equals(splitAlias[0])) {
                            int indexInStore = Integer.parseInt(splitAlias[1]);
                            if (orderedKeyAliases.containsKey(indexInStore)) {
                                int newIndex = getNextKeyIndex();
                                orderedKeyAliases.put(newIndex, splitAlias[2]);
                                keyAliasesIndices.put(splitAlias[2], newIndex);
                            } else {
                                orderedKeyAliases.put(indexInStore, splitAlias[2]);
                                keyAliasesIndices.put(splitAlias[2], indexInStore);
                            }
                        }
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Saves this key store to the given {@code file}, with the given {@code keyStorePassword}.
     *
     * @param file             the file to save the key store
     * @param keyStorePassword the key store password
     * @throws IOException     if an error occurs when writing the key store
     * @throws CryptoException if any cryptographic error occurs
     */
    public void saveKeyStore(File file, String keyStorePassword)
            throws IOException, CryptoException {
        OutputStream keyStoreOutputStream = null;
        try {
            keyStoreOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            saveKeyStore(keyStoreOutputStream, keyStorePassword);
        } finally {
            if (null != keyStoreOutputStream) {
                keyStoreOutputStream.close();
            }
        }
    }

    /**
     * Writes this key store to the given {@code outputStream}, with the given {@code keyStorePassword}.
     *
     * @param outputStream     the output stream to write the key store to
     * @param keyStorePassword the key store password
     * @throws IOException     if an error occurs when writing the key store
     * @throws CryptoException if any cryptographic error occurs
     */
    public void saveKeyStore(OutputStream outputStream, String keyStorePassword)
            throws IOException, CryptoException {
        try {
            keyStore.store(outputStream, keyStorePassword.toCharArray());
        } catch (CertificateException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Adds the given {@code key} to this key store, with the given {@code alias}
     *
     * @param alias the key alias
     * @param key the key to add
     * @throws CryptoException if any cryptographic error occurs
     */
    private void addKey(String alias, SecretKey key) throws CryptoException {
        try {
            if (null != keyStore && null != alias && null != key) {
                KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(key);
                keyStore.setEntry(alias, secretKeyEntry, new KeyStore.PasswordProtection(alias.toCharArray()));
            }
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Deletes the key with the given {@code alias} from this key store
     *
     * @param alias the key alias
     * @throws CryptoException if any cryptographic error occurs
     */
    private void deleteKey(String alias) throws CryptoException {
        try {
            if (null != keyStore && null != alias) {
                keyStore.deleteEntry(alias);
            }
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }

    }

    /**
     * Adds the given {@code secretKeys} to this key store, as database security keys.
     *
     * @param secretKeys the secret keys to add
     * @return true if the {@code secretKeys} were successfully added
     * @throws CryptoException if any cryptographic error occurs
     */
    public boolean addDatabaseSecurityKeys(SecretKeys secretKeys) throws CryptoException {
        if (null != secretKeys) {
            addKey(DB_SECURITY_ENCRYPTION_KEY_ALIAS, secretKeys.getEncryptionKey());
            addKey(DB_SECURITY_SIGNATURE_KEY_ALIAS, secretKeys.getSignatureKey());
            return true;
        }
        return false;
    }

    /**
     * Adds the given {@code secretKeys} to this key store, at the given {@code index},
     * and with the given {@code alias}.
     *
     * @param index      the index defines the order of this key compared to the other keys
     * @param alias      the alias of the {@code secretKeys}
     * @param secretKeys the secret keys to add
     * @return true if the {@code secretKeys} were successfully added
     * @throws CryptoException if any cryptographic error occurs
     */
    public boolean addKeys(int index, String alias, SecretKeys secretKeys) throws CryptoException {
        if (null!=alias && null != secretKeys) {
            if (!orderedKeyAliases.containsKey(index) && !keyAliasesIndices.containsKey(alias)) {
                addKey(Constants.CRYPTO.KEY_STORE_ENCRYPTION_KEY_ALIAS
                                + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + index
                                + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + alias,
                        secretKeys.getEncryptionKey());
                addKey(Constants.CRYPTO.KEY_STORE_SIGNATURE_KEY_ALIAS
                                + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + index
                                + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + alias,
                        secretKeys.getSignatureKey());
                orderedKeyAliases.put(index, alias);
                keyAliasesIndices.put(alias, index);
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the given {@code secretKeys} to this key store, with the given {@code alias}, after the
     * existing keys.
     *
     * @param alias      the alias of the {@code secretKeys}
     * @param secretKeys the secret keys to add
     * @return true if the {@code secretKeys} were successfully added
     * @throws CryptoException if any cryptographic error occurs
     */
    public boolean addKeys(String alias, SecretKeys secretKeys) throws CryptoException {
        return addKeys(getNextKeyIndex(), alias, secretKeys);
    }

    /**
     * Deletes the {@link SecretKeys} with the given {@code alias} from this key store.
     *
     * @param alias the alias of the {@code SecretKeys} to delete
     * @throws CryptoException if any cryptographic error occurs
     */
    public void deleteKeys(String alias) throws CryptoException {
        if (null!=alias) {
            int index = keyAliasesIndices.get(alias);
            deleteKey(Constants.CRYPTO.KEY_STORE_ENCRYPTION_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + index
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + alias);
            deleteKey(Constants.CRYPTO.KEY_STORE_SIGNATURE_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + index
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + alias);
            orderedKeyAliases.remove(index);
            keyAliasesIndices.remove(alias);
        }
    }

    /**
     * Renames the alias of a {@link SecretKeys} in this key store, preserving its index.
     *
     * @param oldAlias the old alias
     * @param newAlias the new alias
     * @return true if the alias was successfully renamed
     * @throws CryptoException if any cryptographic error occurs
     */
    public boolean renameKeys(String oldAlias, String newAlias) throws CryptoException {
        SecretKeys keys = getKeys(oldAlias);
        if (null==keys || null!=getKeys(newAlias)) {
            return false;
        }
        int index = keyAliasesIndices.get(oldAlias);
        deleteKeys(oldAlias);
        addKeys(index, newAlias, keys);
        return true;
    }

    /**
     * Returns the {@code SecretKey} with the given {@code alias}
     *
     * @param alias the alias of the {@code SecretKey}
     * @return the {@code SecretKey} with the given {@code alias} if one was found, null otherwise
     * @throws CryptoException if any cryptographic error occurs
     */
    private SecretKey getKey(String alias) throws CryptoException {
        try {
            if (null != keyStore && null != alias) {
                KeyStore.Entry entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(alias.toCharArray()));
                if (null != entry && entry instanceof KeyStore.SecretKeyEntry) {
                    KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) entry;
                    return secretKeyEntry.getSecretKey();
                }
            }
            return null;
        } catch (UnrecoverableEntryException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Returns the "database security keys".
     *
     * @return the database security keys, if found, or null
     * @throws CryptoException if any cryptographic error occurs
     */
    public SecretKeys getDatabaseSecurityKeys() throws CryptoException {
        SecretKey encryptionKey = getKey(DB_SECURITY_ENCRYPTION_KEY_ALIAS);
        SecretKey signatureKey = getKey(DB_SECURITY_SIGNATURE_KEY_ALIAS);
        if (null == encryptionKey || null == signatureKey) {
            return null;
        }
        return new SecretKeys(encryptionKey, signatureKey);
    }

    /**
     * Returns the {@code SecretKeys} with the given {@code alias}.
     *
     * @param alias the alias of the {@code SecretKeys}
     * @return the {@code SecretKeys} with the given {@code alias} if one was found, null otherwise
     * @throws CryptoException if any cryptographic error occurs
     */
    public SecretKeys getKeys(String alias) throws CryptoException {
        if (!keyAliasesIndices.containsKey(alias)) {
            return null;
        }
        SecretKey encryptionKey = null;
        SecretKey signatureKey = null;
        if (keyAliasesIndices.containsKey(alias)) {
            int index = keyAliasesIndices.get(alias);
            encryptionKey = getKey(Constants.CRYPTO.KEY_STORE_ENCRYPTION_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + index
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + alias);
            signatureKey = getKey(Constants.CRYPTO.KEY_STORE_SIGNATURE_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + index
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR + alias);
        }
        if (null == encryptionKey || null == signatureKey) {
            return null;
        }
        return new SecretKeys(encryptionKey, signatureKey);
    }

    /**
     * Returns the list of the key aliases, ordered by their index.
     *
     * @return the list of the key aliases, ordered by their index
     */
    public List<String> getKeyAliases() {
        ArrayList<String> aliases = new ArrayList<>();
        aliases.addAll(orderedKeyAliases.values());
        return aliases;
    }
}