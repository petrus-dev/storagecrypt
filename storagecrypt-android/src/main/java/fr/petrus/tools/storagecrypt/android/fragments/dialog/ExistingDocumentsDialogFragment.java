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

import fr.petrus.lib.core.filesystem.tree.IndentedPathNode;
import fr.petrus.lib.core.filesystem.tree.PathNode;
import fr.petrus.lib.core.filesystem.tree.PathTree;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.ShowDialogListener;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedItem;
import fr.petrus.tools.storagecrypt.android.adapters.SelectedPathNodeArrayAdapter;

/**
 * This dialog lets the user select which existing documents to overwrite.
 *
 * @author Pierre Sagne
 * @since 21.09.2016
 */
public class ExistingDocumentsDialogFragment extends CustomDialogFragment<ExistingDocumentsDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "ExistingDocumentsDialogFragment";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private int dialogId = -1;
        private List<String> existingDocuments;
        private List<String> allDocuments;

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
        public Parameters setExistingDocuments(List<String> existingDocuments) {
            this.existingDocuments = existingDocuments;
            return this;
        }

        /**
         * Sets the list of all documents.
         *
         * @param allDocuments the list of all documents
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setAllDocuments(List<String> allDocuments) {
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
        public List<String> getExistingDocuments() {
            return existingDocuments;
        }

        /**
         * Returns the list of all documents.
         *
         * @return the list of all documents
         */
        public List<String> getAllDocuments() {
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
         * @param documents the selected documents
         */
        void onSelectExistingDocuments(int dialogId, List<String> documents);
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

    private final List<String> existingDocuments = new ArrayList<>();
    private final List<String> allDocuments = new ArrayList<>();
    private final List<SelectedItem<IndentedPathNode>> documentsSelectionNodes = new ArrayList<>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_documents_exist, null);

        if (null!=parameters) {
            existingDocuments.addAll(parameters.getExistingDocuments());
            allDocuments.addAll(parameters.getAllDocuments());
            final PathTree existingDocumentsTree = PathTree.buildTree(parameters.getExistingDocuments());
            for (IndentedPathNode existingDocumentNode : existingDocumentsTree.toIndentedPathNodeList()) {
                documentsSelectionNodes.add(new SelectedItem<>(existingDocumentNode, false));
            }
        }

        documentsListView = (ListView) view.findViewById(R.id.existing_documents_list_view);
        documentsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (null!=view) {
                    CheckBox checkBox = (CheckBox) view.findViewById(R.id.item_checkbox);
                    boolean checked = !checkBox.isChecked();
                    checkBox.setChecked(checked);
                    documentsSelectionNodes.get(position).setSelected(checked);
                }
            }
        });
        updateList();

        registerForContextMenu(documentsListView);

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.documents_exist_fragment_title));

        dialogBuilder.setPositiveButton(getString(R.string.documents_exist_fragment_ok_button_text), new AlertDialog.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final List<SelectedItem<IndentedPathNode>> selectedItemNodes =
                        SelectedPathNodeArrayAdapter.getListItems(documentsListView);
                final List<String> selectedPaths =
                        IndentedPathNode.getFilePaths(SelectedItem.selectedToList(selectedItemNodes));

                final PathTree documentsTree = PathTree.buildTree(allDocuments);
                final PathTree existingDocumentsTree = PathTree.buildTree(existingDocuments);
                final PathTree nonExistingDocumentsTree = documentsTree.subtract(existingDocumentsTree);
                final PathTree selectedDocumentsTree = PathTree.buildTree(selectedPaths);
                final PathTree documentsToEncryptTree = nonExistingDocumentsTree.merge(selectedDocumentsTree);

                dialogListener.onSelectExistingDocuments(parameters.getDialogId(), documentsToEncryptTree.toStringList());
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel_button_text), null);

        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    private void updateList() {
        Adapter adapter = documentsListView.getAdapter();
        if (null==adapter) {
            documentsListView.setAdapter(new SelectedPathNodeArrayAdapter(getActivity(), documentsSelectionNodes));
        } else {
            SelectedPathNodeArrayAdapter selectedPathNodeArrayAdapter = (SelectedPathNodeArrayAdapter) adapter;
            selectedPathNodeArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId()==R.id.existing_documents_list_view) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_context_existing_documents, menu);

            SelectedItem<IndentedPathNode> target = getContextMenuTarget(menuInfo);

            if (null==target || !target.getObject().getPathNode().isDirectory()) {
                menu.removeItem(R.id.existing_documents_select_children);
                menu.removeItem(R.id.existing_documents_deselect_children);
            }

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
        SelectedItem<IndentedPathNode> target = getContextMenuTarget(item.getMenuInfo());
        switch(item.getItemId()) {
            case R.id.existing_documents_select_all:
                for (SelectedItem<IndentedPathNode> selectedItem : documentsSelectionNodes) {
                    selectedItem.setSelected(true);
                }
                updateList();
                return true;
            case R.id.existing_documents_deselect_all:
                for (SelectedItem<IndentedPathNode> selectedItem : documentsSelectionNodes) {
                    selectedItem.setSelected(false);
                }
                updateList();
                return true;
            case R.id.existing_documents_select_children:
                recursivelySetChildrenSelected(target, true);
                target.setSelected(true);
                recursivelySelectParents(target);
                updateList();
                return true;
            case R.id.existing_documents_deselect_children:
                recursivelySetChildrenSelected(target, false);
                updateList();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private SelectedItem<IndentedPathNode> getContextMenuTarget(ContextMenu.ContextMenuInfo menuInfo) {
        if (null!=menuInfo && menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            return SelectedPathNodeArrayAdapter.getListItemAt(documentsListView, info.position);
        } else {
            return null;
        }
    }

    private void recursivelySetChildrenSelected(SelectedItem<IndentedPathNode> item, boolean selected) {
        PathNode itemPathNode = item.getObject().getPathNode();
        for (SelectedItem<IndentedPathNode> documentSelectionNode : documentsSelectionNodes) {
            PathNode documentSelectionPathNode = documentSelectionNode.getObject().getPathNode();
            if (itemPathNode.equals(documentSelectionPathNode.getParent())) {
                documentSelectionNode.setSelected(selected);
                recursivelySetChildrenSelected(documentSelectionNode, selected);
            }
        }
    }

    private void recursivelySelectParents(SelectedItem<IndentedPathNode> item) {
        PathNode itemPathNode = item.getObject().getPathNode();
        for (SelectedItem<IndentedPathNode> documentSelectionNode : documentsSelectionNodes) {
            PathNode documentSelectionPathNode = documentSelectionNode.getObject().getPathNode();
            if (documentSelectionPathNode.equals(itemPathNode.getParent())) {
                documentSelectionNode.setSelected(true);
                recursivelySelectParents(documentSelectionNode);
                return;
            }
        }
    }

    /**
     * Creates a {@code ExistingDocumentsDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code ExistingDocumentsDialogFragment} to
     * @param parameters      the parameters to create the {@code ExistingDocumentsDialogFragment}
     * @return the newly created {@code ExistingDocumentsDialogFragment}
     */
    public static ExistingDocumentsDialogFragment showFragment(FragmentManager fragmentManager, Parameters parameters) {
        ExistingDocumentsDialogFragment fragment = new ExistingDocumentsDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}