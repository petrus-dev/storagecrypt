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

package fr.petrus.tools.storagecrypt.desktop.swt;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;

/**
 * A utility class to simply apply a GridLayout to a SWT {@link Composite}.
 * <p/>
 * <p>This class is heavily inspired by Moritz Post solution described here :
 * <a href="http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/">http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/</a>
 *
 * @author Pierre Sagne
 * @since 28.05.2016
 */
public class GridLayoutUtil {

    private final GridLayout gridLayout;

    private GridLayoutUtil( GridLayout gridLayout ) {
        this.gridLayout = gridLayout;
    }

    /**
     * Applies a {@code GridLayout} to the given {@code composite} and returns a new
     * {@code GridLayoutUtil} to configure this new layout.
     *
     * @param composite the composite to apply the {@code GridLayout} to
     * @return the new {@code GridLayoutUtil} which will be used to congigure the {@code GridLayout}
     */
    public static GridLayoutUtil applyGridLayout( Composite composite ) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        composite.setLayout( gridLayout );
        return new GridLayoutUtil( gridLayout );
    }

    /**
     * Tries to get an existing {@code GridLayout} from the given {@code composite} and returns a
     * new {@code GridLayoutUtil} to configure this layout.
     *
     * @param composite the composite to get the {@code GridLayout} from
     * @return the new {@code GridLayoutUtil} which will be used to configure the {@code GridLayout}
     * @throws IllegalStateException if the given {@code composite} does not have a {@code GridLayout}
     *                               attached
     */
    public static GridLayoutUtil onGridLayout( Composite composite ) {
        Layout layout = composite.getLayout();
        if( layout instanceof GridLayout ) {
            return new GridLayoutUtil( ( GridLayout )layout );
        }
        throw new IllegalStateException( "Composite has to have a GridLayout. Has " + layout );
    }

    /**
     * Sets the number of cell columns in the layout.
     *
     * @see GridLayout#numColumns
     *
     * @param numColumns the number of cell columns in the layout
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil numColumns( int numColumns ) {
        gridLayout.numColumns = numColumns;
        return this;
    }

    /**
     * Sets whether all columns in the layout will be forced to have the same width.
     *
     * @see GridLayout#makeColumnsEqualWidth
     *
     * @param columnsEqualWidth if set to true, all columns in the layout will be forced to have
     *                          the same width
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil columnsEqualWidth( boolean columnsEqualWidth ) {
        gridLayout.makeColumnsEqualWidth = columnsEqualWidth;
        return this;
    }

    /**
     * Sets the number of pixels between the right edge of one cell and the left edge of its
     * neighbouring cell to the right.
     *
     * @see GridLayout#horizontalSpacing
     *
     * @param horizontalSpacing the number of pixels between the right edge of one cell and the left
     *                          edge of its neighbouring cell to the right
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil horizontalSpacing( int horizontalSpacing ) {
        gridLayout.horizontalSpacing = horizontalSpacing;
        return this;
    }

    /**
     * Sets the number of pixels between the bottom edge of one cell and the top edge of its
     * neighbouring cell underneath.
     *
     * @see GridLayout#verticalSpacing
     *
     * @param verticalSpacing the number of pixels between the bottom edge of one cell and the top
     *                        edge of its neighbouring cell underneath
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil verticalSpacing( int verticalSpacing ) {
        gridLayout.verticalSpacing = verticalSpacing;
        return this;
    }

    /**
     * Sets the number of pixels of horizontal margin that will be placed along the left and right
     * edges of the layout.
     *
     * @param marginWidth the number of pixels of horizontal margin that will be placed along the
     *                    left and right edges of the layout
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil marginWidth( int marginWidth ) {
        gridLayout.marginWidth = marginWidth;
        return this;
    }

    /**
     * Sets the number of pixels of vertical margin that will be placed along the top and bottom
     * edges of the layout.
     *
     * @see GridLayout#marginHeight
     *
     * @param marginHeight the number of pixels of vertical margin that will be placed along the top
     *                     and bottom edges of the layout
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil marginHeight( int marginHeight ) {
        gridLayout.marginHeight = marginHeight;
        return this;
    }

    /**
     * Sets the number of pixels of vertical margin that will be placed along the top edge of the
     * layout.
     *
     * @see GridLayout#marginTop
     *
     * @param marginTop the number of pixels of vertical margin that will be placed along the top
     *                  edge of the layout
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil marginTop( int marginTop ) {
        gridLayout.marginTop = marginTop;
        return this;
    }

    /**
     * Sets the number of pixels of vertical margin that will be placed along the bottom edge of the
     * layout.
     *
     * @see GridLayout#marginBottom
     *
     * @param marginBottom the number of pixels of vertical margin that will be placed along the
     *                     bottom edge of the layout
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil marginBottom( int marginBottom ) {
        gridLayout.marginBottom = marginBottom;
        return this;
    }

    /**
     * Sets the number of pixels of horizontal margin that will be placed along the left edge of the
     * layout.
     *
     * @see GridLayout#marginLeft
     *
     * @param marginLeft the number of pixels of horizontal margin that will be placed along the
     *                   left edge of the layout
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil marginLeft( int marginLeft ) {
        gridLayout.marginLeft = marginLeft;
        return this;
    }

    /**
     * Sets the number of pixels of horizontal margin that will be placed along the right edge of
     * the layout.
     *
     * @see GridLayout#marginRight
     *
     * @param marginRight the number of pixels of horizontal margin that will be placed along the
     *                    right edge of the layout
     * @return this {@code GridLayoutUtil} for further configuration
     */
    public GridLayoutUtil marginRight( int marginRight ) {
        gridLayout.marginRight = marginRight;
        return this;
    }
}
