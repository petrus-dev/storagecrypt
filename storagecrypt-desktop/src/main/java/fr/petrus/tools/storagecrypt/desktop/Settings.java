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

package fr.petrus.tools.storagecrypt.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import fr.petrus.lib.core.filesystem.FileSystem;

/**
 * This class loads and saves the application settings.
 *
 * @author Pierre Sagne
 * @since 27.10.2015
 */
public class Settings {
    private static Logger LOG = LoggerFactory.getLogger(Settings.class);

    /**
     * The enumeration for proxy configuration.
     */
    public enum ProxyConfiguration {

        /**
         * No proxy.
         */
        NoProxy,

        /**
         * Use the system proxie configuration.
         */
        UseSystemProxies,

        /**
         * Manual proxy configuration.
         */
        ManualProxy
    }

    private FileSystem fileSystem = null;
    private String databaseEncryptionPassword = null;
    private ProxyConfiguration proxyConfiguration = ProxyConfiguration.UseSystemProxies;
    private String proxyAddress = null;
    private int proxyPort = -1;

    /**
     * Creates a new {@code Settings} instance.
     *
     * @param fileSystem a {@code FileSystem} instance
     */
    public Settings(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        load();
    }

    /**
     * Loads the settings.
     */
    public void load() {
        Properties props = new Properties();
        InputStream is = null;

        try {
            // First try loading from the current directory
            try {
                File f = new File(fileSystem.getAppDir(), DesktopConstants.OPTIONS.SETTINGS_FILE);
                is = new FileInputStream(f);
            } catch (Exception e) {
                LOG.debug("Settings file {} not found in the application folder {}.",
                        DesktopConstants.OPTIONS.SETTINGS_FILE, fileSystem.getAppDirPath(), e);
                is = null;
            }

            try {
                if (null == is) {
                    // Try loading from classpath
                    is = getClass().getResourceAsStream(DesktopConstants.OPTIONS.SETTINGS_FILE);
                }

                // Try loading properties from the file (if found)
                props.load(is);
            } catch (Exception e) {
                LOG.debug("Settings file {} not found in the classpath.", DesktopConstants.OPTIONS.SETTINGS_FILE, e);
                is = null;
            }

            databaseEncryptionPassword = props.getProperty(DesktopConstants.OPTIONS.PROPERTY_DATABASE_ENCRYPTION_PASSWORD, null);
            proxyConfiguration = ProxyConfiguration.valueOf(
                    props.getProperty(DesktopConstants.OPTIONS.PROPERTY_PROXY_CONFIGURATION,
                            ProxyConfiguration.NoProxy.name()));
            proxyAddress = props.getProperty(DesktopConstants.OPTIONS.PROPERTY_PROXY_ADDRESS, null);
            proxyPort = getIntValue(props.getProperty(DesktopConstants.OPTIONS.PROPERTY_PROXY_PORT, null), -1);
        } finally {
            if (null!=is) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOG.error("Error when closing the properties file", e);
                }
            }
        }
    }

    /**
     * Saves the settings.
     */
    public void save() {
        OutputStream out = null;
        try {
            Properties props = new Properties();
            if (null!=databaseEncryptionPassword) {
                props.setProperty(DesktopConstants.OPTIONS.PROPERTY_DATABASE_ENCRYPTION_PASSWORD,
                        databaseEncryptionPassword);
            } else {
                props.remove(DesktopConstants.OPTIONS.PROPERTY_DATABASE_ENCRYPTION_PASSWORD);
            }
            if (null!=proxyConfiguration) {
                props.setProperty(DesktopConstants.OPTIONS.PROPERTY_PROXY_CONFIGURATION,
                        proxyConfiguration.name());
            } else {
                props.remove(DesktopConstants.OPTIONS.PROPERTY_PROXY_CONFIGURATION);
            }
            if (null!=proxyAddress) {
                props.setProperty(DesktopConstants.OPTIONS.PROPERTY_PROXY_ADDRESS, proxyAddress);
            } else {
                props.remove(DesktopConstants.OPTIONS.PROPERTY_PROXY_ADDRESS);
            }
            if (proxyPort>=0) {
                props.setProperty(DesktopConstants.OPTIONS.PROPERTY_PROXY_PORT, String.valueOf(proxyPort));
            } else {
                props.remove(DesktopConstants.OPTIONS.PROPERTY_PROXY_PORT);
            }
            File f = new File(fileSystem.getAppDir(), DesktopConstants.OPTIONS.SETTINGS_FILE);
            out = new FileOutputStream( f );
            props.store(out, "StorageCrypt settings");
        } catch (Exception e) {
            LOG.error("Error when writing the properties file {} in the application folder{}",
                    DesktopConstants.OPTIONS.SETTINGS_FILE, fileSystem.getAppDirPath(), e);
        } finally {
            if (null!=out) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error("Error when closing the properties file", e);
                }
            }
        }
    }

    /**
     * Sets the encrypted database encryption password.
     *
     * @param databaseEncryptionPassword the encrypted database encryption password
     */
    public void setDatabaseEncryptionPassword(String databaseEncryptionPassword) {
        this.databaseEncryptionPassword = databaseEncryptionPassword;
    }

    /**
     * Sets the proxy configuration type.
     *
     * @param proxyConfiguration the proxy configuration type
     */
    public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    /**
     * Sets the proxy address.
     *
     * @param proxyAddress the proxy address
     */
    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    /**
     * Sets the proxy port.
     *
     * @param proxyPort the proxy port
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * Returns the encrypted database encryption password.
     *
     * @return the encrypted database encryption password
     */
    public String getDatabaseEncryptionPassword() {
        return databaseEncryptionPassword;
    }

    /**
     * Returns the proxy configuration type.
     *
     * @return the proxy configuration type
     */
    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    /**
     * Returns the proxy address.
     *
     * @return the proxy address
     */
    public String getProxyAddress() {
        return proxyAddress;
    }

    /**
     * Returns the proxy port.
     *
     * @return the proxy port
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Converts the given {@code stringValue} as an integer.
     *
     * @param stringValue  the string value to convert
     * @param defaultValue the default value to return if the conversion fails
     * @return the int value
     */
    public static int getIntValue(String stringValue, int defaultValue) {
        if (null!=stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                LOG.error("Failed to convert '{}' to integer", stringValue, e);
            }
        }
        return defaultValue;
    }

    /**
     * Converts the given {@code stringValue} as a boolean.
     *
     * @param stringValue  the string value to convert
     * @param defaultValue the default value to return if the conversion fails
     * @return the boolean value
     */
    public static boolean getBooleanValue(String stringValue, boolean defaultValue) {
        if (null!=stringValue) {
            try {
                return Boolean.parseBoolean(stringValue);
            } catch (NumberFormatException e) {
                LOG.error("Failed to convert '{}' to boolean", stringValue, e);
            }
        }
        return defaultValue;
    }
}
