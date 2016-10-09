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

import android.content.Context;

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
import fr.petrus.tools.storagecrypt.android.platform.crypto.AndroidBCLightWeightApiCrypto;
import fr.petrus.tools.storagecrypt.android.platform.crypto.AndroidJcaCrypto;

/**
 * The {@link PlatformFactory} implementation for the Android platform.
 *
 * @author Pierre Sagne
 * @since 22.03.2016
 */
public class AndroidPlatformFactory implements PlatformFactory {
    private Context context;
    private boolean useJca;

    /**
     * Creates a new {@code AndroidPlatformFactory} instance.
     *
     * @param context the Android context
     */
    public AndroidPlatformFactory(Context context) {
        this(context, false);
    }

    /**
     * Creates a new {@code AndroidPlatformFactory} instance.
     *
     * @param context the Android context
     * @param useJca if true, the crypto engine will use Java Cryptography Architecture, otherwise
     *               it will use BouncyCastle lightweight API
     */
    public AndroidPlatformFactory(Context context, boolean useJca) {
        this.context = context.getApplicationContext();
        this.useJca = useJca;
    }

    @Override
    public Crypto crypto() {
        if (useJca) {
            return new AndroidJcaCrypto();
        } else {
            return new AndroidBCLightWeightApiCrypto();
        }
    }

    @Override
    public KeyManager keyManager(Crypto crypto) {
        return new KeyManager(crypto, fileSystem().getAppDir());
    }

    @Override
    public FileSystem fileSystem() {
        return new AndroidFileSystem(context);
    }

    @Override
    public CloudAppKeys cloudAppKeys(FileSystem fileSystem) {
        return new AndroidCloudAppKeys(context, fileSystem);
    }

    @Override
    public Network network() {
        return new AndroidNetwork(context);
    }

    @Override
    public TextI18n textI18n() {
        return new AndroidTextI18n(context);
    }

    @Override
    public Database database(FileSystem fileSystem, TextI18n textI18n) {
        return new H2Database(AndroidFileSystem.getInternalStoragePath(context), textI18n);
    }

    @Override
    public <T extends Task> T task(AppContext appContext, Class<T> taskClass)
            throws TaskCreationException {
        try {
            return taskClass.getConstructor(AppContext.class, Context.class)
                    .newInstance(appContext, context);
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
