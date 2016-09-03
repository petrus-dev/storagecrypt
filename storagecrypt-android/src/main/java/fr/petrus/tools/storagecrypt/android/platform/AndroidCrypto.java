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

package fr.petrus.tools.storagecrypt.android.platform;

import android.util.Base64;

import java.security.Security;

import fr.petrus.lib.core.crypto.AbstractCrypto;
import fr.petrus.lib.core.crypto.Crypto;

/**
 * The {@link Crypto} implementation for the Android platform.
 *
 * <p>This implementation uses SpongyCastle.
 *
 * @author Pierre Sagne
 * @since 11.08.2015
 */
public class AndroidCrypto extends AbstractCrypto {

    @Override
    public void initProvider() {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    @Override
    public String encodeBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    @Override
    public byte[] decodeBase64(String data) {
        return Base64.decode(data, Base64.NO_WRAP);
    }

    @Override
    public String encodeUrlSafeBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
    }

    @Override
    public byte[] decodeUrlSafeBase64(String data) {
        return Base64.decode(data, Base64.URL_SAFE);
    }
}
