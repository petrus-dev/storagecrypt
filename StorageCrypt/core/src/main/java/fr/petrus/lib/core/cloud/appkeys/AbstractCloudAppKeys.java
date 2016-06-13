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

package fr.petrus.lib.core.cloud.appkeys;

import fr.petrus.lib.core.cloud.RemoteStorage;

/**
 * This abstract class implements the base methods of {@link CloudAppKeys}
 *
 * @author Pierre Sagne
 * @since 24.03.2016
 */
public class AbstractCloudAppKeys implements CloudAppKeys {

    /** The API keys of all implementations of {@link RemoteStorage} */
    private AllAppKeys allAppKeys = null;

    /**
     * Sets API keys of all implementations of {@link RemoteStorage}.
     *
     * @param allAppKeys the API keys of all implementations of {@link RemoteStorage}
     */
    protected void setCloudAppKeys(AllAppKeys allAppKeys) {
        this.allAppKeys = allAppKeys;
    }

    /**
     * {@inheritDoc}
     * This implementation simply tests if {@code allAppKeys} is not null.
     */
    @Override
    public boolean found() {
        return null!=allAppKeys;
    }

    /**
     * {@inheritDoc}
     * This implementation returns Google Drive API keys or null if {@code allAppKeys} is null.
     */
    @Override
    public AppKeys getGoogleDriveAppKeys() {
        if (null==allAppKeys) {
            return null;
        }
        return allAppKeys.getGoogleDrive();
    }

    /**
     * {@inheritDoc}
     * This implementation returns Dropbox API keys or null if {@code allAppKeys} is null.
     */
    @Override
    public AppKeys getDropboxAppKeys() {
        if (null==allAppKeys) {
            return null;
        }
        return allAppKeys.getDropbox();
    }

    /**
     * {@inheritDoc}
     * This implementation returns Box API keys or null if {@code allAppKeys} is null.
     */
    @Override
    public AppKeys getBoxAppKeys() {
        if (null==allAppKeys) {
            return null;
        }
        return allAppKeys.getBox();
    }

    /**
     * {@inheritDoc}
     * This implementation returns HubiC API keys or null if {@code allAppKeys} is null.
     */
    @Override
    public AppKeys getHubicAppKeys() {
        if (null==allAppKeys) {
            return null;
        }
        return allAppKeys.getHubic();
    }

    /**
     * {@inheritDoc}
     * This implementation returns OneDrive API keys or null if {@code allAppKeys} is null.
     */
    @Override
    public AppKeys getOneDriveAppKeys() {
        if (null==allAppKeys) {
            return null;
        }
        return allAppKeys.getOneDrive();
    }
}
