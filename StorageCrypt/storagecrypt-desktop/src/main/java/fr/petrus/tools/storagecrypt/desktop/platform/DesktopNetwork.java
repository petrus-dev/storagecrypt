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

package fr.petrus.tools.storagecrypt.desktop.platform;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.network.Network;
import fr.petrus.tools.storagecrypt.desktop.Settings;

/**
 * The {@link Network} implementation for the "Desktop" platform.
 *
 * @author Pierre Sagne
 * @since 27.08.2015
 */
public class DesktopNetwork implements Network {

    /**
     * Creates a new {@code DesktopNetwork} instance.
     */
    DesktopNetwork() {}

    /**
     * {@inheritDoc}
     * <p>This implementation simply checks that the network interface is up
     */
    @Override
    public boolean isConnected() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    return true;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>This implementation simply checks that the network interface is up
     */
    @Override
    public boolean isNetworkReadyForSyncAction(SyncAction syncAction) {
        return isConnected();
    }

    /**
     * Setups the proxy configuration according to the application settings.
     *
     * @param settings the application settings
     */
    public static void setupProxy(Settings settings) {
        switch (settings.getProxyConfiguration()) {
            case NoProxy:
                System.clearProperty("java.net.useSystemProxies");
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
                break;
            case UseSystemProxies:
                System.setProperty("java.net.useSystemProxies", "true");
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
                break;
            case ManualProxy:
                System.clearProperty("java.net.useSystemProxies");
                System.setProperty("http.proxyHost", settings.getProxyAddress());
                System.setProperty("http.proxyPort", String.valueOf(settings.getProxyPort()));
                System.setProperty("https.proxyHost", settings.getProxyAddress());
                System.setProperty("https.proxyPort", String.valueOf(settings.getProxyPort()));
                break;
        }
    }
}
