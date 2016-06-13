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

package fr.petrus.tools.storagecrypt.android.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.ProcessResults;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.adapters.ResultAdapter;

/**
 * This dialog displays the detailed list of one type of result of a task.
 *
 * @author Pierre Sagne
 * @since 11.04.2015
 */
public class ResultsListDialogFragment extends CustomDialogFragment<ResultsListDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    public static final String TAG = "ResultsListDialogFragment";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private String title = null;
        private ProcessResults results = null;
        private BaseProcessResults.ResultsType resultsType = null;

        /**
         * Creates a new empty {@code Parameters} instance.
         */
        public Parameters() {}

        /**
         * Sets the title of the dialog to create.
         *
         * @param title the title of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the results to display in the dialog.
         *
         * @param results the results to display in the dialog
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setResults(ProcessResults results) {
            this.results = results;
            return this;
        }

        /**
         * Sets the type of results to display in the dialog.
         *
         * @param resultsType the type of results to display in the dialog
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setResultsType(BaseProcessResults.ResultsType resultsType) {
            this.resultsType = resultsType;
            return this;
        }

        /**
         * Returns the title of the dialog to create.
         *
         * @return the title of the dialog to create
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the results to display in the dialog.
         *
         * @return the results to display in the dialog
         */
        public ProcessResults getResults() {
            return results;
        }

        /**
         * Returns the type of results to display in the dialog.
         *
         * @return the type of results to display in the dialog
         */
        public BaseProcessResults.ResultsType getResultsType() {
            return resultsType;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_results_list, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        if (null!=parameters) {
            if (null != parameters.getTitle()) {
                dialogBuilder.setTitle(parameters.getTitle());
            }

            if (null != parameters.getResultsType()) {
                TextView resultsListMessage = (TextView) view.findViewById(R.id.results_list_message);
                switch (parameters.getResultsType()) {
                    case Success:
                        resultsListMessage.setText(getString(R.string.results_dialog_success_message));
                        break;
                    case Skipped:
                        resultsListMessage.setText(getString(R.string.results_dialog_skipped_message));
                        break;
                    case Errors:
                        resultsListMessage.setText(getString(R.string.results_dialog_errors_message));
                        break;
                }
            }
            if (null != parameters.getResults()) {
                ListView resultsList = (ListView) view.findViewById(R.id.results_list_view);

                resultsList.setAdapter(new ResultAdapter(getActivity(),
                        parameters.getResults(), parameters.getResultsType()));
            }
        }
        dialogBuilder.setNeutralButton(getActivity().getString(R.string.results_dialog_back_button_text), null);
        dialogBuilder.setView(view);
        AlertDialog dialog = dialogBuilder.create();
        return dialog;
    }

    /**
     * Creates a {@code ResultsListDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code ResultsListDialogFragment} to
     * @param parameters      the parameters to create the {@code ResultsListDialogFragment}
     * @return the newly created {@code ResultsListDialogFragment}
     */
    public static ResultsListDialogFragment showFragment(FragmentManager fragmentManager, Parameters parameters) {
        ResultsListDialogFragment fragment = new ResultsListDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}