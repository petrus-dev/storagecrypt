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

package fr.petrus.lib.core.i18n;

import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.StorageType;

/**
 * This interface provides methods which returns the translated textual representation of various types.
 *
 * @author Pierre Sagne
 * @since 03.07.2015.
 */
public interface TextI18n {
    /**
     * Returns the textual representation of the given {@code size}.
     *
     * @param size the size, in bytes
     * @return the textual representation of the given {@code size}
     */
    String getSizeText(long size);

    /**
     * Returns the textual representation of the given {@code time}.
     *
     * @param time the time, in ms from the epoch
     * @return the textual representation of the given {@code time}
     */
    String getTimeText(long time);

    /**
     * Returns the textual representation of the given {@code storageType}.
     *
     * @param storageType the storage type
     * @return the textual representation of the given {@code storageType}
     */
    String getStorageTypeText(StorageType storageType);

    /**
     * Returns the textual representation of the storage account, with the given {@code storageType},
     * and the given {@code account} user name.
     *
     * <p>If the {@code storageType} is {@link StorageType#Unsynchronized}, there is no user name.
     *
     * @param storageType the storage type
     * @param account     the account
     * @return the textual representation
     */
    String getStorageText(StorageType storageType, Account account);

    /**
     * Returns the description of the given {@code StorageCryptException}.
     *
     * @param exception the {@code StorageCryptException}
     * @return the description of the given {@code StorageCryptException}
     */
    String getExceptionDescription(StorageCryptException exception);
}
