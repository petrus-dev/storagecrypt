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

package fr.petrus.lib.core.rest.models.onedrive;

import java.util.List;

import fr.petrus.lib.core.rest.models.PrintableJson;

/**
 * This class holds the results returned by some OneDrive API calls.
 *
 * <p>It is filled with the JSON response of the API call.
 *
 * @author Pierre Sagne
 * @since 03.03.2015
 */
public class OneDriveRoot extends PrintableJson {
    public static class IdentitySet {
        public static class Identity {
            public String id;
            public String displayName;

            public Identity() {
                id = null;
                displayName = null;
            }
        }

        public Identity user;
        public Identity application;
        public Identity device;

        public IdentitySet() {
            user = null;
            application = null;
            device = null;
        }
    }

    public static class Quota {
        public Long total;
        public Long used;
        public Long remaining;
        public Long deleted;
        public String state;

        public Quota() {
            total = null;
            used = null;
            remaining = null;
            deleted = null;
            state = null;
        }
    }

    public String id;
    public IdentitySet owner;
    public Quota quota;
    public List<OneDriveItem> items;
    public OneDriveItem root;
    public List<OneDriveItem> special;

    public OneDriveRoot() {
        id = null;
        owner = null;
        quota = null;
        items = null;
        root = null;
        special = null;
    }
}
