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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;

/**
 * Shortens text to be drawn in a limited space.
 *
 * @author Pierre Sagne
 * @since 22.08.2016
 */
public class TextShortener {

    /**
     * The shortening mode (the method used to shorten the text)
     */
    public enum Mode {SPLIT, ELLIPSIZE}

    private static final String ELLIPSIS = "...";
    //private static final String ELLIPSIS = "\u2026";

    private Mode mode;
    private TextLayout textLayout;
    private int drawFlags = SWT.NULL;

    /**
     * Creates a new {@code TextShortener} instance.
     *
     * @param display the SWT Display on which the text will be rendered
     * @param mode the shortening mode (SPLIT the text in 2 halfs, or ELIPSIZE it)
     */
    public TextShortener(Display display, Mode mode) {
        this.mode = mode;
        switch (mode) {
            case SPLIT:
                textLayout = null;
                drawFlags = SWT.DRAW_DELIMITER | SWT.DRAW_TRANSPARENT;
                break;
            default:
                textLayout = new TextLayout(display);
                drawFlags = SWT.DRAW_TRANSPARENT;
        }
    }

    /**
     * Returns the SWT draw flags, according to the shortening mode.
     *
     * @return the SWT draw flags, according to the shortening mode
     */
    public int getDrawFlags() {
        return drawFlags;
    }

    /**
     * Disposes the elements which need to be disposed
     */
    public void dispose() {
        if (null!=textLayout) {
            textLayout.dispose();
        }
    }

    /**
     * Shortens the given {@code text} if its rendering size exceeds the given {@code availableWidth},
     * with the appropriate method.
     *
     * @param gc the GC used to render the text
     * @param text the text to shorten
     * @param availableWidth the width available to display the shortened text
     * @return the text, shortened if needed, with the appropriate method
     */
    public String shortenText(GC gc, String text, int availableWidth) {
        switch (mode) {
            case SPLIT:
                return splitIn2IfNeeded(gc, text, availableWidth);
            default:
                return ellipsizeIfNeeded(gc, text, availableWidth);
        }
    }

    /**
     * Shortens the given {@code text} if its rendering size exceeds the given {@code availableWidth},
     * by cutting it in two parts and adding a new line character between the two halfs.
     *
     * @param gc the GC used to render the text
     * @param text the text to shorten
     * @param availableWidth the width available to display the shortened text
     * @return the text, shortened if needed, by cutting it in two parts and adding a new line
     *         character between the two halfs
     */
    private String splitIn2IfNeeded(GC gc, String text, int availableWidth) {
        int textWidth = gc.textExtent(text, SWT.DRAW_DELIMITER).x;
        if (availableWidth < textWidth) {
            return new StringBuilder(text).insert(text.length()/2, " \n ").toString();
        } else {
            return text;
        }
    }

    /**
     * Shortens the given {@code text} if its rendering size exceeds the given {@code availableWidth},
     * by removing the middle characters.
     *
     * <p>This method is heavily inspired by the SWT CLabel.shortenText code.
     *
     * @param gc the GC used to render the text
     * @param text the text to shorten
     * @param availableWidth the width available to display the shortened text
     * @return the text, shortened if needed, by remobing the middle characters
     */
    private String ellipsizeIfNeeded(GC gc, String text, int availableWidth) {
        int textWidth = gc.textExtent(text, drawFlags).x;
        if (availableWidth <= textWidth) return text;

        int textLength = text.length();
        int max = textLength/2;
        int min = 0;
        int mid = (max+min)/2 - 1;

        if (mid <= 0) return text;

        setTextForOffsetValidation(text);

        mid = validateOffset(mid);

        while (min < mid && mid < max) {
            String stringBeforeEllipsis = text.substring(0, mid);
            String stringAfterEllipsis =
                    text.substring(validateOffset(textLength - mid), textLength);
            int stringBeforeEllipsisWidth = gc.textExtent(stringBeforeEllipsis, drawFlags).x;
            int stringAfterEllipsisWidth = gc.textExtent(stringAfterEllipsis, drawFlags).x;
            if (stringBeforeEllipsisWidth + textWidth + stringAfterEllipsisWidth > availableWidth) {
                max = mid;
                mid = validateOffset((max + min) / 2);
            } else if (stringBeforeEllipsisWidth + textWidth + stringAfterEllipsisWidth < availableWidth) {
                min = mid;
                mid = validateOffset((max + min) / 2);
            } else {
                min = max;
            }
        }
        String result;
        if (0==mid) {
            result = text;
        } else {
            result = text.substring(0, mid) + ELLIPSIS
                    + text.substring(validateOffset(textLength - mid), textLength);
        }
        return result;
    }

    private void setTextForOffsetValidation(String text) {
        textLayout.setText(text);
    }

    private int validateOffset(int offset) {
        int nextOffset = textLayout.getNextOffset(offset, SWT.MOVEMENT_CLUSTER);
        if (nextOffset != offset) return textLayout.getPreviousOffset(nextOffset, SWT.MOVEMENT_CLUSTER);
        return offset;
    }
}
