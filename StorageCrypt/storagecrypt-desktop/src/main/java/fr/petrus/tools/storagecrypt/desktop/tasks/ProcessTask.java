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

package fr.petrus.tools.storagecrypt.desktop.tasks;

import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.processes.Process;
import fr.petrus.lib.core.tasks.Task;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

/**
 * The abstract implementation of the {@code Task} interface for the "Desktop" platform.
 *
 * <p>The real implementations should extend this abstract class.
 *
 * @author Pierre Sagne
 * @since 13.09.2015
 */
public abstract class ProcessTask implements Task {
    /**
     * The application main window.
     */
    protected AppWindow appWindow = null;

    /**
     * The {@code AppContext} class holds all the needed dependencies for most of the other classes.
     */
    protected AppContext appContext = null;

    /**
     * The {@code TextBundle} class is used to access the I18n texts of the "Desktop" platform.
     */
    protected TextBundle textBundle = null;

    /**
     * The {@code TextI18n} instance which will be used to translate the size and dates into text.
     */
    protected TextI18n textI18n = null;

    /**
     * The process which will run the task
     */
    private Process process = null;

    /**
     * Creates a new {@code ProcessTask} instance.
     *
     * @param appWindow the dependencies needed by the {@code ProcessTask} to do its work
     */
    public ProcessTask(AppWindow appWindow) {
        this.appWindow = appWindow;
        this.appContext = appWindow.getAppContext();
        this.textI18n = appContext.getTextI18n();
        this.textBundle = appWindow.getTextBundle();
    }

    /**
     * Sets the process which does the actual work.
     *
     * <p>This method should be called by the class inheriting {@code ProgressTask} when the
     * {@code Process} has been setup.
     *
     * <p>It should be called with a {@code null} parameter when the work is done
     *
     * @param process the process which has been created to do the work.
     */
    protected void setProcess(Process process) {
        this.process = process;
    }

    /**
     * Returns whether this task has a process attached.
     *
     * @return true if this task has a process attached
     */
    protected boolean hasProcess() {
        return null != process;
    }

    /**
     * Returns the process attached to this task.
     *
     * @return the process attached to this task, or null
     */
    protected Process getProcess() {
        return process;
    }

    /**
     * Returns whether the process attached to this task has been canceled.
     *
     * @return true if the process attached to this task has been canceled, or if no process is attached
     */
    public boolean isCanceled() {
        return null==process || process.isCanceled();
    }

    @Override
    public boolean isPaused() {
        return null!=process &&process.isPaused();
    }

    @Override
    public void pause() {
        if (null!=process) {
            process.pause();
        }
    }

    @Override
    public void resume() {
        if (null!=process) {
            process.resume();
        }
    }

    @Override
    public void cancel() {
        if (null!=process) {
            process.cancel();
        }
    }
}
