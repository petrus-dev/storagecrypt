package fr.petrus.lib.core.crypto.keystore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.SecretKeys;

/**
 * The interface used to access a KeyStore
 *
 * @author Pierre Sagne
 * @since 06.10.2016
 */
public interface KeyStore {
    /**
     * Loads a key store from the given {@code file} with the given {@code keyStorePassword}.
     *
     * @param file             the key store file
     * @param keyStorePassword the key store password
     *
     * @throws CryptoException if any cryptographic error occurs
     * @throws IOException     if an error occurs when reading the key store file
     */
    void load(File file, String keyStorePassword) throws CryptoException, IOException;

    /**
     * Loads a key store from the given {@code inputStream} with the given {@code keyStorePassword}.
     *
     * @param inputStream      the input stream to read the key store from
     * @param keyStorePassword the key store password
     *
     * @throws CryptoException if any cryptographic error occurs
     * @throws IOException     if an error occurs when reading the key store
     */
    void load(InputStream inputStream, String keyStorePassword) throws CryptoException, IOException;

    /**
     * Generates the internal key maps from the contents of this key store.
     * <p/>
     * <p>These maps are used internally to manages key indices
     *
     * @throws CryptoException if any cryptographic error occurs
     */
    void generateMaps() throws CryptoException;

    /**
     * Saves this key store to the given {@code file}, with the given {@code keyStorePassword}.
     *
     * @param file             the file to save the key store
     * @param keyStorePassword the key store password
     * @throws IOException     if an error occurs when writing the key store
     * @throws CryptoException if any cryptographic error occurs
     */
    void save(File file, String keyStorePassword)
            throws IOException, CryptoException;

    /**
     * Writes this key store to the given {@code outputStream}, with the given {@code keyStorePassword}.
     *
     * @param outputStream     the output stream to write the key store to
     * @param keyStorePassword the key store password
     * @throws IOException     if an error occurs when writing the key store
     * @throws CryptoException if any cryptographic error occurs
     */
    void save(OutputStream outputStream, String keyStorePassword)
            throws IOException, CryptoException;

    /**
     * Adds the given {@code secretKeys} to this key store, as database security keys.
     *
     * @param secretKeys the secret keys to add
     * @return true if the {@code secretKeys} were successfully added
     * @throws CryptoException if any cryptographic error occurs
     */
    boolean addDatabaseSecurityKeys(SecretKeys secretKeys) throws CryptoException;

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
    boolean addKeys(int index, String alias, SecretKeys secretKeys) throws CryptoException;

    /**
     * Adds the given {@code secretKeys} to this key store, with the given {@code alias}, after the
     * existing keys.
     *
     * @param alias      the alias of the {@code secretKeys}
     * @param secretKeys the secret keys to add
     * @return true if the {@code secretKeys} were successfully added
     * @throws CryptoException if any cryptographic error occurs
     */
    boolean addKeys(String alias, SecretKeys secretKeys) throws CryptoException;

    /**
     * Deletes the {@link SecretKeys} with the given {@code alias} from this key store.
     *
     * @param alias the alias of the {@code SecretKeys} to delete
     * @throws CryptoException if any cryptographic error occurs
     */
    void deleteKeys(String alias) throws CryptoException;

    /**
     * Renames the alias of a {@link SecretKeys} in this key store, preserving its index.
     *
     * @param oldAlias the old alias
     * @param newAlias the new alias
     * @return true if the alias was successfully renamed
     * @throws CryptoException if any cryptographic error occurs
     */
    boolean renameKeys(String oldAlias, String newAlias) throws CryptoException;

    /**
     * Returns whether this keystore is unlocked and has "database security keys".
     *
     * @return true if this keystore is unlocked and has "database security keys"
     */
    boolean hasDatabaseSecurityKeys();

    /**
     * Returns the "database security keys".
     *
     * @return the database security keys, if found, or null
     * @throws CryptoException if any cryptographic error occurs
     */
    SecretKeys getDatabaseSecurityKeys() throws CryptoException;

    /**
     * Returns the {@code SecretKeys} with the given {@code alias}.
     *
     * @param alias the alias of the {@code SecretKeys}
     * @return the {@code SecretKeys} with the given {@code alias} if one was found, null otherwise
     * @throws CryptoException if any cryptographic error occurs
     */
    SecretKeys getKeys(String alias) throws CryptoException;

    /**
     * Returns the list of the key aliases, ordered by their index.
     *
     * @return the list of the key aliases, ordered by their index
     */
    List<String> getKeyAliases();
}
