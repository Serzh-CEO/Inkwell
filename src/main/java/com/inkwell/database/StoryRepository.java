package com.inkwell.database;

import com.inkwell.model.Story;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StoryRepository {
    private final DatabaseManager databaseManager;

    public StoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Story create(Story story) {
        String sql = """
                INSERT INTO stories (project_id, title, summary, content, category)
                VALUES (?, ?, ?, ?, ?);
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            if (story.getProjectId() == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, story.getProjectId());
            }
            statement.setString(2, story.getTitle());
            statement.setString(3, story.getSummary());
            statement.setString(4, story.getContent());
            statement.setString(5, story.getCategory());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    story.setId(keys.getInt(1));
                }
            }
            return getById(story.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create story.", e);
        }
    }

    public Story getById(int id) {
        String sql = """
                SELECT id, project_id, title, summary, content, category, created_at, updated_at
                FROM stories
                WHERE id = ?;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapStory(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get story by id.", e);
        }
    }

    public List<Story> getAllByProjectId(int projectId) {
        String sql = """
                SELECT id, project_id, title, summary, content, category, created_at, updated_at
                FROM stories
                WHERE project_id = ?
                ORDER BY updated_at DESC;
                """;
        List<Story> stories = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                stories.add(mapStory(rs));
            }
            }
            return stories;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get stories.", e);
        }
    }

    public List<Story> searchByProject(int projectId, String query) {
        String sql = """
                SELECT id, project_id, title, summary, content, category, created_at, updated_at
                FROM stories
                WHERE project_id = ?
                  AND (lower(title) LIKE lower(?) OR lower(summary) LIKE lower(?) OR lower(content) LIKE lower(?))
                ORDER BY updated_at DESC;
                """;
        List<Story> stories = new ArrayList<>();
        String wildcard = "%" + query + "%";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            statement.setString(2, wildcard);
            statement.setString(3, wildcard);
            statement.setString(4, wildcard);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    stories.add(mapStory(rs));
                }
            }
            return stories;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to search stories.", e);
        }
    }

    public boolean update(Story story) {
        String sql = """
                UPDATE stories
                SET title = ?,
                    summary = ?,
                    content = ?,
                    category = ?,
                    project_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, story.getTitle());
            statement.setString(2, story.getSummary());
            statement.setString(3, story.getContent());
            statement.setString(4, story.getCategory());
            if (story.getProjectId() == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, story.getProjectId());
            }
            statement.setInt(6, story.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update story.", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM stories WHERE id = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete story.", e);
        }
    }

    private Story mapStory(ResultSet rs) throws SQLException {
        int projectValue = rs.getInt("project_id");
        Integer projectId = rs.wasNull() ? null : projectValue;
        return new Story(
                rs.getInt("id"),
                projectId,
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("content"),
                rs.getString("category"),
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }
}
