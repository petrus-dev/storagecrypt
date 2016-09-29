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

package fr.petrus.lib.core.filesystem.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A structure representing a tree of filesystem path nodes.
 *
 * <p>This tree can have multiple roots.
 *
 * @author Pierre Sagne
 * @since 28.09.2016
 */

public class PathTree {

    private final Map<String, PathNode> roots = new LinkedHashMap<>();

    /**
     * Creates a new tree by analyzing the paths in the given {@code documents} array.
     *
     * @see #buildTree(List)
     *
     * @param documents an array containing the paths used to build the tree
     * @return the tree representing the structure of the given {@code documents}
     */
    public static PathTree buildTree(String[] documents) {
        return buildTree(Arrays.asList(documents));
    }

    /**
     * Creates a new tree by analyzing the paths in the given {@code documents} list.
     *
     * <p>The upper level folders become the root nodes.
     *
     * @param documents a list containing the paths used to build the tree
     * @return the tree representing the structure of the given {@code documents}
     */
    public static PathTree buildTree(List<String> documents) {
        final Map<String, PathNode> paths = new LinkedHashMap<>();
        for (String document : documents) {
            if (null!=document) {
                paths.put(document, new PathNode(document));
            }
        }
        final PathTree pathTree = new PathTree();
        for (PathNode pathNode : paths.values()) {
            if (!paths.containsKey(pathNode.getParentPath())) {
                pathTree.addRoot(pathNode);
            } else {
                PathNode parent = paths.get(pathNode.getParentPath());
                parent.addChild(pathNode);
            }
        }
        return pathTree;
    }

    /**
     * Returns the root nodes.
     *
     * @return the root nodes
     */
    public List<PathNode> getRoots() {
        return new ArrayList<>(roots.values());
    }

    private void addRoot(PathNode root) {
        roots.put(root.getFilePath(), root);
    }

    private void removeRoot(PathNode root) {
        roots.remove(root.getFilePath());
    }

    /**
     * Returns the paths of the documents in the tree, as a list.
     *
     * @return the paths of the documents in the tree, as a list
     */
    public List<String> toStringList() {
        final List<String> stringList = new ArrayList<>();
        for (PathNode root : roots.values()) {
            root.addToStringList(stringList);
        }
        return stringList;
    }

    /**
     * Returns a list of {@code IndentedPathNode}s representing the documents in the tree, to help
     * representing the tree structure nicely as a list.
     *
     * @return a list of {@code IndentedPathNode}s representing the documents in the tree
     */
    public List<IndentedPathNode> toIndentedPathNodeList() {
        final List<IndentedPathNode> nodeList = new ArrayList<>();
        for (PathNode root : roots.values()) {
            root.addToIndentedPathNodeList(nodeList, 0);
        }
        return nodeList;
    }

    /**
     * Makes a copy of this {@code PathTree}.
     *
     * <p>All the included nodes are also cloned, so modifying the cloned tree will not modify the original one.
     *
     * @return a copy of this {@code PathTree}
     */
    public PathTree clone() {
        final PathTree clonedTree = new PathTree();
        for (PathNode root : roots.values()) {
            clonedTree.addRoot(root.clone());
        }
        return clonedTree;
    }

    /**
     * Merges this tree with the given {@code tree2} tree, and returns the merged tree.
     *
     * <p>A clone is made before merging, so this tree is not modified.
     *
     * @param tree2 the tree to merge with this one
     * @return the result of the merge operation
     */
    public PathTree merge(final PathTree tree2) {
        final PathTree result = clone();
        for (PathNode root2 : tree2.roots.values()) {
            final PathNode root = result.roots.get(root2.getFilePath());
            if (null == root) {
                result.addRoot(root2.clone());
            } else {
                root.merge(root2);
            }
        }
        return result;
    }

    /**
     * Subtracts the given {@code tree2} tree nodes from this one, and returns the resulting tree.
     *
     * <p>A clone is made before subtracting, so this tree is not modified.
     *
     * <p>When subtracting a branch, the resulting branch will be removed if it has no children after
     * children subtraction.
     *
     * @param tree2 the tree to subtract from with this one
     * @return the result of the subtract operation
     */
    public PathTree subtract(PathTree tree2) {
        final PathTree result = clone();
        for (PathNode root2 : tree2.roots.values()) {
            final PathNode root = result.roots.get(root2.getFilePath());
            if (null != root) {
                if (root.isDirectory()) {
                    root.subtract(root2);
                    if (!root.hasChildren()) {
                        result.removeRoot(root);
                    }
                } else {
                    result.removeRoot(root);
                }
            }
        }
        return result;
    }
}
