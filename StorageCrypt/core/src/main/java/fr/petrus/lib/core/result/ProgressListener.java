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

package fr.petrus.lib.core.result;

/**
 * A listener interface for reporting the progress of of an operation
 *
 * <p>Every method of this interface has an index parameter representing a layer.
 *
 * <p>The first index (0) usually represents the highest level layer (the whole process layer)
 *
 * <p>For example for a process which uploads 4 files the layer 0 progress could go from 1/4 to 4/4,
 * layer 1 representing the progress of each of the files.
 *
 * @author Pierre Sagne
 * @since 15.03.2015
 */
public interface ProgressListener {
    /**
     * Implement this method if you want to receive a message about the progress of the {@code i}th
     * layer of the reporting the progress.
     *
     * @param i       the index of the layer of the process for which the message is reported
     * @param message the message reported for the given layer
     */
    void onMessage(int i, String message);

    /**
     * Implement this method if you want to receive the progress of the task.
     *
     * @param i        the index of the layer of the process for which the progress is reported
     * @param progress the progress reported for the given layer
     */
    void onProgress(int i, int progress);

    /**
     * Implement this method if you want to receive the maximum value of the progress of the task.
     *
     * @param i   the index of the layer of the process for which the maximum value of the progress
     *            is reported
     * @param max the maximum value of the progress reported for the given layer
     */
    void onSetMax(int i, int max);
}
