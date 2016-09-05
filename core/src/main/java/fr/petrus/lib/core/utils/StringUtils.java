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

package fr.petrus.lib.core.utils;

/**
 * A utility class which provides some useful methods for processing Strings.
 *
 * @author Pierre Sagne
 * @since 15.02.2015
 */
public class StringUtils {

    /**
     * Splits the given {@code str} in two, at the first location of the given {@code separator}.
     *
     * @param string       the string to split
     * @param separator the separator
     * @return null if the given {@code str} does not contain the given {@code separator}, or an array of 2 strings, the first one being the part before the separator, the second one, the part after the separator.
     */
    public static String[] splitAtFirstSeparator(String string, String separator) {
        int separatorPos = string.indexOf(separator);
        if (separatorPos<0) {
            return null;
        }
        String[] result = new String[2];
        result[0] = string.substring(0, separatorPos);
        result[1] = string.substring(separatorPos + separator.length());
        return result;
    }

    /**
     * Splits the given {@code str} in two, at the last location of the given {@code separator}.
     *
     * @param string       the string to split
     * @param separator the separator
     * @return null if the given {@code str} does not contain the given {@code separator}, or an array of 2 strings, the first one being the part before the separator, the second one, the part after the separator.
     */
    public static String[] splitAtLastSeparator(String string, String separator) {
        int separatorPos = string.lastIndexOf(separator);
        if (separatorPos<0) {
            return null;
        }
        String[] result = new String[2];
        result[0] = string.substring(0, separatorPos);
        result[1] = string.substring(separatorPos + separator.length());
        return result;
    }


    /**
     * Returns whether the given {@code str} contains at least twice the given {@code separator}.
     *
     * @param string       the string to check
     * @param separator the separator to search
     * @return true if the given {@code str} contains at least twice the given {@code separator}
     */
    public static boolean hasDoubleSeparator(String string, String separator) {
        int firstSeparatorIndex = string.indexOf(separator);
        int lastSeparatorIndex =  string.lastIndexOf(separator);
        return -1 != firstSeparatorIndex && -1 != lastSeparatorIndex && firstSeparatorIndex != lastSeparatorIndex;
    }

    /**
     * Filters the given {@code string}, returning another String which contains only the {@code allowedCharacters}.
     *
     * @param string            the string
     * @param allowedCharacters the allowed characters
     * @return the filtered string
     */
    public String filter(String string, String allowedCharacters) {
        if (null==string) {
            return null;
        }
        if (null==allowedCharacters) {
            return string;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<string.length(); i++) {
            char c = string.charAt(i);
            if (allowedCharacters.contains(Character.toString(c))) {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Removes the starting "/" character from the given {@code path}, if it starts with a "/".
     *
     * @param path the path
     * @return the path without the starting "/" character
     */
    public static String removeStartingSlash(String path) {
        if (null!=path) {
            if (path.startsWith("/")) {
                return path.substring(1);
            }
        }
        return path;
    }

    /**
     * Removes the trailing "/" character from the given {@code path}, if it ends with a "/".
     *
     * @param path the path
     * @return the path without the trailing "/" character
     */
    public static String removeTrailingSlash(String path) {
        if (null!=path) {
            if (path.endsWith("/")) {
                return path.substring(0, path.length()-1);
            }
        }
        return path;
    }

    /**
     * Removes the starting and trailing "/" characters from the given {@code path}, if it starts
     * or ends with a "/".
     *
     * @param path the path
     * @return the path without the starting and trailing "/" characters
     */
    public static String trimSlashes(String path) {
        String result = path;
        if (null!=result) {
            if (result.startsWith("/")) {
                result = result.substring(1);
            }
            if (result.endsWith("/")) {
                return result.substring(0, result.length()-1);
            }
        }
        return result;
    }

    /**
     * Add {@code numSpaces} space characters before and after the given {code text}.
     *
     * @param text      the text to surround with spaces
     * @param numSpaces the number of spaces to add before and after the text
     * @return the text surrounded by spaces
     */
    public static String surroundWithSpaces(String text, int numSpaces) {
        if (null==text) {
            return null;
        }
        if (numSpaces<=0) {
            return text;
        }
        StringBuilder stringBuilder = new StringBuilder();
        appendNumSpaces(stringBuilder, numSpaces);
        stringBuilder.append(text);
        appendNumSpaces(stringBuilder, numSpaces);
        return stringBuilder.toString();
    }

    private static void appendNumSpaces(StringBuilder stringBuilder, int numSpaces) {
        for (int i=0; i<numSpaces; i++) {
            stringBuilder.append(' ');
        }
    }
}
