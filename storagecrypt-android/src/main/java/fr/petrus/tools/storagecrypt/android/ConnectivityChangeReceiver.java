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

package fr.petrus.tools.storagecrypt.android;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;

/**
 * This {@code BroadcastReceiver} triggers the {@link DocumentsSyncTask} when the network connection
 * is established.
 *
 * @author Pierre Sagne
 * @since 22.03.2015
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityChangeRcv";

    private AppContext appContext = null;
    private Network network = null;

    /**
     * Creates a new {@code ConnectivityChangeReceiver} instance.
     */
    public ConnectivityChangeReceiver() {
        super();
        appContext = Application.getInstance().getAppContext();
        network = appContext.getNetwork();
    }

    /**
     * Enables the {@code ConnectivityChangeReceiver}.
     *
     * @param context the Android context
     */
    public static void enable(Context context) {
        ComponentName receiver = new ComponentName(context, ConnectivityChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * Disables the {@code ConnectivityChangeReceiver}.
     *
     * @param context the Android context
     */
    public static void disable(Context context) {
        ComponentName receiver = new ComponentName(context, ConnectivityChangeReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (network.isConnected()) {
            try {
                appContext.getTask(DocumentsSyncTask.class).start();
            } catch (TaskCreationException e) {
                Log.e(TAG, "Failed to get task " + e.getTaskClass().getCanonicalName(), e);
            }
        }
    }
}
