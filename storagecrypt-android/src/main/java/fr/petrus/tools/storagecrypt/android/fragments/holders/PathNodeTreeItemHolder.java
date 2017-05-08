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

package fr.petrus.tools.storagecrypt.android.fragments.holders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.unnamed.b.atv.model.TreeNode;

import java.util.List;

import fr.petrus.lib.core.filesystem.tree.PathNode;
import fr.petrus.tools.storagecrypt.R;

/**
 * This class creates the view for a TreeNode representing a {@code PathNode}.
 *
 * @author Pierre Sagne
 * @since 05.05.2017
 */
public class PathNodeTreeItemHolder extends AbstractTreeItemHolder<PathNode> {
    public PathNodeTreeItemHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, PathNode pathNode) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.tree_view_item, null, false);
        final ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        final TextView textView = (TextView) view.findViewById(R.id.node_value);

        if (pathNode.isDirectory()) {
            iconView.setImageResource(R.drawable.ic_folder_black_36dp);
        } else {
            iconView.setImageResource(R.drawable.ic_file_black_36dp);
        }
        textView.setText(pathNode.getFileName());

        final CheckBox nodeSelector = (CheckBox) view.findViewById(R.id.node_selector);
        nodeSelector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setSelected(isChecked);
            }
        });
        nodeSelector.setChecked(node.isSelected());
        setNodeSelector(nodeSelector);

        final ImageView arrowView = (ImageView) view.findViewById(R.id.arrow_icon);
        if (node.isLeaf()) {
            arrowView.setVisibility(View.INVISIBLE);
        }
        setArrowView(arrowView);

        return view;
    }

    /**
     * Select or deselect this node.
     *
     * If selecting, all the parents will be selected too.
     * If deselecting, all children nodes will be deselected too, as all of their children, and so on.
     *
     * @param isChecked true to select this node, false to deselect it
     */
    public void setSelected(boolean isChecked) {
        getTreeView().selectNode(mNode, isChecked);
        if (isChecked) {
            recursivelySelectParents(mNode);
        } else {
            recursivelyDeselectChildren(mNode);
        }
    }

    private void recursivelySelectParents(TreeNode node) {
        TreeNode parentTreeNode = node.getParent();
        if (null!=parentTreeNode && !parentTreeNode.isRoot()) {
            getTreeView().selectNode(parentTreeNode, true);
            recursivelySelectParents(parentTreeNode);
        }
    }

    private void recursivelyDeselectChildren(TreeNode node) {
        List<TreeNode> childrenTreeNodes = node.getChildren();
        for (TreeNode child: childrenTreeNodes) {
            getTreeView().selectNode(child, false);
            recursivelyDeselectChildren(child);
        }
    }
}
