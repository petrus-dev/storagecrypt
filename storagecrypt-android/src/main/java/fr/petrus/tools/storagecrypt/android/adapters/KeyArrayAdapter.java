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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import fr.petrus.tools.storagecrypt.R;

/**
 * The {@code ArrayAdapter} for key aliases.
 *
 * @author Pierre Sagne
 * @since 15.06.2015
 */
public class KeyArrayAdapter extends ArrayAdapter<String> {

    /**
     * Returns the key alias at the given {@code position} of the given {@code listView}.
     *
     * @param listView the list where the key aliases are displayed
     * @param position the position of the key alias in the list
     * @return the key alias at the given {@code position} of the given {@code listView}
     */
    public static String getListItemAt(ListView listView, int position) {
        ListAdapter adapter = listView.getAdapter();
        if (null!=adapter && adapter instanceof KeyArrayAdapter) {
            KeyArrayAdapter keyArrayAdapter = (KeyArrayAdapter) adapter;
            if (position >=0 && position < keyArrayAdapter.getCount()) {
                return keyArrayAdapter.getItem(position);
            }
        }
        return null;
    }

    private Context context;
    private int layoutResourceId;
    private List<String> keyAliases;

    /**
     * Creates a new {@code KeyArrayAdapter} instance.
     *
     * @param context    the Android context
     * @param keyAliases the list of key aliases managed by this adapter
     */
    public KeyArrayAdapter(Context context, List<String> keyAliases) {
        this(context, R.layout.row_key, keyAliases);
    }

    /**
     * Creates a new {@code KeyArrayAdapter} instance.
     *
     * @param context          the Android context
     * @param layoutResourceId the resource id of the layout used to display a row of the list
     * @param keyAliases       the list of key aliases managed by this adapter
     */
    public KeyArrayAdapter(Context context, int layoutResourceId, List<String> keyAliases) {
        super(context, layoutResourceId, keyAliases);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.keyAliases = keyAliases;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(layoutResourceId, null);
        }

        String keyAlias = keyAliases.get(position);

        if (null != keyAlias) {
            TextView keyAliasText = (TextView) view.findViewById(R.id.key_alias_text);

            if (null != keyAliasText) {
                keyAliasText.setText(keyAlias);
            }
        }

        return view;
    }

}