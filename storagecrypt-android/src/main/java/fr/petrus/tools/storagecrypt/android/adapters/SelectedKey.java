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

package fr.petrus.tools.storagecrypt.android.adapters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds the alias of a key, along with a selection flag and an optional new alias if
 * the key is renamed
 *
 * @author Pierre Sagne
 * @since 20.07.2015
 */
public class SelectedKey {
    private String keyAlias;
    private boolean selected;
    private String newKeyAlias;

    /**
     * Creates a new {@code SelectedKey} instance.
     *
     * @param keyAlias    the key alias
     * @param selected    if true, the key is considered selected
     * @param newKeyAlias the new key alias, if the key is renamed
     */
    public SelectedKey(String keyAlias, boolean selected, String newKeyAlias) {
        this.keyAlias = keyAlias;
        this.selected = selected;
        this.newKeyAlias = newKeyAlias;
    }

    /**
     * Returns the key alias.
     *
     * @return the key alias
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Sets the selection state of this key.
     *
     * @param selected if true, the key is considered selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns whether this key is selected.
     *
     * @return true if this key is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the new key alias.
     *
     * @param newKeyAlias the new key alias
     */
    public void setNewKeyAlias(String newKeyAlias) {
        this.newKeyAlias = newKeyAlias;
    }

    /**
     * Returns the new key alias.
     *
     * @return the new key alias
     */
    public String getNewKeyAlias() {
        return newKeyAlias;
    }

    /**
     * Builds and returns a {@code Map} from a list of {@code SelectedKey}s.
     *
     * <p>The keys of the map are the key aliases.
     * <p>The values of the map are the new key aliases (or the origial key alias it the new key alias
     * is null.
     * <p>Only the selected keys are put into the {@code Map}
     *
     * @param selectedKeys the list of {@code SelectedKey}s
     * @return the map containing the selected keys
     */
    public static Map<String, String> selectedKeysToMap(List<SelectedKey> selectedKeys) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (SelectedKey selectedKey : selectedKeys) {
            if (selectedKey.isSelected()) {
                map.put(selectedKey.getKeyAlias(), selectedKey.getNewKeyAlias());
            }
        }
        return map;
    }
}
