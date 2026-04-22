package com.inkwell.database;

public final class DatabaseConfig {
    private static final String DB_FILE_NAME = "inkwell.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE_NAME;

    private DatabaseConfig() {
    }

    public static String getDbUrl() {
        return DB_URL;
    }
}
