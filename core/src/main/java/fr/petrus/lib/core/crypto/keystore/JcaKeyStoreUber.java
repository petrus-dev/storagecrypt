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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.crypto.CryptoException;

/**
 * This class is used to perform operations on a "UBER" key store, using JCA.
 *
 * <p>"UBER" key stores are password protected, and can store AES keys.
 *
 * @author Pierre Sagne
 * @since 19.12.2014
 */
public class JcaKeyStoreUber extends AbstractKeyStore {

    private static Logger LOG = LoggerFactory.getLogger(JcaKeyStoreUber.class);

    private java.security.KeyStore keyStore;

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

    @Override
    protected List<String> keyStoreAliases() throws CryptoException {
        try {
            return Collections.list(keyStore.aliases());
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
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

    @Override
    protected void addKey(String alias, SecretKey key) throws CryptoException {
        try {
            if (null != alias && null != key) {
                java.security.KeyStore.SecretKeyEntry secretKeyEntry =
                        new java.security.KeyStore.SecretKeyEntry(key);
                keyStore.setEntry(alias, secretKeyEntry,
                        new java.security.KeyStore.PasswordProtection(alias.toCharArray()));
            }
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    protected void deleteKey(String alias) throws CryptoException {
        try {
            if (null != alias) {
                keyStore.deleteEntry(alias);
            }
        } catch (KeyStoreException e) {
            throw new CryptoException(e);
        }

    }

    @Override
    protected SecretKey getKey(String alias) throws CryptoException {
        try {
            if (null != alias) {
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
}