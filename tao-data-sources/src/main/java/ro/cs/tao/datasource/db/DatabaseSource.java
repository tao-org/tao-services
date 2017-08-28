/*
 *
 *  * Copyright (C) 2017 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *  *
 *
 */

package ro.cs.tao.datasource.db;

import org.apache.commons.lang.NotImplementedException;
import ro.cs.tao.component.Identifiable;
import ro.cs.tao.datasource.AbstractDataSource;
import ro.cs.tao.eodata.EOData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * @author Cosmin Cara
 */
public class DatabaseSource extends AbstractDataSource<EOData, DatabaseQuery> {

    protected final Logger logger;

    public DatabaseSource(String connectionString) {
        super(connectionString);
        this.logger = Logger.getLogger(DatabaseSource.class.getName());
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            this.logger.severe("PostgreSQL driver not registered");
        }
        addParameterProvider(null, new DatabaseParameterProvider());
    }

    @Override
    public boolean ping() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(this.connectionString);
        } catch (SQLException e) {
            this.logger.warning(e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    this.logger.warning(e.getMessage());
                }
            }
        }
        return true;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String defaultName() { return "NewDatabaseSource"; }

    @Override
    public Identifiable copy() {
        throw new NotImplementedException("This should not be called on this instance");
    }

    @Override
    protected DatabaseQuery createQueryImpl(String code) {
        return new DatabaseQuery(this, getParameterProvider(null));
    }

    Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(this.connectionString);
        } catch (SQLException e) {
            this.logger.warning(e.getMessage());
        }
        return connection;
    }
}