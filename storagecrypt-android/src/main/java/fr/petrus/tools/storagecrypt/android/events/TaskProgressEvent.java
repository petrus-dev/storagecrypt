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

package fr.petrus.tools.storagecrypt.android.events;

import org.greenrobot.eventbus.EventBus;

import fr.petrus.lib.core.Progress;

/**
 * The {@link EventBus} event cast to send progress updates to a {@code ProgressDialog}.
 *
 * @author Pierre Sagne
 * @since 04.02.2015
 */
public class TaskProgressEvent extends Event {
    private int dialogId;
    private Progress[] progresses;

    /**
     * Creates a new {@code TaskProgressEvent} instance.
     *
     * @param dialogId the ID of the {@code ProgressDialog} to update
     * @param numProgresses the number of {@code Progress channels}
     */
    public TaskProgressEvent(int dialogId, int numProgresses) {
        this.dialogId = dialogId;
        progresses = new Progress[numProgresses];
        for (int i = 0; i<numProgresses; i++) {
            progresses[i] = new Progress(true);
        }
    }

    /**
     * Returns the ID of the {@code ProgressDialog} to update.
     * @return the ID of the {@code ProgressDialog} to update
     */
    public int getDialogId() {
        return dialogId;
    }

    /**
     * Sets the message to display in the {@code i}th channel of the {@code ProgressDialog}.
     *
     * @param i       the channel to set the message for
     * @param message the message to display in the {@code i}th channel of the {@code ProgressDialog}
     * @return this {@code TaskProgressEvent} for further configuration
     */
    public TaskProgressEvent setMessage(int i, String message) {
        if (i>=0 && i<progresses.length) {
            this.progresses[i].setMessage(message);
        }
        return this;
    }

    /**
     * Sets the current progress to display in the {@code i}th channel of the {@code ProgressDialog}.
     *
     * @param i        the channel to set the current progress for
     * @param progress the current progress to display in the {@code i}th channel of the {@code ProgressDialog}
     * @return this {@code TaskProgressEvent} for further configuration
     */
    public TaskProgressEvent setProgress(int i, int progress) {
        if (i>=0 && i<progresses.length) {
            this.progresses[i].setProgress(progress);
        }
        return this;
    }

    /**
     * Sets the maximum progress to display in the {@code i}th channel of the {@code ProgressDialog}.
     *
     * @param i   the channel to set the maximum progress for
     * @param max the maximum progress to display in the {@code i}th channel of the {@code ProgressDialog}
     * @return this {@code TaskProgressEvent} for further configuration
     */
    public TaskProgressEvent setMax(int i, int max) {
        if (i>=0 && i<progresses.length) {
            this.progresses[i].setMax(max);
        }
        return this;
    }

    /**
     * Returns the number of progress channels.
     *
     * @return the number of progress channels
     */
    public int getNumProgresses() {
        return progresses.length;
    }

    /**
     * Returns the progress to display in the {@code i}th channel of the {@code ProgressDialog}.
     *
     * @param i   the progress channel
     * @return the progress to display in the {@code i}th channel of the {@code ProgressDialog}
     */
    public Progress getProgress(int i) {
        if (i>=0 && i<progresses.length) {
            return progresses[i];
        } else {
            return null;
        }
    }
}
