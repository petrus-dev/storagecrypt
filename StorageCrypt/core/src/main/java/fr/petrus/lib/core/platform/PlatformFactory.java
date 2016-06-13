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

package fr.petrus.lib.core.platform;

import fr.petrus.lib.core.cloud.appkeys.CloudAppKeys;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.Database;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.lib.core.tasks.Task;

/**
 * Factory interface for creating instances of various platform dependant classes,
 * without knowledge of the underlying platform.
 *
 * @author Pierre Sagne
 * @since 22.03.2016
 */
public interface PlatformFactory {
    /**
     * Creates an instance of the platform dependant {@code Crypto} implementation.
     *
     * @return the interface of the newly created {@code Crypto} implementation
     */
    Crypto crypto();

    /**
     * Creates a {@code KeyManager} instance, providing its dependancies.
     *
     * @param crypto a {@code Crypto} instance
     * @return the newly created {@code KeyManager} instance
     */
    KeyManager keyManager(Crypto crypto);

    /**
     * Creates an instance of the platform dependant {@code FileSystem} implementation.
     *
     * @return the interface of the newly created {@code FileSystem} implementation
     */
    FileSystem fileSystem();

    /**
     * Creates an instance of the platform dependant {@code CloudAppKeys} implementation.
     *
     * @param fileSystem a {@code FileSystem} instance
     * @return the interface of the newly created {@code CloudAppKeys} implementation
     */
    CloudAppKeys cloudAppKeys(FileSystem fileSystem);

    /**
     * Creates an instance of the platform dependant {@code Network} implementation.
     *
     * @return the interface of the newly created {@code Network} implementation
     */
    Network network();

    /**
     * Creates an instance of the platform dependant {@code TextI18n} implementation.
     *
     * @return the interface of the newly created {@code TextI18n} implementation
     */
    TextI18n textI18n();

    /**
     * Creates an instance of the {@code Database} implementation.
     *
     * @param fileSystem a {@code FileSystem} instance
     * @param textI18n a {@code TextI18n} instance
     * @return the interface of the newly created {@code Database} implementation
     */
    Database database(FileSystem fileSystem, TextI18n textI18n);

    /**
     * Creates a task of the given {@code taskClass}, providing dependencies from the given {@code appContext}.
     *
     * @param appContext the {@code AppContext} providing the task dependencies
     * @param taskClass  the task class
     * @return interface of the newly created {@code Task}
     * @throws TaskCreationException if an error occurs when creating the task
     */
    <T extends Task> T task(AppContext appContext, Class<T> taskClass) throws TaskCreationException;
}
