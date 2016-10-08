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

package fr.petrus.lib.core.crypto.keystore;

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
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.SecretKeys;

/**
 * This class is used to perform operations on a "UBER" key store
 * <p/>
 * <p>"UBER" key stores are password protected, and can store AES keys.
 *
 * @author Pierre Sagne
 * @since 19.12.2014.
 */
public class JcaKeyStoreUber implements KeyStore {

    private static Logger LOG = LoggerFactory.getLogger(JcaKeyStoreUber.class);

    /* The aliases for the special keys used to encrypt the database password */
    private static final String DB_SECURITY_ENCRYPTION_KEY_ALIAS =
            Constants.CRYPTO.KEY_STORE_DATABASE_SECURITY_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR
                    + Constants.CRYPTO.KEY_STORE_ENCRYPTION_KEY_ALIAS;

    private static final String DB_SECURITY_SIGNATURE_KEY_ALIAS =
            Constants.CRYPTO.KEY_STORE_DATABASE_SECURITY_KEY_ALIAS
                    + Constants.CRYPTO.KEY_STORE_ALIAS_SEPARATOR
                    + Constants.CRYPTO.KEY_STORE_SIGNATURE_KEY_ALIAS;

    private java.security.KeyStore keyStore;
    private TreeMap<Integer, String> orderedKeyAliases;
    private HashMap<String, Integer> keyAliasesIndices;

    /**
     * Creates an empty {@code JcaKeyStoreUber} instance
     *
     * @throws CryptoException if any cryptographic error occurs
     */
    public JcaKeyStoreUber() throws CryptoException {
        try {
            keyStore = java.security.KeyStore.getInstance("UBER");
            keyStore.load(null);
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        } catch (CertificateException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
        orderedKeyAliases = new TreeMap<>();
        keyAliasesIndices = new HashMap<>();
    }

    @Override
    public void load(File file, String keyStorePassword) throws CryptoException, IOException {
        InputStream storeInputStream = null;
        try {
            storeInputStream = new BufferedInputStream(new FileInputStream(file));
            load(storeInputStream, keyStorePassword);
        } finally {
            if (null != storeInputStream) {
                storeInputStream.close();
            }
        }
    }

    @Override
    public void load(InputStream inputStream, String keyStorePassword) throws CryptoException, IOException {
        try {
            keyStore.load(inputStream, keyStorePassword.toCharArray());
            generateMaps();
        } catch (CertificateException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
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

    @Override
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

    @Override
    public void save(File file, String keyStorePassword)
            throws IOException, CryptoException {
        OutputStream keyStoreOutputStream = null;
        try {
            keyStoreOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            save(keyStoreOutputStream, keyStorePassword);
        } finally {
            if (null != keyStoreOutputStream) {
                keyStoreOutputStream.close();
            }
        }
    }

    @Override
    public void save(OutputStream outputStream, String keyStorePassword)
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
                java.security.KeyStore.SecretKeyEntry secretKeyEntry =
                        new java.security.KeyStore.SecretKeyEntry(key);
                keyStore.setEntry(alias, secretKeyEntry,
                        new java.security.KeyStore.PasswordProtection(alias.toCharArray()));
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

    @Override
    public boolean addDatabaseSecurityKeys(SecretKeys secretKeys) throws CryptoException {
        if (null != secretKeys) {
            addKey(DB_SECURITY_ENCRYPTION_KEY_ALIAS, secretKeys.getEncryptionKey());
            addKey(DB_SECURITY_SIGNATURE_KEY_ALIAS, secretKeys.getSignatureKey());
            return true;
        }
        return false;
    }

    @Override
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

    @Override
    public boolean addKeys(String alias, SecretKeys secretKeys) throws CryptoException {
        return addKeys(getNextKeyIndex(), alias, secretKeys);
    }

    @Override
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

    @Override
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
                java.security.KeyStore.Entry entry = keyStore.getEntry(alias,
                        new java.security.KeyStore.PasswordProtection(alias.toCharArray()));
                if (null != entry && entry instanceof java.security.KeyStore.SecretKeyEntry) {
                    java.security.KeyStore.SecretKeyEntry secretKeyEntry =
                            (java.security.KeyStore.SecretKeyEntry) entry;
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

    @Override
    public SecretKeys getDatabaseSecurityKeys() throws CryptoException {
        SecretKey encryptionKey = getKey(DB_SECURITY_ENCRYPTION_KEY_ALIAS);
        SecretKey signatureKey = getKey(DB_SECURITY_SIGNATURE_KEY_ALIAS);
        if (null == encryptionKey || null == signatureKey) {
            return null;
        }
        return new SecretKeys(encryptionKey, signatureKey);
    }

    @Override
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

    @Override
    public List<String> getKeyAliases() {
        ArrayList<String> aliases = new ArrayList<>();
        aliases.addAll(orderedKeyAliases.values());
        return aliases;
    }
}