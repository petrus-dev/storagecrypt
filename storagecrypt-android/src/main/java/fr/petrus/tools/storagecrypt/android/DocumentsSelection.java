package fr.petrus.tools.storagecrypt.android;

import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;

/**
 *  The interface used to manage encrypted documents selection.
 *
 * @author Pierre Sagne
 * @since 16.08.2016
 */
public interface DocumentsSelection {
    /**
     * Sets the documents list selection mode on or off.
     *
     * @param selectionMode if true, switch the selection mode on, if false, switch it off
     */
    void setSelectionMode(boolean selectionMode);

    /**
     * Returns true if the documents list selection mode is on.
     */
    boolean isInSelectionMode();

    /**
     * Clears the list of selected documents
     */
    void clearSelectedDocuments();

    /**
     * Sets the selection state of the given {@code encryptedDocument} to the given {@selected} state.
     *
     * @param encryptedDocument the document to set the selection state
     * @param selected          if true, selects the given {@code encryptedDocument} ; if false,
     *                          deselects it
     */
    void setDocumentSelected(EncryptedDocument encryptedDocument, boolean selected);

    /**
     * Returns whether the given {@code encryptedDocument} is selected.
     *
     * @param encryptedDocument the document to return the selection state
     * @return true if the given {@code encryptedDocument} is selected
     */
    boolean isDocumentSelected(EncryptedDocument encryptedDocument);

    /**
     * Returns the list of selected documents.
     *
     * @return the list of selected documents
     */
    List<EncryptedDocument> getSelectedDocuments();
}
