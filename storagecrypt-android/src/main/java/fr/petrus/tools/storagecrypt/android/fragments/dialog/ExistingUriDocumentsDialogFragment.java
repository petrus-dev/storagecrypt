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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.ShowDialogListener;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedItem;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedItemArrayAdapter;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedUriArrayAdapter;

/**
 * This dialog lets the user select which existing documents to overwrite.
 *
 * @author Pierre Sagne
 * @since 25.09.2016
 */
public class ExistingUriDocumentsDialogFragment extends CustomDialogFragment<ExistingUriDocumentsDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "ExistingUriDocumentsDialogFragment";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private int dialogId = -1;
        private List<Uri> existingDocuments;
        private List<Uri> allDocuments;

        /**
         * Creates a new empty {@code Parameters} instance.
         */
        public Parameters() {}

        /**
         * Sets the ID of the dialog to create.
         *
         * @param dialogId the ID of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setDialogId(int dialogId) {
            this.dialogId = dialogId;
            return this;
        }

        /**
         * Sets the list of existing documents to choose.
         *
         * @param existingDocuments the list of existing documents to choose
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setExistingDocuments(List<Uri> existingDocuments) {
            this.existingDocuments = existingDocuments;
            return this;
        }

        /**
         * Sets the list of all documents.
         *
         * @param allDocuments the list of all documents
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setAllDocuments(List<Uri> allDocuments) {
            this.allDocuments = allDocuments;
            return this;
        }

        /**
         * Returns the ID of the dialog to create.
         *
         * @return the ID of the dialog to create
         */
        public int getDialogId() {
            return dialogId;
        }

        /**
         * Returns the list of existing documents to choose.
         *
         * @return the list of existing documents to choose
         */
        public List<Uri> getExistingDocuments() {
            return existingDocuments;
        }

        /**
         * Returns the list of all documents.
         *
         * @return the list of all documents
         */
        public List<Uri> getAllDocuments() {
            return allDocuments;
        }
    }

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener extends ShowDialogListener {

        /**
         * Handles existing documents selection.
         *
         * @param uris the selected documents
         */
        void onSelectExistingDocumentUris(int dialogId, List<Uri> uris);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DialogListener) {
            dialogListener = (DialogListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement " + DialogListener.class.getName());
        }
    }

    @Override
    public void onDetach() {
        dialogListener = null;
        super.onDetach();
    }

    private ListView documentsListView = null;

    private final List<SelectedItem<Uri>> existingDocuments = new ArrayList<>();
    private final List<Uri> allDocuments = new ArrayList<>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_documents_exist, null);

        if (null!=parameters) {
            for (Uri existingDocumentUri : parameters.getExistingDocuments()) {
                existingDocuments.add(new SelectedItem<>(existingDocumentUri, false));
            }
            allDocuments.addAll(parameters.getAllDocuments());
        }

        documentsListView = (ListView) view.findViewById(R.id.existing_documents_list_view);
        documentsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (null!=view) {
                    CheckBox checkBox = (CheckBox) view.findViewById(R.id.item_checkbox);
                    boolean checked = !checkBox.isChecked();
                    checkBox.setChecked(checked);
                    existingDocuments.get(position).setSelected(checked);
                }
            }
        });
        updateList();

        registerForContextMenu(documentsListView);

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.documents_exist_fragment_title));

        dialogBuilder.setPositiveButton(getString(R.string.documents_exist_fragment_ok_button_text), new AlertDialog.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final Set<Uri> deselectedDocuments = SelectedItem.itemsToSet(false,
                        SelectedItemArrayAdapter.<Uri>getListItems(documentsListView));
                final List<Uri> result = new ArrayList<>();
                for (Uri document : allDocuments) {
                    if (!deselectedDocuments.contains(document)) {
                        result.add(document);
                    }
                }
                dialogListener.onSelectExistingDocumentUris(parameters.getDialogId(), result);
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel_button_text), null);

        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    private void updateList() {
        Adapter adapter = documentsListView.getAdapter();
        if (null==adapter) {
            documentsListView.setAdapter(new SelectedUriArrayAdapter(getActivity(), existingDocuments));
        } else {
            SelectedUriArrayAdapter selectedUriArrayAdapter = (SelectedUriArrayAdapter) adapter;
            selectedUriArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId()==R.id.existing_documents_list_view) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_context_existing_documents, menu);
            menu.removeItem(R.id.existing_documents_select_children);
            menu.removeItem(R.id.existing_documents_deselect_children);

            //Dirty workaround needed to make sure that onContextItemSelected() is called for a DialogFragment
            MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    onContextItemSelected(item);
                    return true;
                }
            };
            for (int i = 0, n = menu.size(); i < n; i++)
                menu.getItem(i).setOnMenuItemClickListener(listener);
            //End of the workaround
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.existing_documents_select_all:
                for (SelectedItem<Uri> selectedItem : existingDocuments) {
                    selectedItem.setSelected(true);
                }
                updateList();
                return true;
            case R.id.existing_documents_deselect_all:
                for (SelectedItem<Uri> selectedItem : existingDocuments) {
                    selectedItem.setSelected(false);
                }
                updateList();
                return true;
        }
        return super.onContextItemSelected(item);
    }


    /**
     * Creates a {@code ExistingDocumentsDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code ExistingDocumentsDialogFragment} to
     * @param parameters      the parameters to create the {@code ExistingDocumentsDialogFragment}
     * @return the newly created {@code ExistingDocumentsDialogFragment}
     */
    public static ExistingUriDocumentsDialogFragment showFragment(FragmentManager fragmentManager, Parameters parameters) {
        ExistingUriDocumentsDialogFragment fragment = new ExistingUriDocumentsDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}