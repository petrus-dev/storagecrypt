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

package fr.petrus.tools.storagecrypt.desktop.windows.components;

import org.eclipse.jface.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.tools.storagecrypt.desktop.DocumentAction;

/**
 * This {@code Action} implementation describes an action on a particular {@code EncryptedDocument},
 * used for context menus.
 *
 * @author Pierre Sagne
 * @since 19.04.2016
 */
public class DocumentContextMenuAction extends Action {
    private static Logger LOG = LoggerFactory.getLogger(DocumentContextMenuAction.class);

    /**
     * This interface should be implemented by the class which will execute the action
     */
    public interface DocumentContextMenuActionListener {

        /**
         * Implement this method to execute the given {@code documentAction} on the given
         * {@code encryptedDocument}.
         *
         * @param documentAction    the action to execute on the given {@code encryptedDocument}
         * @param encryptedDocument the document to execute the {@code documentAction} on
         * @throws DatabaseConnectionClosedException if the database connection is closed
         */
        void executeContextMenuAction(DocumentAction documentAction, EncryptedDocument encryptedDocument)
                throws DatabaseConnectionClosedException;
    }

    private DocumentAction documentAction = null;
    private EncryptedDocument encryptedDocument = null;
    private DocumentContextMenuActionListener listener = null;

    /**
     * Creates a new {@code DocumentContextMenuAction} instance.
     *
     * @param text              the text describing the action
     * @param documentAction    the action to execute on the given {@code encryptedDocument}
     * @param encryptedDocument the document to execute the {@code documentAction} on
     * @param listener          the listener which will execute the action
     */
    public DocumentContextMenuAction(String text, DocumentAction documentAction,
                                     EncryptedDocument encryptedDocument,
                                     DocumentContextMenuActionListener listener) {
        super(text);
        this.documentAction = documentAction;
        this.encryptedDocument = encryptedDocument;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            listener.executeContextMenuAction(documentAction, encryptedDocument);
        } catch (DatabaseConnectionClosedException e) {
            LOG.error("Database is closed", e);
        }
    }
}
