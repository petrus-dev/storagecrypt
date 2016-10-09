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

package fr.petrus.tools.storagecrypt.desktop.platform.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

import org.bouncycastle.jcajce.provider.keystore.bc.BcKeyStoreSpi.BouncyCastleStore;

import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.crypto.keystore.AbstractKeyStore;

/**
 * This class is used to perform operations on a "UBER" key store, using BouncyCastle LightWeight API.
 *
 * <p>"UBER" key stores are password protected, and can store AES keys.
 *
 * @author Pierre Sagne
 * @since 08.10.2016
 */
public class BCLightWeightApiKeyStoreUber extends AbstractKeyStore {

    private BouncyCastleStore keyStore;

    /**
     * Creates an empty {@code BCLightWeightApiKeyStoreUber} instance
     *
     * @throws CryptoException if any cryptographic error occurs
     */
    public BCLightWeightApiKeyStoreUber() throws CryptoException {
        try {
            keyStore = new BouncyCastleStore();
            keyStore.setRandom(new SecureRandom());
            keyStore.engineLoad(null);
        } catch (CertificateException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    protected List<String> keyStoreAliases() {
        return Collections.list(keyStore.engineAliases());
    }

    @Override
    protected void addKey(String alias, SecretKey key) throws CryptoException {
        if (null != alias && null != key) {
            java.security.KeyStore.SecretKeyEntry secretKeyEntry =
                    new java.security.KeyStore.SecretKeyEntry(key);
            try {
                keyStore.engineSetEntry(alias, secretKeyEntry,
                        new java.security.KeyStore.PasswordProtection(alias.toCharArray()));
            } catch (KeyStoreException e) {
                throw new CryptoException(e);
            }
        }
    }

    @Override
    protected void deleteKey(String alias) throws CryptoException {
        if (null != alias) {
            try {
                keyStore.engineDeleteEntry(alias);
            } catch (KeyStoreException e) {
                throw new CryptoException(e);
            }
        }
    }

    @Override
    protected SecretKey getKey(String alias) throws CryptoException {
        if (null != alias) {
            try {
                java.security.KeyStore.Entry entry = keyStore.engineGetEntry(alias,
                        new java.security.KeyStore.PasswordProtection(alias.toCharArray()));
                if (null != entry && entry instanceof java.security.KeyStore.SecretKeyEntry) {
                    java.security.KeyStore.SecretKeyEntry secretKeyEntry =
                            (java.security.KeyStore.SecretKeyEntry) entry;
                    return secretKeyEntry.getSecretKey();
                }
            } catch (NoSuchAlgorithmException e) {
                throw new CryptoException(e);
            } catch (UnrecoverableKeyException e) {
                throw new CryptoException(e);
            } catch (UnrecoverableEntryException e) {
                throw new CryptoException(e);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void load(InputStream inputStream, String keyStorePassword) throws IOException, CryptoException {
        keyStore.engineLoad(inputStream, keyStorePassword.toCharArray());
        generateMaps();
    }

    @Override
    public void save(OutputStream outputStream, String keyStorePassword) throws IOException, CryptoException {
        keyStore.engineStore(outputStream, keyStorePassword.toCharArray());
        generateMaps();
    }
}
