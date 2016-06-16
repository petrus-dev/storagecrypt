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
import android.widget.BaseAdapter;
import android.widget.TextView;

import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.ProcessResults;
import fr.petrus.tools.storagecrypt.R;

/**
 * The {@code BaseAdapter} for task results.
 *
 * @author Pierre Sagne
 * @since 16.01.2015
 */
public class ResultAdapter extends BaseAdapter {

    private Context context;
    private ProcessResults results;
    private BaseProcessResults.ResultsType resultsType;

    /**
     * Creates a new {@code ResultAdapter} instance.
     *
     * @param context     the Android context
     * @param results     the results of the task to display
     * @param resultsType the type of results to display
     */
    public ResultAdapter(Context context, ProcessResults results, BaseProcessResults.ResultsType resultsType) {
        super();
        this.context = context;
        this.results = results;
        this.resultsType = resultsType;
    }

    @Override
    public int getCount() {
        if (null!=results && null!=resultsType) {
            return results.getResultsCount(resultsType);
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (null!=results && null!=resultsType) {
            return results.getResultColumns(resultsType, i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if(view==null) {
            // inflate the layout
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.row_result, parent, false);
        }

        TextView textViewLeft = (TextView) view.findViewById(R.id.left_text);
        TextView textViewRight = (TextView) view.findViewById(R.id.right_text);

        if (null!=results && null!=resultsType) {
            String[] columns;
            switch (results.getResultsColumnsCount(resultsType)) {
                case 1:
                    columns = results.getResultColumns(resultsType, position);
                    textViewLeft.setText(columns[0]);
                    textViewLeft.setVisibility(View.VISIBLE);
                    textViewRight.setVisibility(View.GONE);
                    break;
                case 2:
                    columns = results.getResultColumns(resultsType, position);
                    textViewLeft.setText(columns[0]);
                    textViewRight.setText(columns[1]);
                    textViewLeft.setVisibility(View.VISIBLE);
                    textViewRight.setVisibility(View.VISIBLE);
                    break;
                default:
                    textViewLeft.setVisibility(View.GONE);
                    textViewRight.setVisibility(View.GONE);
            }
        } else {
            textViewLeft.setVisibility(View.GONE);
            textViewRight.setVisibility(View.GONE);
        }
        return view;
    }
}
