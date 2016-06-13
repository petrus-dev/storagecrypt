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

package fr.petrus.tools.storagecrypt.desktop.windows.components;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

/**
 * An {@code EditingSupport} which filters the characters when editing a table cell.
 *
 * @author Pierre Sagne
 * @since 30.04.2016
 */
public abstract class TableColumnTextEditingSupport extends EditingSupport {
    protected final TableViewer viewer;
    private final CellEditor editor;

    /**
     * Creates a new {@code TableColumnTextEditingSupport} instance for the given {@code viewer}
     *
     * @param viewer the {@code TableViewer} this {@code TableColumnTextEditingSupport} is
     *               applied to
     */
    public TableColumnTextEditingSupport(TableViewer viewer) {
        super(viewer);
        this.viewer = viewer;
        editor = new TextCellEditor(viewer.getTable());
    }

    /**
     * Creates a new {@code TableColumnTextEditingSupport} instance for the given {@code viewer}
     *
     * @param viewer            the {@code TableViewer} this {@code TableColumnTextEditingSupport}
     *                          is applied to
     * @param allowedCharacters a {@code String} containing the only allowed characters
     */
    public TableColumnTextEditingSupport(TableViewer viewer, final String allowedCharacters) {
        this(viewer);
        editor.setValidator(new ICellEditorValidator() {
            @Override
            public String isValid(Object value) {
                String text = (String) value;
                for (int i = 0; i<text.length(); i++) {
                    if (!allowedCharacters.contains(String.valueOf(text.charAt(i)))) {
                        return "!";
                    }
                }
                return null;
            }
        });
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
        return editor;
    }
}
