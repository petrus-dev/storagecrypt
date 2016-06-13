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

package fr.petrus.tools.storagecrypt.android.events;

import org.greenrobot.eventbus.EventBus;

import fr.petrus.lib.core.processes.DocumentsImportProcess;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsImportTask;

/**
 * The {@link EventBus} event cast when the {@link DocumentsImportTask} is done.
 *
 * @author Pierre Sagne
 * @since 11.04.2015
 */
public class DocumentsImportDoneEvent extends Event {
    private DocumentsImportProcess.Results result;

    /**
     * Creates a new {@code DocumentsImportDoneEvent} instance with the given {@code results}.
     *
     * @param result the results of the {@link DocumentsImportTask} execution
     */
    public DocumentsImportDoneEvent(DocumentsImportProcess.Results result) {
        this.result = result;
    }

    /**
     * Returns the results of the {@link DocumentsImportTask} execution.
     *
     * @return the results of the {@link DocumentsImportTask} execution
     */
    public DocumentsImportProcess.Results getResults() {
        return result;
    }
}
