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

package fr.petrus.lib.core.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * This class simply holds the database version number
 * <p/>
 * <p> It used by the {@link Database} implementations to track the database version, and plan
 * database structure upgrades.
 *
 * @author Pierre Sagne
 * @since 27.07.2015.
 */
@DatabaseTable(tableName = DatabaseConstants.DATABASE_INFO_TABLE)
public class DatabaseInfo {
    @DatabaseField(generatedId=true, columnName = DatabaseConstants.DATABASE_INFO_ID)
    private long id;

    @DatabaseField(columnName = DatabaseConstants.DATABASE_INFO_VERSION)
    private int version;

    /**
     * Creates a new {@code DatabaseInfo} instance.
     */
    public DatabaseInfo() {
        id = -1;
        version = 0;
    }

    /**
     * Returns the database id of this {@code DatabaseInfo}.
     *
     * @return the database id of this {@code DatabaseInfo}
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the version of the database.
     *
     * @param version the version of the database
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Returns the version of the database.
     *
     * @return the version of the database
     */
    public int getVersion() {
        return version;
    }
}
