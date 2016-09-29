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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A structure representing a node of a {@link PathTree}.
 *
 * @author Pierre Sagne
 * @since 23.09.2016
 */

public class PathNode {
    private String filePath;
    private String fileName;
    private String parentPath;
    private Map<String, PathNode> children;
    private PathNode parent;

    /**
     * Creates a new {@code PathNode} representing the document at the given {@code filePath}.
     *
     * @param filePath the path of the document represented by this {@code PathNode}
     */
    public PathNode(String filePath) {
        this.filePath = filePath;
        File file = new File(filePath);
        parentPath = file.getParent();
        fileName = file.getName();
        if (file.isDirectory()) {
            children = new LinkedHashMap<>();
        } else {
            children = null;
        }
        parent = null;
    }

    /**
     * Returns whether this {@code PathNode} represents a directory.
     *
     * @return true if this {@code PathNode} represents a directory
     */
    public boolean isDirectory() {
        return null != children;
    }

    /**
     * Returns the path of the parent of the document represented by this {@code PathNode}.
     *
     * @return the path of the parent of the document represented by this {@code PathNode}
     */
    public String getParentPath() {
        return parentPath;
    }

    /**
     * Returns the path of the document represented by this {@code PathNode}.
     *
     * @return the path of the document represented by this {@code PathNode}
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns the name of the document represented by this {@code PathNode}.
     *
     * @return the name of the document represented by this {@code PathNode}
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns a list of {@code PathNode}s representing the children of this {@code PathNode}.
     *
     * @return a list of {@code PathNode}s representing the children of this {@code PathNode}
     */
    public List<PathNode> getChildren() {
        if (isDirectory()) {
            return new ArrayList<>(children.values());
        }
        return null;
    }

    /**
     * Returns whether this {@code PathNode} has children.
     *
     * @return true if this {@code PathNode} has children
     */
    public boolean hasChildren() {
        return null!=children && !children.isEmpty();
    }

    /**
     * Returns the parent of this {@code PathNode}.
     *
     * @return the parent of this {@code PathNode}, or null if this {@code PathNode} is a root
     */
    public PathNode getParent() {
        return parent;
    }

    /**
     * Adds a child to this {@code PathNode}, if it is a directory.
     *
     * @param child the child to add to this {@code PathNode}
     */
    public void addChild(PathNode child) {
        if (null!=children) {
            children.put(child.getFilePath(), child);
            child.parent = this;
        }
    }

    /**
     * Removes a child from this {@code PathNode}, if it is a directory.
     *
     * @param child the child to remove from this {@code PathNode}
     */
    public void removeChild(PathNode child) {
        children.remove(child.getFilePath());
    }

    /**
     * Recursively clones this {@code PathNode} and all its children.
     *
     * @return the cloned {@code PathNode}
     */
    public PathNode clone() {
        final PathNode clonedPathNode = new PathNode(filePath);
        if (isDirectory()) {
            for (PathNode child : children.values()) {
                clonedPathNode.addChild(child.clone());
            }
        }
        return clonedPathNode;
    }

    /**
     * Merges this node with the given {@code other} tree.
     *
     * @param other the node to merge with this one
     */
    public void merge(PathNode other) {
        if (isDirectory() && other.isDirectory()) {
            for (PathNode otherChild : other.children.values()) {
                PathNode child = children.get(otherChild.getFilePath());
                if (null == child) {
                    addChild(otherChild.clone());
                } else {
                    child.merge(otherChild);
                }
            }
        }
    }

    /**
     * Subtracts the given {@code other} node from this one.
     *
     * <p>When subtracting a branch, the resulting branch will be removed if it has no children after
     * children subtraction.
     *
     * @param other the node to subtract from with this one
     */
    public void subtract(PathNode other) {
        if (isDirectory() && other.isDirectory()) {
            for (PathNode otherChild : other.children.values()) {
                PathNode child = children.get(otherChild.getFilePath());
                if (null != child) {
                    if (child.isDirectory()) {
                        child.subtract(otherChild);
                        if (!child.hasChildren()) {
                            removeChild(child);
                        }
                    } else {
                        removeChild(child);
                    }
                }
            }
        }
    }

    /**
     * Adds the path of the document represented by this node to the given {@code stringList} and
     * do the same for all children
     *
     * @param stringList the string list where the paths are added
     */
    public void addToStringList(List<String> stringList) {
        stringList.add(filePath);
        if (isDirectory()) {
            for (PathNode child : children.values()) {
                child.addToStringList(stringList);
            }
        }
    }

    /**
     * Adds an {@code IndentedPathNode} representing this {@code PathNode} to the given
     * {@code nodeList}, at the given indentation {@code level} and do the same for all children
     *
     * @param nodeList the string list where the nodes are added
     * @param level the current indentation level
     */
    public void addToIndentedPathNodeList(List<IndentedPathNode> nodeList, int level) {
        nodeList.add(new IndentedPathNode(level, this));
        if (isDirectory()) {
            for (PathNode child : children.values()) {
                child.addToIndentedPathNodeList(nodeList, level + 1);
            }
        }
    }
}
