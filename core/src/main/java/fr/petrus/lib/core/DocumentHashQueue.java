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

package fr.petrus.lib.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A mixed HashMap/Queue structure containing {@link EncryptedDocument}s
 *
 * @author Pierre Sagne
 * @since 30.09.2015
 */
public class DocumentHashQueue {
    private Queue<Long> queue;
    private HashMap<Long, EncryptedDocument> hashMap;


    /**
     * Creates a new {@code DocumentHashQueue} empty instance.
     */
    public DocumentHashQueue() {
        queue = new LinkedList<>();
        hashMap = new HashMap<>();
    }

    /**
     * Returns whether this {@code DocumentHashQueue} contains the given {@code encryptedDocument}.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to check
     * @return true if this {@code DocumentHashQueue} contains the given {@code EncryptedDocument}
     */
    public synchronized boolean contains(EncryptedDocument encryptedDocument) {
        return null != encryptedDocument && hashMap.containsKey(encryptedDocument.getId());
    }

    /**
     * Adds the given {@code encryptedDocument} to this {@code DocumentHashQueue}.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to add
     * @return true if the given {@code encryptedDocument} is not null
     */
    public synchronized boolean offer(EncryptedDocument encryptedDocument) {
        if (null != encryptedDocument) {
            if (!hashMap.containsKey(encryptedDocument.getId())) {
                hashMap.put(encryptedDocument.getId(), encryptedDocument);
                queue.offer(encryptedDocument.getId());
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the first {@code EncryptedDocument} from this queue, and returns it.
     *
     * @return the first {@code EncryptedDocument} from this queue
     */
    public synchronized EncryptedDocument poll() {
        Long id = queue.poll();
        if (null==id) {
            return null;
        }
        EncryptedDocument encryptedDocument = hashMap.get(id);
        return hashMap.remove(encryptedDocument.getId());
    }

    /**
     * Removes the given {@code encryptedDocument} from this queue.
     *
     * @param encryptedDocument the {@code EncryptedDocument} to remove
     */
    public synchronized void remove(EncryptedDocument encryptedDocument) {
        if (null!= encryptedDocument) {
            queue.remove(encryptedDocument.getId());
            hashMap.remove(encryptedDocument.getId());
        }
    }

    /**
     * Returns the number of {@code EncryptedDocument}s contained in this queue.
     *
     * @return the number of {@code EncryptedDocument}s contained in this queue
     */
    public synchronized int size() {
        return hashMap.size();
    }

    /**
     * Returns whether this queue is empty.
     *
     * @return true if this queue is empty
     */
    public synchronized boolean isEmpty() {
        return hashMap.isEmpty();
    }

    /**
     * Clears this queue, by removing its contents.
     */
    public synchronized void clear() {
        queue.clear();
        hashMap.clear();
    }
}
