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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * A utility class to simply apply a GridData to a SWT {@link Control}.
 * <p/>
 * <p>This class is heavily inspired by Moritz Post solution described here :
 * <a href="http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/">http://eclipsesource.com/blogs/2013/07/25/efficiently-dealing-with-swt-gridlayout-and-griddata/</a>
 *
 * @author Pierre Sagne
 * @since 28.05.2016
 */
public class GridDataUtil {

    private final GridData gridData;

    private GridDataUtil( GridData gridData ) {
        this.gridData = gridData;
    }

    /**
     * Applies a {@code GridData} to the given {@code control} and returns a new
     * {@code GridDataUtil} to configure this new data.
     *
     * @param control the control to apply the {@code GridData} to
     * @return the new {@code GridDataUtil} which will be used to congigure the {@code GridData}
     */
    public static GridDataUtil applyGridData( Control control ) {
        GridData gridData = new GridData();
        control.setLayoutData( gridData );
        return new GridDataUtil( gridData );
    }

    /**
     * Tries to get an existing {@code GridData} from the given {@code control} and returns a
     * new {@code GridDataUtil} to configure this data.
     *
     * @param control the control to get the {@code GridData} from
     * @return the new {@code GridDataUtil} which will be used to configure the {@code GridData}
     * @throws IllegalStateException if the given {@code control} does not have a {@code GridData}
     *                               attached
     */
    public static GridDataUtil onGridData( Control control ) {
        Object layoutData = control.getLayoutData();
        if( layoutData instanceof GridData ) {
            return new GridDataUtil( ( GridData )layoutData );
        }
        throw new IllegalStateException( "Control must have GridData layout data. Has " + layoutData );
    }

    /**
     * Configures the grid data to fill the available horizontal space.
     *
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil withHorizontalFill() {
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        return this;
    }

    /**
     * Configures the grid data to fill the available vertical space.
     *
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil withVerticalFill() {
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessVerticalSpace = true;
        return this;
    }

    /**
     * Configures the grid data to fill the available horizontal and vertical space.
     *
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil withFill() {
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        return this;
    }

    /**
     * Configures the grid data to collapse to the minimum size and be centered.
     *
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil withCenterCollapse() {
        gridData.horizontalAlignment = SWT.CENTER;
        gridData.verticalAlignment = SWT.CENTER;
        gridData.grabExcessHorizontalSpace = false;
        gridData.grabExcessVerticalSpace = false;
        return this;
    }

    /**
     * Sets whether the width of the cell changes depending on the size of the parent Composite.
     *
     * @see GridData#grabExcessHorizontalSpace
     *
     * @param grabExcessHorizontalSpace if true, the width of the cell will change depending on the
     *                                  size of the parent Composite
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil grabExcessHorizontalSpace( boolean grabExcessHorizontalSpace ) {
        gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
        return this;
    }

    /**
     * Sets whether the height of the cell changes depending on the size of the parent Composite.
     *
     * @see GridData#grabExcessVerticalSpace
     *
     * @param grabExcessVerticalSpace if true, the height of the cell will change depending on the
     *                                size of the parent Composite
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil grabExcessVerticalSpace( boolean grabExcessVerticalSpace ) {
        gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
        return this;
    }

    /**
     * Sets the number of column cells that the control will take up.
     *
     * @see GridData#horizontalSpan
     *
     * @param horizontalSpan the number of column cells that the control will take up
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil horizontalSpan( int horizontalSpan ) {
        gridData.horizontalSpan = horizontalSpan;
        return this;
    }

    /**
     * Sets the number of row cells that the control will take up.
     *
     * @see GridData#verticalSpan
     *
     * @param verticalSpan the number of row cells that the control will take up
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil verticalSpan( int verticalSpan ) {
        gridData.verticalSpan = verticalSpan;
        return this;
    }

    /**
     * Sets the minimum height in pixels.
     *
     * @see GridData#minimumHeight
     *
     * @param minimumHeight the minimum height in pixels
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil minimumHeight( int minimumHeight ) {
        gridData.minimumHeight = minimumHeight;
        return this;
    }

    /**
     * Sets the minimum width in pixels.
     *
     * @see GridData#minimumWidth
     *
     * @param minimumWidth the minimum width in pixels
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil minimumWidth( int minimumWidth ) {
        gridData.minimumWidth = minimumWidth;
        return this;
    }

    /**
     * Sets the number of pixels of indentation that will be placed along the top side of the cell.
     *
     * @see GridData#verticalIndent
     *
     * @param verticalIndent the number of pixels of indentation that will be placed along the top
     *                       side of the cell
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil verticalIndent( int verticalIndent ) {
        gridData.verticalIndent = verticalIndent;
        return this;
    }

    /**
     * Sets the number of pixels of indentation that will be placed along the left side of the cell.
     *
     * @see GridData#horizontalIndent
     *
     * @param horizontalIndent the number of pixels of indentation that will be placed along the
     *                         left side of the cell
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil horizontalIndent( int horizontalIndent ) {
        gridData.horizontalIndent = horizontalIndent;
        return this;
    }

    /**
     * Sets the preferred height in pixels.
     *
     * @see GridData#heightHint
     *
     * @param heightHint the preferred height in pixels
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil heightHint( int heightHint ) {
        gridData.heightHint = heightHint;
        return this;
    }

    /**
     * Sets the preferred width in pixels.
     *
     * @see GridData#widthHint
     *
     * @param widthHint the preferred width in pixels
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil widthHint( int widthHint ) {
        gridData.widthHint = widthHint;
        return this;
    }

    /**
     * Specifies how controls will be positioned vertically within a cell.
     *
     * @see GridData#verticalAlignment
     *
     * @param verticalAlignment specifies how controls will be positioned vertically within a cell
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil verticalAlignment( int verticalAlignment ) {
        gridData.verticalAlignment = verticalAlignment;
        return this;
    }

    /**
     * Specifies how controls will be positioned horizontally within a cell.
     *
     * @see GridData#horizontalAlignment
     *
     * @param horizontalAlignment specifies how controls will be positioned horizontally within a cell
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil horizontalAlignment( int horizontalAlignment ) {
        gridData.horizontalAlignment = horizontalAlignment;
        return this;
    }

    /**
     * Informs the layout to ignore this control when sizing and positioning controls.
     *
     * @see GridData#exclude
     *
     * @param exclude if true, the layout will ignore this control when sizing and positioning
     *                controls
     * @return this {@code GridDataUtil} for further configuration
     */
    public GridDataUtil exclude( boolean exclude ) {
        gridData.exclude = exclude;
        return this;
    }
}
