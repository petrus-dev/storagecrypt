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

import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.tools.storagecrypt.R;

/**
 * The {@code ArrayAdapter} which handles key alias selection and edition.
 *
 * @author Pierre Sagne
 * @since 15.06.2015
 */
public class SelectedKeyArrayAdapter extends ArrayAdapter<SelectedKey> {

    /**
     * Returns the {@code SelectedKey} at the given {@code position} of the given {@code listView}.
     *
     * @param listView the list where the key aliases are displayed
     * @param position the position of the key alias in the list
     * @return the {@code SelectedKey} at the given {@code position} of the given {@code listView}
     */
    public static SelectedKey getListItemAt(ListView listView, int position) {
        ListAdapter adapter = listView.getAdapter();
        if (null!=adapter && adapter instanceof SelectedKeyArrayAdapter) {
            return ((SelectedKeyArrayAdapter) adapter).getItem(position);
        }
        return null;
    }

    /**
     * Returns a list containing all the {@code SelectedKey}s displayed in the given {@code listView}.
     *
     * @param listView the list where the {@code SelectedKey}s are displayed
     * @return the list containing all the {@code SelectedKey}s displayed in the given {@code listView}
     */
    public static List<SelectedKey> getListItems(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (null!=adapter && adapter instanceof SelectedKeyArrayAdapter) {
            return ((SelectedKeyArrayAdapter) adapter).renamedKeys;
        }
        return null;
    }

    private Context context;
    private int layoutResourceId;
    private List<SelectedKey> renamedKeys;

    /**
     * Creates a new {@code SelectedKeyArrayAdapter} instance.
     *
     * @param context     the Android context
     * @param renamedKeys the list of {@code SelectedKey}s managed by this adapter
     */
    public SelectedKeyArrayAdapter(Context context, List<SelectedKey> renamedKeys) {
        this(context, R.layout.row_key_select_and_rename, renamedKeys);
    }

    /**
     * Creates a new {@code SelectedKeyArrayAdapter} instance.
     *
     * @param context          the Android context
     * @param layoutResourceId the resource id of the layout used to display a row of the list
     * @param renamedKeys      the list of {@code SelectedKey}s managed by this adapter
     */
    public SelectedKeyArrayAdapter(Context context, int layoutResourceId,
                                   List<SelectedKey> renamedKeys) {
        super(context, layoutResourceId, renamedKeys);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.renamedKeys = renamedKeys;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(layoutResourceId, null);
        }

        final SelectedKey renamedKey = renamedKeys.get(position);

        if (null != renamedKey) {
            final CheckBox keyAliasCheckBox = (CheckBox) view.findViewById(R.id.key_alias_checkbox);
            final EditText keyAliasEditText = (EditText) view.findViewById(R.id.key_alias_edit_text);
            if (null != keyAliasCheckBox) {
                keyAliasCheckBox.setText(renamedKey.getKeyAlias());
                keyAliasCheckBox.setChecked(renamedKey.isSelected());
                if (null!=keyAliasEditText) {
                    if (renamedKey.isSelected()) {
                        keyAliasEditText.setVisibility(View.VISIBLE);
                    } else {
                        keyAliasEditText.setVisibility(View.GONE);
                    }
                }
                keyAliasCheckBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (renamedKey.isSelected()) {
                            renamedKey.setSelected(false);
                            keyAliasCheckBox.setSelected(false);
                            if (null!=keyAliasEditText) {
                                keyAliasEditText.setVisibility(View.GONE);
                            }
                        } else {
                            renamedKey.setSelected(true);
                            keyAliasCheckBox.setSelected(true);
                            if (null!=keyAliasEditText) {
                                keyAliasEditText.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }

            if (null != keyAliasEditText) {
                if (null!=renamedKey.getNewKeyAlias()) {
                    keyAliasEditText.setText(renamedKey.getNewKeyAlias());
                } else {
                    keyAliasEditText.setText(renamedKey.getKeyAlias());
                }

                keyAliasEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        if (!keyAliasEditText.hasFocus()) {
                            renamedKey.setNewKeyAlias(keyAliasEditText.getText().toString());
                            Log.d("Renamed Key", "Renamed key : " + renamedKey.getNewKeyAlias());
                        }
                    }
                });

                InputFilter[] inputFilters = new InputFilter[1];
                inputFilters[0] = new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        if (source instanceof SpannableStringBuilder) {
                            SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
                            for (int i = end - 1; i >= start; i--) {
                                char currentChar = source.charAt(i);
                                if (!Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS
                                        .contains(String.valueOf(currentChar))) {
                                    sourceAsSpannableBuilder.delete(i, i+1);
                                }
                            }
                            return source;
                        } else {
                            StringBuilder filteredStringBuilder = new StringBuilder();
                            for (int i = start; i < end; i++) {
                                char currentChar = source.charAt(i);
                                if (Constants.CRYPTO.KEY_STORE_KEY_ALIAS_ALLOWED_CHARACTERS
                                        .contains(String.valueOf(currentChar))) {
                                    filteredStringBuilder.append(currentChar);
                                }
                            }
                            return filteredStringBuilder.toString();
                        }
                    }
                };
                keyAliasEditText.setFilters(inputFilters);
            }
        }

        return view;
    }
}