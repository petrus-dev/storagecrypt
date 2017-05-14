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

package fr.petrus.lib.core.processes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.EncryptedDocumentMetadata;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.filesystem.FileSystem;
import fr.petrus.lib.core.i18n.TextI18n;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.result.ProgressListener;

/**
 * The {@code Process} which handles documents move.
 *
 * @author Pierre Sagne
 * @since 10.05.2017
 */
public class DocumentsMoveProcess extends AbstractProcess<DocumentsMoveProcess.Results> {

    private static Logger LOG = LoggerFactory.getLogger(DocumentsMoveProcess.class);

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     * <p/>
     * <p>This process returns no results by this means so this implementation holds nothing
     */
    public static class Results extends BaseProcessResults<Void, Void> {
        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super(textI18n, false, false, false);
        }
    }

    private Crypto crypto;
    private KeyManager keyManager;
    private FileSystem fileSystem;
    private ProgressListener progressListener;

    /**
     * Creates a new {@code DocumentsMoveProcess}, providing its dependencies.
     *
     * @param crypto     a {@code Crypto} instance
     * @param keyManager a {@code KeyManager} instance
     * @param textI18n   a {@code TextI18n} instance
     * @param fileSystem a {@code FileSystem} instance
     */
    public DocumentsMoveProcess(Crypto crypto, KeyManager keyManager, TextI18n textI18n,
                                FileSystem fileSystem) {
        super(new Results(textI18n));
        this.crypto = crypto;
        this.keyManager = keyManager;
        this.fileSystem = fileSystem;
        progressListener = null;
    }

    /**
     * Sets the {@code ProgressListener} which this process will report its progress to.
     *
     * @param progressListener the {@code ProgressListener} which this process will report its progress to
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Moves the given {@code srcDocuments} into the given {@code dstFolder}.
     *
     * @param srcDocuments  the list of {@code EncryptedDocument}s to decrypt
     * @param dstFolder     the destination folder
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void moveDocuments(List<EncryptedDocument> srcDocuments, EncryptedDocument dstFolder)
            throws DatabaseConnectionClosedException {
        start();

        if (null != srcDocuments && srcDocuments.size() > 0) {
            final List<EncryptedDocument> allDocuments = new ArrayList<>();
            final Map<EncryptedDocument, List<EncryptedDocument>> foldersChildren = new HashMap<>();
            final List<EncryptedDocument> createdDocuments = new ArrayList<>();

            for (EncryptedDocument document: srcDocuments) {
                allDocuments.addAll(document.unfoldAsList(true));
                recursivelyBuildFoldersChildrenMap(foldersChildren, document);
            }

            if (null != progressListener) {
                progressListener.onMessage(0, dstFolder.failSafeLogicalPath());
                progressListener.onSetMax(0, allDocuments.size());
                progressListener.onProgress(0, 0);
                progressListener.onSetMax(1, 1);
                progressListener.onProgress(1, 0);
            }

            try {
                for (EncryptedDocument srcDocument: srcDocuments) {
                    recursivelyCopyDocument(srcDocument, dstFolder,
                            foldersChildren, createdDocuments);
                }
                if (isCanceled()) {
                    undoMoves(createdDocuments);
                } else {
                    // if everything is ok, remove source documents in reverse order
                    for (int i = allDocuments.size() - 1; i >= 0; i--) {
                        allDocuments.get(i).delete();
                    }
                }
            } catch (IOException e) {
                LOG.debug("Error when moving documents to {}",
                        dstFolder.failSafeLogicalPath(), e);
                undoMoves(createdDocuments);
            } catch (StorageCryptException e) {
                LOG.debug("Error when moving documents to {}",
                        dstFolder.failSafeLogicalPath(), e);
                undoMoves(createdDocuments);
            } catch (DatabaseConnectionClosedException e) {
                LOG.debug("Error when moving documents to {}",
                        dstFolder.failSafeLogicalPath(), e);
                undoMoves(createdDocuments);
                throw e;
            }
        }
    }

    private void recursivelyBuildFoldersChildrenMap(Map<EncryptedDocument,
                                                    List<EncryptedDocument>> foldersChildren,
                                                    EncryptedDocument document)
            throws DatabaseConnectionClosedException {
        if (document.isFolder()) {
            List<EncryptedDocument> children = document.children(false);
            foldersChildren.put(document, children);
            for (EncryptedDocument child: children) {
                recursivelyBuildFoldersChildrenMap(foldersChildren, child);
            }
        }
    }

    private void recursivelyCopyDocument(EncryptedDocument srcDocument, EncryptedDocument dstFolder,
                                         Map<EncryptedDocument, List<EncryptedDocument>> foldersChildren,
                                         List<EncryptedDocument> createdDocuments)
            throws DatabaseConnectionClosedException, StorageCryptException, IOException {
        pauseIfNeeded();
        if (isCanceled()) {
            return;
        }

        LOG.debug("Moving document {} to {}", srcDocument.failSafeLogicalPath(), dstFolder.failSafeLogicalPath());

        if (null != progressListener) {
            progressListener.onProgress(0, createdDocuments.size());
            progressListener.onMessage(1, srcDocument.getDisplayName());
            progressListener.onProgress(1, 0);
            progressListener.onSetMax(1, 1);
        }

        if (srcDocument.isFolder()) {
            LOG.debug("  - document {} is a folder", srcDocument.failSafeLogicalPath());
            EncryptedDocument newFolder = dstFolder.createChild(
                    srcDocument.getDisplayName(), srcDocument.getMimeType(),
                    srcDocument.getKeyAlias());
            createdDocuments.add(0, newFolder);
            for (EncryptedDocument child: foldersChildren.get(srcDocument)) {
                recursivelyCopyDocument(child, newFolder, foldersChildren, createdDocuments);
            }
        } else {
            LOG.debug("  - document {} is a file", srcDocument.failSafeLogicalPath());
            File sourceEncryptedFile = srcDocument.file();
            File destinationEncryptedFile = new File(dstFolder.file(),
                    sourceEncryptedFile.getName());

            EncryptedDocumentMetadata metadata = new EncryptedDocumentMetadata(crypto, keyManager);
            metadata.setMetadata(srcDocument.getMimeType(),
                    srcDocument.getDisplayName(), srcDocument.getKeyAlias());
            fileSystem.copyFile(sourceEncryptedFile, destinationEncryptedFile);
            EncryptedDocument newDocument =
                    dstFolder.createChild(metadata, destinationEncryptedFile);
            createdDocuments.add(0, newDocument);
        }
        if (null != progressListener) {
            progressListener.onProgress(1, 1);
        }
    }

    private void undoMoves(List<EncryptedDocument> createdDocuments) {
        for (EncryptedDocument createdDocument: createdDocuments) {
            try {
                createdDocument.delete();
            } catch (DatabaseConnectionClosedException e) {
                LOG.error("Error when removing document {}", createdDocument.failSafeLogicalPath(), e);
            }
        }
    }
}
