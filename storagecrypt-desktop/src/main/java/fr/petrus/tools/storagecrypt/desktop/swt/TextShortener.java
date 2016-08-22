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
                drawFlags = SWT.DRAW_DELIMITER | SWT.TRANSPARENT;
                break;
            default:
                textLayout = new TextLayout(display);
                drawFlags = SWT.TRANSPARENT;
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
        if (availableWidth < textWidth) {
            int w = gc.textExtent(ELLIPSIS, drawFlags).x;
            if (availableWidth<=w) return text;
            int l = text.length();
            int max = l/2;
            int min = 0;
            int mid = (max+min)/2 - 1;
            if (mid <= 0) return text;
            textLayout.setText(text);
            mid = validateOffset(textLayout, mid);
            while (min < mid && mid < max) {
                String s1 = text.substring(0, mid);
                String s2 = text.substring(validateOffset(textLayout, l-mid), l);
                int l1 = gc.textExtent(s1, drawFlags).x;
                int l2 = gc.textExtent(s2, drawFlags).x;
                if (l1+w+l2 > availableWidth) {
                    max = mid;
                    mid = validateOffset(textLayout, (max+min)/2);
                } else if (l1+w+l2 < availableWidth) {
                    min = mid;
                    mid = validateOffset(textLayout, (max+min)/2);
                } else {
                    min = max;
                }
            }
            String result = mid == 0 ? text : text.substring(0, mid) + ELLIPSIS
                    + text.substring(validateOffset(textLayout, l-mid), l);
            return result;
        } else {
            return text;
        }
    }

    private static int validateOffset(TextLayout layout, int offset) {
        int nextOffset = layout.getNextOffset(offset, SWT.MOVEMENT_CLUSTER);
        if (nextOffset != offset) return layout.getPreviousOffset(nextOffset, SWT.MOVEMENT_CLUSTER);
        return offset;
    }
}
