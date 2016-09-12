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

package fr.petrus.lib.core.cloud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the changes on a {@link RemoteStorage} :
 * which documents were created, modified or deleted.
 *
 * @author Pierre Sagne
 * @since 09.11.2015
 */
public class RemoteChanges {
    private String lastChangeId;
    private List<RemoteChange> changes;

    /**
     * Creates a new empty instance.
     */
    public RemoteChanges() {
        this(null);
    }

    /**
     * Creates a new empty instance, with id of the last change of the list.
     *
     * @param lastChangeId the last changes id
     */
    public RemoteChanges(String lastChangeId) {
        this.lastChangeId = lastChangeId;
        changes = new ArrayList<>();
    }

    /**
     * Sets the last change id.
     *
     * @param lastChangeId the last change id
     */
    public void setLastChangeId(String lastChangeId) {
        this.lastChangeId = lastChangeId;
    }

    /**
     * Returns the last change id.
     *
     * @return the last change id
     */
    public String getLastChangeId() {
        return lastChangeId;
    }

    /**
     * Adds a change to the list.
     *
     * @param change     the change object to add
     */
    public void addChange(RemoteChange change) {
        changes.add(change);
    }

    /**
     * Returns the changes (defensive copy).
     *
     * @return the changes
     */
    public List<RemoteChange> getChanges() {
        return new ArrayList<>(changes);
    }
}
