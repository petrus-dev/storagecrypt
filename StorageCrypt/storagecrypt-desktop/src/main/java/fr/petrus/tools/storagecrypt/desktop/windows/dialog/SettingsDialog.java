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

package fr.petrus.tools.storagecrypt.desktop.windows.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fr.petrus.tools.storagecrypt.desktop.Settings;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to let the user edit the application settings.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class SettingsDialog extends CustomDialog<SettingsDialog> {

    private Settings settings;
    private Settings.ProxyConfiguration proxyConfiguration = null;
    private String proxyAddress = null;
    private String proxyPort = null;

    /**
     * Creates a new {@code SettingsDialog} instance.
     *
     * @param appWindow the application window
     */
    public SettingsDialog(AppWindow appWindow) {
        super(appWindow);
        setClosable(true);
        setResizable(false);
        setTitle(textBundle.getString("settings_dialog_title"));
        setPositiveButtonText(textBundle.getString("settings_dialog_save_button_text"));
        setNegativeButtonText(textBundle.getString("settings_dialog_cancel_button_text"));
        this.settings = appWindow.getSettings();
        proxyConfiguration = settings.getProxyConfiguration();
        proxyAddress = settings.getProxyAddress();
        if (settings.getProxyPort()>0) {
            proxyPort = String.valueOf(settings.getProxyPort());
        } else {
            proxyPort = null;
        }
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Group proxyConfigurationGroup = new Group(parent, SWT.SHADOW_IN);
        applyGridData(proxyConfigurationGroup).withHorizontalFill().horizontalSpan(2);
        proxyConfigurationGroup.setText(textBundle.getString("settings_dialog_proxy_configuration_text"));
        proxyConfigurationGroup.setLayout(new RowLayout(SWT.VERTICAL));

        final Button noProxyButton = new Button(proxyConfigurationGroup, SWT.RADIO);
        noProxyButton.setText(textBundle.getString("settings_dialog_proxy_configuration_no_proxy_text"));

        final Button systemProxiesButton = new Button(proxyConfigurationGroup, SWT.RADIO);
        systemProxiesButton.setText(textBundle.getString("settings_dialog_proxy_configuration_use_system_proxies_text"));

        final Button manualProxyButton = new Button(proxyConfigurationGroup, SWT.RADIO);
        manualProxyButton.setText(textBundle.getString("settings_dialog_proxy_configuration_manual_configuration_text"));

        switch (proxyConfiguration) {
            case NoProxy:
                noProxyButton.setSelection(true);
                break;
            case UseSystemProxies:
                systemProxiesButton.setSelection(true);
                break;
            case ManualProxy:
                manualProxyButton.setSelection(true);
                break;
        }

        SelectionListener proxyConfigurationButtonListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                if (noProxyButton.getSelection()) {
                    proxyConfiguration = Settings.ProxyConfiguration.NoProxy;
                } else if (systemProxiesButton.getSelection()) {
                    proxyConfiguration = Settings.ProxyConfiguration.UseSystemProxies;
                } else if (manualProxyButton.getSelection()) {
                    proxyConfiguration = Settings.ProxyConfiguration.ManualProxy;
                }
            }
        };
        noProxyButton.addSelectionListener(proxyConfigurationButtonListener);
        systemProxiesButton.addSelectionListener(proxyConfigurationButtonListener);
        manualProxyButton.addSelectionListener(proxyConfigurationButtonListener);

        Label proxyAddressLabel = new Label(parent, SWT.NONE);
        proxyAddressLabel.setText(textBundle.getString("settings_dialog_proxy_configuration_proxy_address_text"));
        applyGridData(proxyAddressLabel).withHorizontalFill();

        final Text proxyAddressText = new Text(parent, SWT.BORDER);
        if (null!=proxyAddress) {
            proxyAddressText.setText(proxyAddress);
        }
        proxyAddressText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                proxyAddress = proxyAddressText.getText();
                checkInputValidity();
            }
        });
        applyGridData(proxyAddressText).withHorizontalFill();

        Label proxyPortLabel = new Label(parent, SWT.NONE);
        proxyPortLabel.setText(textBundle.getString("settings_dialog_proxy_configuration_proxy_port_text"));
        applyGridData(proxyPortLabel).withHorizontalFill();

        final Text proxyPortText = new Text(parent, SWT.BORDER);
        if (null!=proxyPort) {
            proxyPortText.setText(proxyPort);
        }
        proxyPortText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                proxyPort = proxyPortText.getText();
                checkInputValidity();
            }
        });
        applyGridData(proxyPortText).withHorizontalFill();

        validateOnReturnPressed(proxyAddressText, proxyPortText);
    }

    @Override
    protected void returnResult(boolean result) {
        if (result) {
            settings.setProxyConfiguration(proxyConfiguration);
            settings.setProxyAddress(proxyAddress);
            if (null!=proxyPort) {
                settings.setProxyPort(Integer.parseInt(proxyPort));
            }
            settings.save();
        }
        super.returnResult(result);
    }

    @Override
    protected boolean isInputValid() {
        try {
            return null==proxyPort || Integer.parseInt(proxyPort) > 0;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }
}
