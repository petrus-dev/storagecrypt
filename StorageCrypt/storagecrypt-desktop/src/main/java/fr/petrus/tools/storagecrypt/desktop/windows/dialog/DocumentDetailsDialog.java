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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.io.File;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.tools.storagecrypt.desktop.Resources;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The dialog used to show the details about a document.
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public class DocumentDetailsDialog extends CustomDialog<DocumentDetailsDialog> {

    private FileSystem fileSystem = null;
    private TextI18n textI18n = null;
    private Resources resources = null;

    private EncryptedDocument encryptedDocument = null;

    /**
     * Creates a new {@code DocumentDetailsDialog} instance for the given {@code encryptedDocument}.
     *
     * @param appWindow         the application window
     * @param encryptedDocument the encrypted document to display the details
     */
    public DocumentDetailsDialog(AppWindow appWindow,
                                 EncryptedDocument encryptedDocument) {
        super(appWindow);
        setClosable(true);
        setResizable(true);
        setTitle(textBundle.getString("document_details_dialog_title"));
        this.fileSystem = appWindow.getAppContext().getFileSystem();
        this.textI18n = appWindow.getAppContext().getTextI18n();
        this.resources = appWindow.getResources();
        this.encryptedDocument = encryptedDocument;
    }

    @Override
    protected void createDialogContents(Composite parent) {
        applyGridLayout(parent).numColumns(2);

        Composite headerGroup = new Composite(parent, SWT.NONE);
        applyGridLayout(headerGroup).numColumns(2);
        applyGridData(headerGroup).withHorizontalFill().horizontalSpan(2);

        Label imageLabel = new Label(headerGroup, SWT.NONE);
        if (encryptedDocument.isRoot()) {
            imageLabel.setImage(resources.loadImage("/res/drawable/ic_launcher.png"));
        } else if (encryptedDocument.isFolder()) {
            imageLabel.setImage(resources.loadImage("/res/drawable/ic_folder.png"));
        } else {
            imageLabel.setImage(resources.loadImage("/res/drawable/ic_file.png"));
        }
        applyGridData(imageLabel).horizontalAlignment(SWT.FILL);

        Label nameLabel = new Label(headerGroup, SWT.NONE);
        if (encryptedDocument.isRoot()) {
            nameLabel.setText(encryptedDocument.storageText());
        } else {
            nameLabel.setText(encryptedDocument.getDisplayName());
        }
        applyGridData(nameLabel).withHorizontalFill();

        if (!encryptedDocument.isRoot() && !encryptedDocument.isFolder()) {
            Label mimeTypeLabel = new Label(parent, SWT.NONE);
            mimeTypeLabel.setText(textBundle.getString("document_details_dialog_mime_type_text"));
            applyGridData(mimeTypeLabel).withHorizontalFill();

            Label mimeTypeValueLabel = new Label(parent, SWT.NONE);
            mimeTypeValueLabel.setText(encryptedDocument.getMimeType());
            applyGridData(mimeTypeValueLabel).withHorizontalFill();
        }

        Label keyAliasLabel = new Label(parent, SWT.NONE);
        keyAliasLabel.setText(textBundle.getString("document_details_dialog_key_alias_text"));
        applyGridData(keyAliasLabel).withHorizontalFill();

        Label keyAliasValueLabel = new Label(parent, SWT.NONE);
        keyAliasValueLabel.setText(encryptedDocument.getKeyAlias());
        applyGridData(keyAliasValueLabel).withHorizontalFill();

        if (!encryptedDocument.isRoot() && encryptedDocument.getBackEntryVersion()>0) {
            Label versionLabel = new Label(parent, SWT.NONE);
            versionLabel.setText(textBundle.getString("document_details_dialog_version_text"));
            applyGridData(versionLabel).withHorizontalFill();

            Label versionValueLabel = new Label(parent, SWT.NONE);
            versionValueLabel.setText(String.valueOf(encryptedDocument.getBackEntryVersion()));
            applyGridData(versionValueLabel).withHorizontalFill();
        }

        if (!encryptedDocument.isRoot()) {
            if (encryptedDocument.getLocalModificationTime()>0) {
                Label localModificationTimeLabel = new Label(parent, SWT.NONE);
                localModificationTimeLabel.setText(textBundle.getString("document_details_dialog_local_modification_time_text"));
                applyGridData(localModificationTimeLabel).withHorizontalFill();

                Label localModificationTimeValueLabel = new Label(parent, SWT.NONE);
                localModificationTimeValueLabel.setText(textI18n.getTimeText(encryptedDocument.getLocalModificationTime()));
                applyGridData(localModificationTimeValueLabel).withHorizontalFill();
            }

            if (encryptedDocument.getRemoteModificationTime()>0) {
                Label remoteModificationTimeLabel = new Label(parent, SWT.NONE);
                remoteModificationTimeLabel.setText(textBundle.getString("document_details_dialog_remote_modification_time_text"));
                applyGridData(remoteModificationTimeLabel).withHorizontalFill();

                Label remoteModificationTimeValueLabel = new Label(parent, SWT.NONE);
                remoteModificationTimeValueLabel.setText(textI18n.getTimeText(encryptedDocument.getRemoteModificationTime()));
                applyGridData(remoteModificationTimeValueLabel).withHorizontalFill();
            }
        }

        Label storageTypeLabel = new Label(parent, SWT.NONE);
        storageTypeLabel.setText(textBundle.getString("document_details_dialog_storage_type_text"));
        applyGridData(storageTypeLabel).withHorizontalFill();

        Label storageTypeValueLabel = new Label(parent, SWT.NONE);
        storageTypeValueLabel.setText(textI18n.getStorageTypeText(encryptedDocument.getBackStorageType()));
        applyGridData(storageTypeValueLabel).withHorizontalFill();

        if (null != encryptedDocument.getBackStorageAccount()) {
            Label storageAccountLabel = new Label(parent, SWT.NONE);
            storageAccountLabel.setText(textBundle.getString("document_details_dialog_storage_account_text"));
            applyGridData(storageAccountLabel).withHorizontalFill();

            Label storageAccountValueLabel = new Label(parent, SWT.NONE);
            storageAccountValueLabel.setText(encryptedDocument.getBackStorageAccount().getAccountName());
            applyGridData(storageAccountValueLabel).withHorizontalFill();
        }

        if (!encryptedDocument.isRoot() && !encryptedDocument.isFolder()) {
            Label sizeLabel = new Label(parent, SWT.NONE);
            sizeLabel.setText(textBundle.getString("document_details_dialog_size_text"));
            applyGridData(sizeLabel).withHorizontalFill();

            Label sizeValueLabel = new Label(parent, SWT.NONE);
            sizeValueLabel.setText(encryptedDocument.getSizeText());
            applyGridData(sizeValueLabel).withHorizontalFill();
        }

        long quotaAmount;
        long quotaFree;
        long quotaUsed;
        if (encryptedDocument.isUnsynchronized()) {
            File localAppFolder = fileSystem.getAppDir();
            quotaAmount = localAppFolder.getTotalSpace();
            quotaFree = localAppFolder.getFreeSpace();
            quotaUsed = quotaAmount - quotaFree;
        } else {
            quotaAmount = encryptedDocument.getBackStorageAccount().getQuotaAmount();
            quotaUsed = encryptedDocument.getBackStorageAccount().getQuotaUsed();
            if (quotaAmount>=0 && quotaUsed>=0) {
                quotaFree = quotaAmount - quotaUsed;
            } else {
                quotaFree = -1;
            }
        }
        if (quotaAmount>=0 && quotaUsed >=0 && quotaFree>=0) {
            Label quotaAmountLabel = new Label(parent, SWT.NONE);
            quotaAmountLabel.setText(textBundle.getString("document_details_dialog_quota_amount_text"));
            applyGridData(quotaAmountLabel).withHorizontalFill();

            Label quotaAmountValueLabel = new Label(parent, SWT.NONE);
            quotaAmountValueLabel.setText(textI18n.getSizeText(quotaAmount));
            applyGridData(quotaAmountValueLabel).withHorizontalFill();

            Label quotaUsedLabel = new Label(parent, SWT.NONE);
            quotaUsedLabel.setText(textBundle.getString("document_details_dialog_quota_used_text"));
            applyGridData(quotaUsedLabel).withHorizontalFill();

            Label quotaUsedValueLabel = new Label(parent, SWT.NONE);
            quotaUsedValueLabel.setText(textI18n.getSizeText(quotaUsed));
            applyGridData(quotaUsedValueLabel).withHorizontalFill();

            Label quotaFreeLabel = new Label(parent, SWT.NONE);
            quotaFreeLabel.setText(textBundle.getString("document_details_dialog_quota_free_text"));
            applyGridData(quotaFreeLabel).withHorizontalFill();

            Label quotaFreeValueLabel = new Label(parent, SWT.NONE);
            quotaFreeValueLabel.setText(textI18n.getSizeText(quotaFree));
            applyGridData(quotaFreeValueLabel).withHorizontalFill();
        }
    }
}
