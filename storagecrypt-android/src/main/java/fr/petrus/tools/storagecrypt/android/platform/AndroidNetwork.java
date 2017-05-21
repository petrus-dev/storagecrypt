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

package fr.petrus.tools.storagecrypt.android.platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.network.Network;
import fr.petrus.tools.storagecrypt.R;

/**
 * The {@link Network} implementation for the Android platform.
 *
 * @author Pierre Sagne
 * @since 27.08.2015
 */
public class AndroidNetwork implements Network {
    private Context context;

    /**
     * Creates a new {@code AndroidNetwork} instance.
     *
     * @param context the Android context
     */
    AndroidNetwork(Context context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     * <p>This implementation simply calls {@link AndroidNetwork#isConnected}
     */
    @Override
    public boolean isConnected() {
        return isConnected(context);
    }

    /**
     * {@inheritDoc}
     * <p>This implementation checks whether the given {@code syncAction} requires wifi connectivity
     * and returns the network state for the appropriate connection type.
     */
    @Override
    public boolean isNetworkReadyForSyncAction(SyncAction syncAction) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean status = false;
        switch (syncAction) {
            case Upload:
                status = isNetworkInRequestedState(
                        prefs.getBoolean(
                                context.getString(R.string.pref_key_upload_wifi_only), true));
                break;
            case Download:
                status = isNetworkInRequestedState(
                        prefs.getBoolean(
                                context.getString(R.string.pref_key_download_wifi_only), true));
                break;
            case Deletion:
                status = isNetworkInRequestedState(
                        prefs.getBoolean(
                                context.getString(R.string.pref_key_deletion_wifi_only), true));
                break;
        }
        return status;
    }

    /**
     * Returns whether the network connectivity is active.
     *
     * @param wifiOnly if true, checks the state of the wifi connection, if false, check the network
     *                 connection
     * @return true if the network connectivity is active
     */
    private boolean isNetworkInRequestedState(boolean wifiOnly) {
        if (wifiOnly) {
            return isConnectedWithWifi(context);
        } else {
            return isConnected(context);
        }
    }

    /**
     * Returns whether the network connection is active.
     *
     * @param context the Android context
     * @return true if the network connection is active
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Returns whether the wifi connection is active.
     *
     * @param context the Android context
     * @return true if the wifi connection is active
     */
    public static boolean isConnectedWithWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        return isConnected && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * Returns whether the network connection is active or connecting.
     *
     * @param context the Android context
     * @return true if the network connection is active or connecting
     */
    public static boolean isConnectedOrConnecting(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Returns whether the wifi connection is active or connecting.
     *
     * @param context the Android context
     * @return true if the wifi connection is active or connecting
     */
    public static boolean isConnectedOrConnectingWithWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
