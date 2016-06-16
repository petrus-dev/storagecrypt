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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.db.H2Database;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.platform.PlatformFactory;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.lib.core.tasks.Task;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

/**
 * The {@link PlatformFactory} implementation for the "Desktop" platform.
 *
 * @author Pierre Sagne
 * @since 22.03.2016
 */
public class DesktopPlatformFactory implements PlatformFactory {
    private static Logger LOG = LoggerFactory.getLogger(DesktopPlatformFactory.class);

    private AppWindow appWindow;

    /**
     * Creates a new {@code DesktopPlatformFactory} instance.
     *
     * @param appWindow the application main window
     */
    public DesktopPlatformFactory(AppWindow appWindow) {
        this.appWindow = appWindow;
    }

    @Override
    public Crypto crypto() {
        return new DesktopCrypto();
    }

    @Override
    public KeyManager keyManager(Crypto crypto) {
        return new KeyManager(crypto, fileSystem().getAppDir());
    }

    @Override
    public FileSystem fileSystem() {
        return new DesktopFileSystem();
    }

    @Override
    public CloudAppKeys cloudAppKeys(FileSystem fileSystem) {
        return new DesktopCloudAppKeys(fileSystem);
    }

    @Override
    public Network network() {
        return new DesktopNetwork();
    }

    @Override
    public TextI18n textI18n() {
        return new DesktopTextI18n(appWindow.getTextBundle());
    }

    @Override
    public Database database(FileSystem fileSystem, TextI18n textI18n) {
        return new H2Database(fileSystem.getAppDirPath(), textI18n);
    }

    @Override
    public <T extends Task> T task(AppContext appContext, Class<T> taskClass)
            throws TaskCreationException {
        try {
            return taskClass.getConstructor(AppWindow.class).newInstance(appWindow);
        } catch (InstantiationException e) {
            throw new TaskCreationException("Failed to create task", taskClass, e);
        } catch (IllegalAccessException e) {
            throw new TaskCreationException("Failed to create task", taskClass, e);
        } catch (InvocationTargetException e) {
            throw new TaskCreationException("Failed to create task", taskClass, e);
        } catch (NoSuchMethodException e) {
            throw new TaskCreationException("Failed to create task", taskClass, e);
        }
    }
}
