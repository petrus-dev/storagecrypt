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

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.logger.LocalLog;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Callable;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionException;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The implementation of the Database instance for a H2 embedded password protected database
 *
 * @author Pierre Sagne
 * @since 17.07.2015.
 */
public class H2Database extends AbstractDatabase {

    private static Logger LOG = LoggerFactory.getLogger(H2Database.class);

    /** The name of the database file */
    private static final String DATABASE_NAME = "StorageCrypt";

    /** The database version. Increased every time the structure of the database changes */
    private static final int DATABASE_VERSION = 10;

    /** The driver class name for this type of database */
    private static final String DB_DRIVER = "org.h2.Driver";

    /** The user name used to access the database */
    private static final String DB_USER = "storagecrypt";

    /** The password used to access the database */
    private static final String DB_PASSWORD = "password";

    /** The folder where the database file is stored */
    private String databaseFolderPath = null;

    /** The connection source used to access the database */
    private ConnectionSource connectionSource = null;

    /** The DAO used to access the {@code DatabaseInfo} object, where we store the database version */
    private Dao<DatabaseInfo, Long> databaseInfoDao = null;

    /** The DAO used to access the {@code Account} objects */
    private Dao<Account, Long> accountDao = null;

    /** The DAO used to access the {@code EncryptedDocument} objects */
    private Dao<EncryptedDocument, Long> encryptedDocumentDao = null;

    /**
     * Creates a new {@code H2Database} instance, providing its dependencies.
     *
     * @param databaseFolderPath the path of the folder where the database file is stored
     * @param textI18n           a {@code TextI18n} instance
     */
    public H2Database(String databaseFolderPath, TextI18n textI18n) {
        super(textI18n);
        this.databaseFolderPath = databaseFolderPath;
    }

    @Override
    public void open(String databaseFilePassword) throws DatabaseConnectionException {
        System.setProperty(LocalLog.LOCAL_LOG_LEVEL_PROPERTY, Constants.ORMLITE.LOG_LEVEL);
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new DatabaseConnectionException("Failed to open encrypted database", e);
        }
        try {
            String databaseUrl = String.format("jdbc:h2:file:%s/%s;CIPHER=AES", databaseFolderPath, DATABASE_NAME);
            connectionSource = new JdbcConnectionSource(databaseUrl, DB_USER, databaseFilePassword + " " + DB_PASSWORD);
            /* Try to connect to the database */
            connectionSource.getReadWriteConnection();
            DatabaseInfo databaseInfo = null;
            try {
                databaseInfo = getDatabaseInfoDao().queryBuilder()
                        .orderBy(DatabaseConstants.DATABASE_INFO_VERSION, false)
                        .queryForFirst();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
            if (null==databaseInfo) {
                onCreate(connectionSource);
            } else if (databaseInfo.getVersion()!=DATABASE_VERSION) {
                onUpgrade(connectionSource, databaseInfo.getVersion(), DATABASE_VERSION);
            }
        } catch (DatabaseConnectionClosedException e) {
            throw new DatabaseConnectionException("Failed to open database", e);
        } catch (SQLException e) {
            connectionSource = null;
            throw new DatabaseConnectionException("Failed to open encrypted database", e);
        }
    }

    @Override
    public void open() throws DatabaseConnectionException {
        System.setProperty(LocalLog.LOCAL_LOG_LEVEL_PROPERTY, Constants.ORMLITE.LOG_LEVEL);
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new DatabaseConnectionException("Failed to open database", e);
        }
        try {
            String databaseUrl = String.format("jdbc:h2:file:%s/%s", databaseFolderPath, DATABASE_NAME);
            connectionSource = new JdbcConnectionSource(databaseUrl);
            DatabaseInfo databaseInfo = null;
            try {
                databaseInfo = getDatabaseInfoDao().queryBuilder()
                        .orderBy(DatabaseConstants.DATABASE_INFO_VERSION, false)
                        .queryForFirst();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
            if (null==databaseInfo) {
                onCreate(connectionSource);
            } else if (databaseInfo.getVersion()!=DATABASE_VERSION) {
                onUpgrade(connectionSource, databaseInfo.getVersion(), DATABASE_VERSION);
            }
        } catch (DatabaseConnectionClosedException e) {
            throw new DatabaseConnectionException("Failed to open database", e);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
            throw new DatabaseConnectionException("Failed to open database", e);
        }
    }

