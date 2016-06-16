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

package fr.petrus.tools.storagecrypt.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.processes.Process;
import fr.petrus.lib.core.tasks.Task;
import fr.petrus.tools.storagecrypt.android.Application;
import fr.petrus.tools.storagecrypt.android.tasks.ServiceTask;

/**
 * An Android {@code Service} which runs a {@code Process} in a background thread.
 *
 * @param <S> the type of the real implementation of the service
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public abstract class ThreadService<S extends ThreadService<S>>
        extends Service implements Task {
    /**
     * The argument used to pass a command.
     */
    public static final String COMMAND = "command";

    /**
     * The {@code COMMAND} value to simply start the service.
     */
    public static final int COMMAND_START   = 0;

    /**
     * The {@code COMMAND} value to pause the service.
     */
    public static final int COMMAND_PAUSE   = 1;

    /**
     * The {@code COMMAND} value to resume the service.
     */
    public static final int COMMAND_RESUME  = 2;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    @SuppressWarnings("unchecked")
    public class ServiceBinder extends Binder {
        /**
         * Return this instance of the service so clients can call public methods
         *
         * @return this instance of the service so clients can call public methods
         */
        public S getService() {
            // Return this instance of the service so clients can call public methods
            return (S)ThreadService.this;
        }
    }

    private final IBinder binder = new ServiceBinder();

    /**
     * The Service name.
     */
    protected String serviceName = "ThreadService";

    /**
     * The {@code AppContext} which provides the dependencies.
     */
    protected AppContext appContext = null;

    /**
     * The Task which this service is bound to and managed by.
     */
    protected ServiceTask<S> boundTask = null;

    private int dialogId;
    private Process process = null;

    /**
     * Creates a new {@code ThreadService} instance.
     *
     * @param serviceName the name of the real service implementation
     * @param dialogId    the ID of the dialog this service reports its progress to
     */
    public ThreadService(String serviceName, int dialogId) {
        this.serviceName = serviceName;
        this.dialogId = dialogId;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Sets the task which this service is bound to.
     *
     * @param boundTask the task which this service is bound to
     */
    public void setBoundTask(ServiceTask<S> boundTask) {
        this.boundTask = boundTask;
    }

    /**
     * Initializes this service dependencies from the {@code Application}.
     */
    public void initDependencies() {
        this.appContext = Application.getInstance().getAppContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(serviceName, "onCreate");
        initDependencies();
    }

    @Override
    public void onDestroy() {
        Log.i(serviceName, "onDestroy");
        cancel();
        super.onDestroy();
    }

    /**
     * Sets the process which will do the real work.
     *
     * @param process the process which will do the real work
     */
    protected void setProcess(Process process) {
        this.process = process;
        if (null==process) {
            if (null!=boundTask) {
                boundTask.unBind();
            }
        }
    }

    /**
     * Starts the service with the given {@code command} and {@code parameters}.
     *
     * @param command    the command to pass to the service
     * @param parameters the parameters to pass to the service
     */
    protected abstract void runIntent(int command, Bundle parameters);

    /**
     * If the service is already started, sends the given {@code command} and {@code parameters}
     * to the running instance.
     *
     * @param command    the command to pass to the service
     * @param parameters the parameters to pass to the service
     */
    protected void refreshIntent(int command, Bundle parameters) {}

    /**
     * Returns whether the service is running.
     *
     * @return true if the service is running
     */
    protected boolean isRunning() {
        return null!=process;
    }

    /**
     * Returns the ID of the dialog this service reports its progress to.
     *
     * @return the ID of the dialog this service reports its progress to
     */
    protected int getDialogId() {
        return dialogId;
    }

    /**
     * Returns the process which will do the real work.
     *
     * @return the process which will do the real work
     */
    protected Process getProcess() {
        return process;
    }

    /**
     * Returns whether there is a registered running process.
     *
     * @return true if there is a registered running process
     */
    protected boolean hasProcess() {
        return null != process;
    }

    /**
     * Returns whether the registered process has been canceled.
     *
     * @return true if the registered process has been canceled
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

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        int command = COMMAND_START;
        Bundle parameters = null;
        if (null!=intent && null!=intent.getExtras()) {
            parameters = intent.getExtras();
            command = parameters.getInt(COMMAND, COMMAND_START);
        }

        switch(command) {
            case COMMAND_PAUSE:
                if (null!=process) {
                    process.pause();
                }
                break;
            case COMMAND_RESUME:
                if (null!=process) {
                    process.resume();
                }
                break;
            default:
                if (null==process) {
                    final int runCommand = command;
                    final Bundle runParameters = parameters;
                    new Thread() {
                        @Override
                        public void run() {
                            runIntent(runCommand, runParameters);
                            stopSelf();
                        }
                    }.start();
                } else {
                    refreshIntent(command, parameters);
                }
        }
        return START_NOT_STICKY;
    }
}
