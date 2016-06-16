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

package fr.petrus.tools.storagecrypt.desktop.platform;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.cloud.appkeys.AbstractCloudAppKeys;
import fr.petrus.lib.core.cloud.appkeys.AllAppKeys;
import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.filesystem.FileSystem;

/**
 * The {@link CloudAppKeys} implementation for the "Desktop" platform.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class DesktopCloudAppKeys extends AbstractCloudAppKeys {

    private static Logger LOG = LoggerFactory.getLogger(DesktopCloudAppKeys.class);

    /**
     * Creates a new {@code DesktopCloudAppKeys} instance, and reads the API keys.
     *
     * <p>If the keys json file is not found inside the app jar, try to read it in the app folder.
     *
     * @param fileSystem the {@code FileSystem} instance used to read the json file
     */
    DesktopCloudAppKeys(FileSystem fileSystem) {
        InputStream in = null;
        if (null!=getClass().getResource("/res/json/keys.json")) {
            in = getClass().getResourceAsStream("/res/json/keys.json");
        } else {
            File externalAppKeysFile = new File(fileSystem.getAppDir(), Constants.FILE.EXTERNAL_APP_KEYS_FILE);
            if (externalAppKeysFile.exists()) {
                try {
                    in = new FileInputStream(externalAppKeysFile);
                } catch (FileNotFoundException e) {
                    LOG.error("Failed to open external cloud app keys file : {}", externalAppKeysFile.getAbsolutePath(), e);
                }
            }
        }
        if (null!=in) {
            final Gson gson = new Gson();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            setCloudAppKeys(gson.fromJson(reader, AllAppKeys.class));
        }
    }
}
