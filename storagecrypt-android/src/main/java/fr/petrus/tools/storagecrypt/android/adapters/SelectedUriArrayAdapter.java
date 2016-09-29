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
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.utils.UriHelper;

/**
 * A {@code SelectedItemArrayAdapter} with {@code Uri} items.
 *
 * @author Pierre Sagne
 * @since 24.09.2016
 */
public class SelectedUriArrayAdapter extends SelectedItemArrayAdapter<Uri> {

    /**
     * Creates a new {@code SelectedUriArrayAdapter} instance.
     *
     * @param context       the Android context
     * @param selectedItems the list of {@code SelectedItem}s managed by this adapter
     */
    public SelectedUriArrayAdapter(Context context, List<SelectedItem<Uri>> selectedItems) {
        this(context, R.layout.row_item_select, selectedItems);
    }

    /**
     * Creates a new {@code SelectedUriArrayAdapter} instance.
     *
     * @param context          the Android context
     * @param layoutResourceId the resource id of the layout used to display a row of the list
     * @param selectedItems    the list of {@code SelectedItem}s managed by this adapter
     */
    public SelectedUriArrayAdapter(Context context, int layoutResourceId,
                                   List<SelectedItem<Uri>> selectedItems) {
        super(context, layoutResourceId, selectedItems);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(layoutResourceId, null);
        }

        final SelectedItem<Uri> selectedItem = selectedItems.get(position);

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
                final UriHelper uriHelper = new UriHelper(context, selectedItem.getObject());
                itemTextView.setText(uriHelper.getDisplayName());
            }
        }

        return view;
    }
}