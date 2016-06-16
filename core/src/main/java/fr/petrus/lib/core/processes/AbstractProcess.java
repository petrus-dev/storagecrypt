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

package fr.petrus.lib.core.processes;

import fr.petrus.lib.core.processes.results.ProcessResults;

/**
 * This abstract implementation of the {@code Process} class implements the common basic behaviour
 * for all the real implementations.
 *
 * <p>Real implementations should subclass this class
 *
 * @author Pierre Sagne
 * @since 13.09.2015.
 */
public abstract class AbstractProcess<R extends ProcessResults> implements Process<R> {

    private volatile boolean paused;
    private volatile boolean canceled;
    private R results;

    protected AbstractProcess(R results) {
        paused = false;
        canceled = false;
        this.results = results;
    }

    public void start() {
        canceled = false;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void pauseIfNeeded() {
        while (!canceled && paused) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                cancel();
            }
        }
    }

    @Override
    public R getResults() {
        return results;
    }
}
