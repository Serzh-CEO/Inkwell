package com.inkwell.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public void initialize() {
        try (Connection connection = DriverManager.getConnection(DatabaseConfig.getDbUrl());
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            createTables(statement);
            migrateProjectColumns(connection, statement);
            insertSampleData(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize SQLite database.", e);
        }
    }

    private void createTables(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    description TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS stories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER,
                    title TEXT NOT NULL,
                    summary TEXT,
                    content TEXT,
                    category TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                );
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS characters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER,
                    name TEXT NOT NULL,
                    description TEXT,
                    traits TEXT,
                    story_id INTEGER,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE SET NULL
                );
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS story_tags (
                    story_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    PRIMARY KEY (story_id, tag_id),
                    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                );
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS relationships (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_character_id INTEGER,
                    target_character_id INTEGER,
                    story_id INTEGER,
                    type TEXT NOT NULL,
                    description TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (source_character_id) REFERENCES characters(id) ON DELETE CASCADE,
                    FOREIGN KEY (target_character_id) REFERENCES characters(id) ON DELETE CASCADE,
                    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
                );
                """);

        // ── Groups ───────────────────────────────────────────────────────────
        statement.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL CHECK(type IN ('story','character')),
                    color TEXT NOT NULL DEFAULT '#5b7cf6',
                    collapsed INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                );
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS group_members (
                    group_id INTEGER NOT NULL,
                    member_id INTEGER NOT NULL,
                    PRIMARY KEY (group_id, member_id),
                    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
                );
                """);
    }

    private void migrateProjectColumns(Connection connection, Statement statement) throws SQLException {
        statement.execute("""
                INSERT OR IGNORE INTO projects (id, name, description)
                VALUES (1, 'Default Project', 'Initial workspace project');
                """);

        ensureColumnExists(connection, statement, "stories", "project_id", "INTEGER");
        ensureColumnExists(connection, statement, "characters", "project_id", "INTEGER");

        statement.execute("UPDATE stories SET project_id = 1 WHERE project_id IS NULL;");
        statement.execute("UPDATE characters SET project_id = 1 WHERE project_id IS NULL;");
    }

    private void ensureColumnExists(Connection connection, Statement statement,
                                     String tableName, String columnName, String type) throws SQLException {
        try (ResultSet resultSet = connection.createStatement()
                .executeQuery("PRAGMA table_info(" + tableName + ");")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) return;
            }
        }
        statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + type + ";");
    }

    private void insertSampleData(Statement statement) throws SQLException {
        statement.execute("""
                INSERT OR IGNORE INTO projects (id, name, description)
                VALUES
                    (1, 'Default Project', 'Initial workspace project'),
                    (2, 'Chronicles Sandbox', 'Second project for separated worlds and casts');
                """);

        statement.execute("""
                INSERT OR IGNORE INTO stories (id, project_id, title, summary, content, category)
                VALUES
                    (1, 1, 'Echoes of Dawn', 'A city wakes to a forgotten prophecy.', 'Draft notes for chapter one.', 'Fantasy'),
                    (2, 2, 'Steel & Salt', 'A crew crosses dangerous northern seas.', 'Worldbuilding snippets and route maps.', 'Adventure');
                """);

        statement.execute("""
                INSERT OR IGNORE INTO characters (id, project_id, name, description, traits, story_id)
                VALUES
                    (1, 1, 'Mira Vale', 'Young archivist searching for truth.', 'curious, stubborn, brave', 1),
                    (2, 2, 'Captain Rhys', 'Sea captain with a hidden debt.', 'calm, strategic, loyal', 2),
                    (3, 1, 'Iven Korr', 'Mercenary tied to old royal bloodlines.', 'silent, observant, ruthless', 1);
                """);

        statement.execute("""
                INSERT OR IGNORE INTO tags (id, name)
                VALUES (1, 'magic'), (2, 'politics'), (3, 'voyage');
                """);

        statement.execute("""
                INSERT OR IGNORE INTO story_tags (story_id, tag_id)
                VALUES (1, 1), (1, 2), (2, 3);
                """);

        statement.execute("""
                INSERT OR IGNORE INTO relationships (id, source_character_id, target_character_id, story_id, type, description)
                VALUES
                    (1, 1, 3, 1, 'alliance', 'Mira and Iven cooperate to decode the prophecy.'),
                    (2, 2, 1, 2, 'mentor', 'Rhys later guides Mira through sea routes and politics.');
                """);

        // Sample groups (only inserted if not yet present)
        statement.execute("""
                INSERT OR IGNORE INTO groups (id, project_id, name, type, color)
                VALUES (1, 1, 'Main Arc', 'story', '#5b7cf6');
                """);
        statement.execute("""
                INSERT OR IGNORE INTO group_members (group_id, member_id)
                VALUES (1, 1);
                """);
    }
}
