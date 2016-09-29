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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * This class holds an object of class C, along with a selection flag
 *
 * @param <C> the class of the object
 *
 * @author Pierre Sagne
 * @since 21.09.2016
 */
public class SelectedItem<C> {
    private C object;
    private boolean selected;

    /**
     * Creates a new {@code SelectedItem} instance.
     *
     * @param object    the object referenced by this item
     * @param selected    if true, the key is considered selected
     */
    public SelectedItem(C object, boolean selected) {
        this.object = object;
        this.selected = selected;
    }

    /**
     * Returns the referenced object.
     *
     * @return the referenced object
     */
    public C getObject() {
        return object;
    }

    /**
     * Sets the selection state.
     *
     * @param selected if true, this item is considered selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns whether this item is selected.
     *
     * @return true if this item is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Builds and returns a {@code List} of selected objects.
     *
     * @param selectedItems the list of {@code SelectedItem}s
     * @return the list containing the objects referenced in the selected items
     */
    public static <C> List<C> selectedToList(List<SelectedItem<C>> selectedItems) {
        List<C> list = new ArrayList<>();
        for (SelectedItem<C> selectedItem : selectedItems) {
            if (selectedItem.isSelected()) {
                list.add(selectedItem.getObject());
            }
        }
        return list;
    }

    /**
     * Builds and returns a {@code Set} of selected or deselected objects.
     *
     * @param selected      if true, this method returns the objects referenced in the selected items
     * @param selectedItems the list of {@code SelectedItem}s
     * @return the {@code Set} containing the objects referenced in the selected or deselected items
     */
    public static <C> Set<C> itemsToSet(boolean selected, List<SelectedItem<C>> selectedItems) {
        Set<C> set = new HashSet<>();
        for (SelectedItem<C> selectedItem : selectedItems) {
            if (selectedItem.isSelected() == selected) {
                set.add(selectedItem.getObject());
            }
        }
        return set;
    }
}
