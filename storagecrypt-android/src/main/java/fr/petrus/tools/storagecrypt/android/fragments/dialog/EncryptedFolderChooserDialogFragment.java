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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.ArrayList;
import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.ShowDialogListener;
import fr.petrus.tools.storagecrypt.android.fragments.holders.EncryptedDocumentTreeItemHolder;

/**
 * This dialog lets the user select an encrypted folder.
 *
 * @author Pierre Sagne
 * @since 28.04.2017
 */
public class EncryptedFolderChooserDialogFragment extends CustomDialogFragment<EncryptedFolderChooserDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "EncryptedFolderChooserDialogFragment";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private int dialogId = -1;
        private List<EncryptedDocument> roots = null;
        private EncryptedDocument expandedFolderOnStart = null;

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
         * Sets the list of documents roots.
         *
         * @param roots the list of documents roots
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setRoots(List<EncryptedDocument> roots) {
            this.roots = roots;
            return this;
        }

        /**
         * Sets the folder which will be expanded on start.
         *
         * @param expandedFolderOnStart the folder which will be expanded on start
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setExpandedFolderOnStart(EncryptedDocument expandedFolderOnStart) {
            this.expandedFolderOnStart = expandedFolderOnStart;
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
        public List<EncryptedDocument> getRoots() {
            return roots;
        }

        /**
         * Returns the folder which will be expanded on start.
         *
         * @return the folder which will be expanded on start
         */
        public EncryptedDocument getExpandedFolderOnStart() {
            return expandedFolderOnStart;
        }
    }

    private DialogListener dialogListener;

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener extends ShowDialogListener {

        /**
         * Handles encrypted folder selection.
         *
         * @param selectedFolder the selected folder
         */
        void onSelectEncryptedFolder(int dialogId, EncryptedDocument selectedFolder);
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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_choose_destination_folder, null);

        final List<EncryptedDocument> roots = new ArrayList<>();

        EncryptedDocument expandedFolderOnStart = null;
        if (null!=parameters) {
            roots.addAll(parameters.getRoots());
            expandedFolderOnStart = parameters.getExpandedFolderOnStart();
        }

        final TreeNode treeRoot = TreeNode.root();
        try {
            for (EncryptedDocument root : roots) {
                recursivelyBuildFolderNodes(root, treeRoot, expandedFolderOnStart);
            }
        } catch (DatabaseConnectionClosedException e) {
            Log.e(TAG.substring(0, 24), "Database is closed", e);
        }

        final RelativeLayout treeViewContainer = (RelativeLayout) view.findViewById(R.id.tree_view_container);

        final AndroidTreeView treeView = new AndroidTreeView(getActivity(), treeRoot);
        //treeView.setDefaultAnimation(true);
        treeView.setDefaultViewHolder(EncryptedDocumentTreeItemHolder.class);
        treeView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        treeView.setSelectionModeEnabled(true);
        //treeView.setUseAutoToggle(false);
        treeView.setUse2dScroll(true);

        treeViewContainer.addView(treeView.getView());

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.choose_destination_folder_fragment_title));

        dialogBuilder.setPositiveButton(getString(R.string.choose_destination_folder_ok_button_text), new AlertDialog.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                final List<TreeNode> selectedNodes = treeView.getSelected();
                if (selectedNodes.size()==1) {
                    final Object selectedNodeValue = selectedNodes.get(0).getValue();
                    if (null != selectedNodeValue && selectedNodeValue instanceof EncryptedDocument) {
                        dialogListener.onSelectEncryptedFolder(parameters.getDialogId(),
                                (EncryptedDocument) selectedNodeValue);
                    }
                }
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.choose_destination_folder_cancel_button_text), null);

        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    private void recursivelyBuildFolderNodes(EncryptedDocument folder, TreeNode parentNode,
                                             EncryptedDocument expandedFolderOnStart)
            throws DatabaseConnectionClosedException {
        TreeNode treeNode = new TreeNode(folder);
        treeNode.setSelectable(true);
        parentNode.addChild(treeNode);
        if (folder.hasInTree(expandedFolderOnStart)) {
            treeNode.setExpanded(true);
        }
        for (EncryptedDocument child: folder.children(true)) {
            if (child.isRoot() || child.isFolder()) {
                recursivelyBuildFolderNodes(child, treeNode, expandedFolderOnStart);
            }
        }
    }

    /**
     * Creates a {@code EncryptedFolderChooserDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code EncryptedFolderChooserDialogFragment} to
     * @param parameters      the parameters to create the {@code EncryptedFolderChooserDialogFragment}
     * @return the newly created {@code EncryptedFolderChooserDialogFragment}
     */
    public static EncryptedFolderChooserDialogFragment showFragment(FragmentManager fragmentManager, Parameters parameters) {
        EncryptedFolderChooserDialogFragment fragment = new EncryptedFolderChooserDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}