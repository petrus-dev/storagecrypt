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

import android.content.Context;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.tools.storagecrypt.R;

/**
 * An {@code ArrayAdapter} which handles items selection.
 *
 * @param <C> the class of the objects to list
 *
 * @author Pierre Sagne
 * @since 21.09.2016
 */
public class SelectedItemArrayAdapter<C> extends ArrayAdapter<SelectedItem<C>> {

    /**
     * Returns the {@code SelectedItem} at the given {@code position} of the given {@code listView}.
     *
     * @param listView the list where the items are displayed
     * @param position the position of the items in the list
     * @return the {@code SelectedItem} at the given {@code position} of the given {@code listView}
     */
    public static <C> SelectedItem<C> getListItemAt(ListView listView, int position) {
        ListAdapter adapter = listView.getAdapter();
        if (null!=adapter && adapter instanceof SelectedItemArrayAdapter) {
            return ((SelectedItemArrayAdapter<C>) adapter).getItem(position);
        }
        return null;
    }

    /**
     * Returns a list containing all the {@code SelectedItem}s displayed in the given {@code listView}.
     *
     * @param listView the list where the {@code SelectedItem}s are displayed
     * @return the list containing all the {@code SelectedItem}s displayed in the given {@code listView}
     */
    public static <C> List<SelectedItem<C>> getListItems(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (null!=adapter && adapter instanceof SelectedItemArrayAdapter) {
            return ((SelectedItemArrayAdapter<C>) adapter).selectedItems;
        }
        return null;
    }

    protected Context context;
    protected int layoutResourceId;
    protected List<SelectedItem<C>> selectedItems;

    /**
     * Creates a new {@code SelectedItemArrayAdapter} instance.
     *
     * @param context       the Android context
     * @param selectedItems the list of {@code SelectedItem}s managed by this adapter
     */
    public SelectedItemArrayAdapter(Context context, List<SelectedItem<C>> selectedItems) {
        this(context, R.layout.row_item_select, selectedItems);
    }

    /**
     * Creates a new {@code SelectedItemArrayAdapter} instance.
     *
     * @param context          the Android context
     * @param layoutResourceId the resource id of the layout used to display a row of the list
     * @param selectedItems    the list of {@code SelectedItem}s managed by this adapter
     */
    public SelectedItemArrayAdapter(Context context, int layoutResourceId,
                                    List<SelectedItem<C>> selectedItems) {
        super(context, layoutResourceId, selectedItems);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.selectedItems = selectedItems;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(layoutResourceId, null);
        }

        final SelectedItem<C> selectedItem = selectedItems.get(position);

        if (null != selectedItem) {
            final CheckBox itemCheckBox = (CheckBox) view.findViewById(R.id.item_checkbox);
            final TextView itemTextView = (TextView) view.findViewById(R.id.item_text);
            if (null != itemCheckBox && null != itemTextView) {
                itemCheckBox.setChecked(selectedItem.isSelected());
                itemCheckBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selectedItem.isSelected()) {
                            selectedItem.setSelected(false);
                            itemCheckBox.setSelected(false);
                        } else {
                            selectedItem.setSelected(true);
                            itemCheckBox.setSelected(true);
                        }
                    }
                });
                itemTextView.setText(selectedItem.getObject().toString());
            }
        }

        return view;
    }
}