    @Override
    public boolean isOpen() {
        return null!=connectionSource;
    }

    @Override
    public void close() {
        if (isOpen()) {
            try {
                connectionSource.close();
                connectionSource = null;
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
    }

    /**
     * Creates the tables if they do not exist, and sets the version of the database.
     *
     * @param connectionSource the ORMLite connection source
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void onCreate(ConnectionSource connectionSource) throws DatabaseConnectionClosedException {
        if (!isOpen()) {
            throw new DatabaseConnectionClosedException("Database is closed");
        }
        try {
            TableUtils.createTableIfNotExists(connectionSource, DatabaseInfo.class);
            TableUtils.createTableIfNotExists(connectionSource, Account.class);
            TableUtils.createTableIfNotExists(connectionSource, EncryptedDocument.class);
            DatabaseInfo databaseInfo = new DatabaseInfo();
            databaseInfo.setVersion(DATABASE_VERSION);
            getDatabaseInfoDao().create(databaseInfo);
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    /**
     * Makes the necessary changes in the database structure from the given {@code oldVersion}
     * to the given {@code newVersion}, then change the database version of the database.
     *
     * @param connectionSource the ORMLite connection source
     * @param oldVersion the current version of the database
     * @param newVersion the version of the database the application expects
     * @throws DatabaseConnectionClosedException if the database connection is closed
     * @throws SQLException                      if an error occurs when making the change
     */
    private void onUpgrade(final ConnectionSource connectionSource,
                           final int oldVersion, final int newVersion)
            throws DatabaseConnectionClosedException, SQLException {
        if (!isOpen()) {
            throw new DatabaseConnectionClosedException("Database is closed");
        }
        callInTransaction(new Callable<Void>() {
            public Void call() throws Exception {
                DatabaseConnection connection = connectionSource.getReadWriteConnection();
                switch (oldVersion) {
                    //case 1:
                        //LOG.warn("Upgrading database from version {} to {}", oldVersion, newVersion);
                        // Example to add an INTEGER column to an existing table :
                        /* connection.executeStatement(String.format("alter table %s add column `%s` INTEGER",
                                DatabaseConstants.<NAME_OF_THE_TABLE>,
                                DatabaseConstants.<NAME_OF_THE_COLUMN_TO_ADD>),
                                DatabaseConnection.DEFAULT_RESULT_FLAGS);*/
                        // Example to add a BIGINT column to an existing table :
                        /* connection.executeStatement(String.format("alter table %s add column `%s` BIGINT",
                                DatabaseConstants.<NAME_OF_THE_TABLE>,
                                DatabaseConstants.<NAME_OF_THE_COLUMN_TO_ADD>),
                                DatabaseConnection.DEFAULT_RESULT_FLAGS);*/
                        // Example to add a VARCHAR column to an existing table :
                        /* connection.executeStatement(String.format("alter table %s add column `%s` VARCHAR",
                                DatabaseConstants.<NAME_OF_THE_TABLE>,
                                DatabaseConstants.<NAME_OF_THE_COLUMN_TO_ADD>),
                                DatabaseConnection.DEFAULT_RESULT_FLAGS);*/
                        // Example to add a VARCHAR column (2048 characters max) to an existing table :
                        /* connection.executeStatement(String.format("alter table %s add column `%s` VARCHAR(2048)",
                                DatabaseConstants.<NAME_OF_THE_TABLE>,
                                DatabaseConstants.<NAME_OF_THE_COLUMN_TO_ADD>),
                                DatabaseConnection.DEFAULT_RESULT_FLAGS);*/
                        // Example to rename an existing table :
                        /*connection.executeStatement(String.format("alter table %s rename to %s",
                                DatabaseConstants.<NAME_OF_THE_TABLE>,
                                DatabaseConstants.<NEW_NAME_OF_THE_TABLE>),
                                DatabaseConnection.DEFAULT_RESULT_FLAGS);*/
                        //updateDatabaseVersion(oldVersion, newVersion);
                        //break;
                    default:
                        LOG.warn("Upgrading database from version {} to {}, which will destroy all old data", oldVersion, newVersion);
                        try {
                            TableUtils.dropTable(connectionSource, DatabaseInfo.class, true);
                            TableUtils.dropTable(connectionSource, Account.class, true);
                            TableUtils.dropTable(connectionSource, EncryptedDocument.class, true);
                            connection.executeStatement("DROP TABLE `openstack_credentials`",
                                    DatabaseConnection.DEFAULT_RESULT_FLAGS);
                        } catch (SQLException e) {
                            LOG.error("exception during onUpgrade", e);
                        }
                        onCreate(connectionSource);
                }
                return null;
            }
        });
    }

    /**
     * Changes the version of the database.
     *
     * @param oldVersion the current version of the database
     * @param newVersion the new version of the database
     * @throws DatabaseConnectionClosedException if the database connection is closed
     * @throws SQLException                      if an error occurs when making the change
     */
    private void updateDatabaseVersion(int oldVersion, int newVersion) throws DatabaseConnectionClosedException, SQLException {
        UpdateBuilder<DatabaseInfo, Long> updateBuilder = getDatabaseInfoDao().updateBuilder();
        updateBuilder.updateColumnValue(DatabaseConstants.DATABASE_INFO_VERSION, newVersion)
                .where().eq(DatabaseConstants.DATABASE_INFO_VERSION, oldVersion);
        updateBuilder.update();
    }

    /**
     * Returns the DAO, used to access the {@code DatabaseInfo}.
     *
     * @return the DAO, used to access the {@code DatabaseInfo}
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private Dao<DatabaseInfo, Long> getDatabaseInfoDao() throws DatabaseConnectionClosedException {
        if (null == databaseInfoDao) {
            try {
                if (!isOpen()) {
                    throw new DatabaseConnectionClosedException("Database is closed");
                }
                databaseInfoDao = DaoManager.createDao(connectionSource, DatabaseInfo.class);
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new DatabaseConnectionClosedException("Failed to open database", e);
            }
        }
        return databaseInfoDao;
    }

    @Override
    protected Dao<Account, Long> getAccountDao() throws DatabaseConnectionClosedException {
        if (null == accountDao) {
            try {
                if (!isOpen()) {
                    throw new DatabaseConnectionClosedException("Database is closed");
                }
                accountDao = DaoManager.createDao(connectionSource, Account.class);
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new DatabaseConnectionClosedException("Failed to open database", e);
            }
        }
        return accountDao;
    }

    @Override
    protected Dao<EncryptedDocument, Long> getEncryptedDocumentDao() throws DatabaseConnectionClosedException {
        if (null == encryptedDocumentDao) {
            try {
                if (!isOpen()) {
                    throw new DatabaseConnectionClosedException("Database is closed");
                }
                encryptedDocumentDao = DaoManager.createDao(connectionSource, EncryptedDocument.class);
            } catch (SQLException e) {
                LOG.error("SQL error", e);
                throw new DatabaseConnectionClosedException("Failed to open database", e);
            }
        }
        return encryptedDocumentDao;
    }

    @Override
    public void resetDatabase() throws DatabaseConnectionClosedException {
        if (!isOpen()) {
            throw new DatabaseConnectionClosedException("Database is closed");
        }
        try {
            callInTransaction(new Callable<Void>() {
                public Void call() throws Exception {
                    // drop all tables
                    TableUtils.dropTable(connectionSource, DatabaseInfo.class, true);
                    TableUtils.dropTable(connectionSource, Account.class, true);
                    TableUtils.dropTable(connectionSource, EncryptedDocument.class, true);
                    DatabaseConnection connection = connectionSource.getReadWriteConnection();
                    connection.executeStatement("DROP TABLE `openstack_credentials`",
                            DatabaseConnection.DEFAULT_RESULT_FLAGS);

                    // then recreate them
                    onCreate(connectionSource);
                    return null;
                }
            });
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }
    }

    @Override
    public <T> T callInTransaction(Callable<T> callable)
            throws DatabaseConnectionClosedException, SQLException {
        if (!isOpen()) {
            throw new DatabaseConnectionClosedException("Database is closed");
        }
        return TransactionManager.callInTransaction(connectionSource, callable);
    }
}