package com.inkwell.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DatabaseConfig.getDbUrl());
        connection.createStatement().execute("PRAGMA foreign_keys = ON;");
        return connection;
    }
}
