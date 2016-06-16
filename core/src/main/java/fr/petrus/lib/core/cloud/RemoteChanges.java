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

import java.util.LinkedHashMap;
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
    private Map<String, RemoteChange> changes;

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
        changes = new LinkedHashMap<>();
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
     * @param documentId               the document id
     * @param change                   the change object to add
     * @param preserveFirstChangeOrder if true, when adding a change for a document with a
     *                                 change already in the list, the new change is inserted
     *                                 at the same place. If false, the previous change is removed,
     *                                 and the new one is added at the end of the list.
     */
    public void putChange(String documentId, RemoteChange change, boolean preserveFirstChangeOrder) {
        if (preserveFirstChangeOrder) {
            // if an older change is already present, remove it first,
            // so the LinkedHashMap stores the order of the last change, not the first one.
            if (changes.containsKey(documentId)) {
                changes.remove(documentId);
            }
        }
        changes.put(documentId, change);
    }

    /**
     * Adds a change to the list.
     * <p>When adding a change for a document with a change already in the list,
     * the new change is inserted at the same place.
     *
     * @param documentId the document id
     * @param change     the change object to add
     */
    public void putChange(String documentId, RemoteChange change) {
        putChange(documentId, change, true);
    }

    /**
     * Returns the changes as a map.
     *
     * <p>The map keys are the documents remote ids or the change ids
     *
     * @return the changes
     */
    public Map<String, RemoteChange> getChanges() {
        return changes;
    }

    /**
     * Return a copy of the changes, so that it can be modified.
     *
     * @return the changes copy
     */
    public Map<String, RemoteChange> getChangesCopy() {
        Map<String, RemoteChange> changesCopy = new LinkedHashMap<>();
        changesCopy.putAll(changes);
        return changesCopy;
    }

    /**
     * Returns the changes targeting folders only.
     *
     * @return the folder changes
     */
    public Map<String, RemoteChange> getFolderChanges() {
        Map<String, RemoteChange> folderChanges = new LinkedHashMap<>();
        for (Map.Entry<String, RemoteChange> entry : changes.entrySet()) {
            RemoteChange change = entry.getValue();
            if (null!=change.getDocument() && change.getDocument().isFolder()) {
                folderChanges.put(entry.getKey(), change);
            }
        }
        return folderChanges;
    }
}
