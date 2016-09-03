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

package fr.petrus.tools.storagecrypt.android.test;

import android.content.Context;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.UnsupportedEncodingException;

import javax.crypto.SecretKey;

import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.CryptoException;
import fr.petrus.lib.core.platform.PlatformFactory;
import fr.petrus.tools.storagecrypt.android.platform.AndroidPlatformFactory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test the cryptography methods
 *
 * @author Pierre Sagne
 * @since 26.08.2016
 */
@RunWith(MockitoJUnitRunner.class)
public class CryptoTest {

    @Mock
    Context context;

    private Crypto crypto = null;

    @Before
    public void init() {
        when(context.getApplicationContext()).thenReturn(context);
        PlatformFactory platformFactory = new AndroidPlatformFactory(context);
        crypto = platformFactory.crypto();
        crypto.initProvider();
    }

    @Test
    public void isAes256Supported() {
        assertTrue(crypto.isAes256Supported());
    }

    @Test
    @Ignore
    /* This test will not work on the local JVM because SpongyCastle jar is not signed */
    public void cryptDecrypt() throws UnsupportedEncodingException, CryptoException {
        byte[] data = "clear text".getBytes("UTF-8");
        SecretKey encryptionKey = crypto.generateEncryptionKey(256);
        assertArrayEquals(crypto.decrypt(encryptionKey, crypto.encrypt(encryptionKey, data)), data);
    }

    @Test
    @Ignore
    /* This test will not work on the local JVM because android.util.Base64 is not mocked */
    public void base64EncodeDecode() throws UnsupportedEncodingException, CryptoException {
        byte[] data = "clear text".getBytes("UTF-8");
        assertArrayEquals(crypto.decodeBase64(crypto.encodeBase64(data)), data);
    }

    @Test
    @Ignore
    /* This test will not work on the local JVM because android.util.Base64 is not mocked */
    public void urlSafebase64EncodeDecode() throws UnsupportedEncodingException, CryptoException {
        byte[] data = "clear text".getBytes("UTF-8");
        assertArrayEquals(crypto.decodeUrlSafeBase64(crypto.encodeUrlSafeBase64(data)), data);
    }

}
