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

package fr.petrus.lib.core;

/**
 * This class holds progress information, including the current progress and the maximum progress.
 * <p/>
 * <p>If the maximum progress is not known, and not specified, it is "indeterminate".
 *
 * @author Pierre Sagne
 * @since 20.09.2015
 */
public class Progress {
    private int progress;
    private int max;

    /**
     * Creates a new {@code Progress} instance, with the same values of the given {@code progress} instance.
     *
     * @param progress the {@code Progress} instance to copy the values from
     */
    public Progress(Progress progress) {
        set(progress);
    }

    /**
     * Creates a new {@code Progress} instance, providing a starting {@code progress} value, and a
     * {@code max} value.
     *
     * @param progress the starting progress value
     * @param max      the maximum progress value
     */
    public Progress(int progress, int max) {
        this.progress = progress;
        this.max = max;
    }

    /**
     * Creates a new {@code Progress} instance.
     *
     * @param indeterminate if set to true, this progress instance is indeterminate
     */
    public Progress(boolean indeterminate) {
        this(0, indeterminate?-1:0);
    }

    /**
     * Copies the values of the given {@code Process}.
     *
     * @param progress the {@code Process} to copy the values from
     */
    public void set(Progress progress) {
        this.progress = progress.progress;
        this.max = progress.max;
    }

    /**
     * Sets the current progress value.
     *
     * @param progress the current progress value
     */
    public void setProgress(int progress) {
        this.progress = progress;
    }

    /**
     * Sets the maximum progress value.
     *
     * @param max the maximum progress value
     */
    public void setMax(int max) {
        this.max = max;
    }

    /**
     * Sets this {@code Progress} as indeterminate.
     */
    public void setIndeterminate() {
        max = -1;
    }

    /**
     * Gets the current progress value.
     *
     * @return the current progress value
     */
    public int getProgress() {
        return progress;
    }

    /**
     * Gets the maximum progress value.
     *
     * @return the maximum progress value
     */
    public int getMax() {
        return max;
    }

    /**
     * Returns whether this {@code Progress} is indeterminate.
     *
     * @return true if this {@code Progress} is indeterminate
     */
    public boolean isIndeterminate() {
        return max < 0;
    }
}
