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

package fr.petrus.tools.storagecrypt.android.tasks;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.tasks.Task;
import fr.petrus.tools.storagecrypt.android.services.ThreadService;

/**
 * The abstract implementation of the {@code Task} interface for the Android platform.
 *
 * <p>The real implementations should extend this abstract class.
 *
 * <p>This class simply manages an android {@link ThreadService} which does the real work
 *
 * @param <S> the type of the {@code ThreadService} managed by this {@code ServiceTask}
 *
 * @author Pierre Sagne
 * @since 13.09.2015
 */
public abstract class ServiceTask<S extends ThreadService<S>> implements Task {
    /**
     * The {@code AppContext} which provides the dependencies.
     */
    protected AppContext appContext = null;

    /**
     * The Android context.
     */
    protected Context context = null;

    /**
     * The class of the {@code Service} managed by this {@code ServiceTask} instance.
     */
    protected Class<S> serviceClass = null;

    /**
     * The {@code Service} managed by this {@code ServiceTask} instance.
     */
    protected S service = null;

    /* Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        @SuppressWarnings("unchecked")
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ThreadService<S>.ServiceBinder binder = (ThreadService<S>.ServiceBinder) serviceBinder;
            service = binder.getService();
            service.setBoundTask(ServiceTask.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    /**
     * Creates a new {@code ServiceTask} instance.
     *
     * @param appContext   the {@code AppContext} which provides the dependencies
     * @param context      the Android context
     * @param serviceClass the class of the {@code Service} to manage
     */
    public ServiceTask(AppContext appContext, Context context, Class<S> serviceClass) {
        this.appContext = appContext;
        this.context = context.getApplicationContext();
        this.serviceClass = serviceClass;
    }

    /**
     * Starts the service, with the given {@code command} and {@code parameters}.
     *
     * @param command    the command to pass to the managed service
     * @param parameters the parameters to pass to the managed service
     */
    public void start(int command, Bundle parameters) {
        Intent startIntent = new Intent(context, serviceClass);
        startIntent.putExtra(ThreadService.COMMAND, command);
        if (null!=parameters) {
            startIntent.putExtras(parameters);
        }
        context.startService(startIntent);
        bind();
    }

    /**
     * Starts the service, with the given {@code command}.
     *
     * @param command    the command to pass to the managed service
     */
    public void start(int command) {
        start(command, null);
    }

    /**
     * Starts the service.
     */
    public void start() {
        start(ThreadService.COMMAND_START);
    }

    /**
     * Stops the service.
     */
    public void stop() {
        Intent stopIntent = new Intent(context, serviceClass);
        context.stopService(stopIntent);
    }

    /**
     * Returns whether the managed service is canceled.
     *
     * @return true if the managed service is canceled
     */
    public boolean isCanceled() {
        if (null!=service) {
            synchronized (service) {
                if (null != service) {
                    return service.isCanceled();
                }
            }
        }
        return true;
    }

    @Override
    public boolean isPaused() {
        if (null!=service) {
            synchronized (service) {
                if (null != service) {
                    return service.isPaused();
                }
            }
        }
        return false;
    }

    @Override
    public void pause() {
        if (null!=service) {
            synchronized (service) {
                if (null != service) {
                    service.pause();
                }
            }
        }
    }

    @Override
    public void resume() {
        if (null!=service) {
            synchronized (service) {
                if (null != service) {
                    service.resume();
                }
            }
        }
    }

    @Override
    public void cancel() {
        if (null!=service) {
            synchronized (service) {
                if (null != service) {
                    service.cancel();
                }
            }
        }
    }

    /**
     * Binds the managed service to this task.
     */
    public void bind() {
        Intent intent = new Intent(context, serviceClass);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbinds the managed service from this task.
     */
    public void unBind() {
        if (null!=service) {
            synchronized (service) {
                context.unbindService(serviceConnection);
                service = null;
            }
        }
    }

    /**
     * Returns whether the service managed by this task is running.
     *
     * @return true if the service managed by this task is running
     */
    public boolean isRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
