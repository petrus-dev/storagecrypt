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

import java.util.Locale;

import fr.petrus.lib.core.StorageType;
import fr.petrus.lib.core.cloud.Account;

/**
 * The abstract implementation of the {@code TextI18n} interface.
 *
 * <p>The real implementations should extend this abstract class.
 *
 * @author Pierre Sagne
 * @since 26.03.2016
 */
public abstract class AbstractTextI18n implements TextI18n {
    private static final long KB = 1024;
    private static final long MB = 1024*KB;
    private static final long GB = 1024*MB;
    private static final long TB = 1024*GB;

    @Override
    public String getSizeText(long size) {
        String sizeText;
        if (size >= TB) {
            sizeText = String.format(Locale.getDefault(), "%.2f TB", (size / (float) TB));
        } else if (size >= GB) {
            sizeText = String.format(Locale.getDefault(), "%.2f GB", (size / (float) GB));
        } else if (size >= MB) {
            sizeText = String.format(Locale.getDefault(), "%.2f MB", (size / (float) MB));
        } else if (size >= KB) {
            sizeText = String.format(Locale.getDefault(), "%.2f KB", (size / (float) KB));
        } else {
            sizeText = size + " B";
        }
        return sizeText;
    }

    @Override
    public String getStorageText(StorageType storageType, Account account) {
        switch(storageType) {
            case Unsynchronized:
                return getStorageTypeText(storageType);
            case GoogleDrive:
            case Dropbox:
            case Box:
            case HubiC:
            case OneDrive:
                return getStorageTypeText(storageType) + " : " + account.getAccountName();
        }
        return null;
    }
}
