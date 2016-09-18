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

package fr.petrus.lib.core.rest.models.dropbox;

import fr.petrus.lib.core.rest.models.PrintableJson;

/**
 * This class holds a parameter used for some Dropbox API calls.
 *
 * <p>It can be converted to JSON when needed.
 *
 * @author Pierre Sagne
 * @since 09.11.2015
 */
public class ListFolderArg extends PrintableJson {
    public String path;
    public boolean recursive;
    public boolean include_media_info;
    public boolean include_deleted;

    public ListFolderArg(String path) {
        this(path, false);
    }

    public ListFolderArg(String path, boolean recursive) {
        this(path, recursive, false, false);
    }

    public ListFolderArg(String path, boolean recursive, boolean include_media_info, boolean include_deleted) {
        this.path = path;
        this.recursive = recursive;
        this.include_media_info = include_media_info;
        this.include_deleted = include_deleted;
    }
}
