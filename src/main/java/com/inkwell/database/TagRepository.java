package com.inkwell.database;

import com.inkwell.model.Tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TagRepository {
    private final DatabaseManager databaseManager;

    public TagRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Tag createIfNotExists(String name) {
        try (Connection connection = databaseManager.getConnection()) {
            return createIfNotExists(connection, name);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create or fetch tag.", e);
        }
    }

    public List<Integer> getStoryIdsByTagName(String tagName) {
        String sql = """
                SELECT st.story_id
                FROM story_tags st
                INNER JOIN tags t ON t.id = st.tag_id
                WHERE lower(t.name) = lower(?)
                ORDER BY st.story_id ASC;
                """;
        List<Integer> storyIds = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tagName);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    storyIds.add(rs.getInt("story_id"));
                }
            }
            return storyIds;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get stories by tag.", e);
        }
    }

    private Tag createIfNotExists(Connection connection, String name) throws SQLException {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            return null;
        }

        String insertSql = "INSERT OR IGNORE INTO tags (name) VALUES (?);";
        String selectSql = "SELECT id, name FROM tags WHERE name = ?;";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            insertStatement.setString(1, normalizedName);
            insertStatement.executeUpdate();
        }
        try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
            selectStatement.setString(1, normalizedName);
            try (ResultSet rs = selectStatement.executeQuery()) {
                if (rs.next()) {
                    return new Tag(rs.getInt("id"), rs.getString("name"));
                }
            }
        }
        return null;
    }

    public List<Tag> getAll() {
        String sql = "SELECT id, name FROM tags ORDER BY name ASC;";
        List<Tag> tags = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                tags.add(new Tag(rs.getInt("id"), rs.getString("name")));
            }
            return tags;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get all tags.", e);
        }
    }

    public List<Tag> getByStoryId(int storyId) {
        String sql = """
                SELECT t.id, t.name
                FROM tags t
                INNER JOIN story_tags st ON st.tag_id = t.id
                WHERE st.story_id = ?
                ORDER BY t.name ASC;
                """;
        List<Tag> tags = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, storyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tags.add(new Tag(rs.getInt("id"), rs.getString("name")));
                }
            }
            return tags;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get tags by story id.", e);
        }
    }

    public void setStoryTags(int storyId, List<String> tagNames) {
        String deleteSql = "DELETE FROM story_tags WHERE story_id = ?;";
        String linkSql = "INSERT OR IGNORE INTO story_tags (story_id, tag_id) VALUES (?, ?);";

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                    deleteStatement.setInt(1, storyId);
                    deleteStatement.executeUpdate();
                }

                for (String tagName : tagNames) {
                    Tag tag = createIfNotExists(connection, tagName);
                    if (tag == null) {
                        continue;
                    }
                    try (PreparedStatement linkStatement = connection.prepareStatement(linkSql)) {
                        linkStatement.setInt(1, storyId);
                        linkStatement.setInt(2, tag.getId());
                        linkStatement.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to set tags for story.", e);
        }
    }
}
