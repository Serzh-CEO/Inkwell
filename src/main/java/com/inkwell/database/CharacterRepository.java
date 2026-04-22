package com.inkwell.database;

import com.inkwell.model.Character;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CharacterRepository {
    private final DatabaseManager databaseManager;

    public CharacterRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Character create(Character character) {
        String sql = """
                INSERT INTO characters (project_id, name, description, traits, story_id)
                VALUES (?, ?, ?, ?, ?);
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            if (character.getProjectId() == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, character.getProjectId());
            }
            statement.setString(2, character.getName());
            statement.setString(3, character.getDescription());
            statement.setString(4, character.getTraits());
            if (character.getStoryId() == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, character.getStoryId());
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    character.setId(keys.getInt(1));
                }
            }
            return getById(character.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create character.", e);
        }
    }

    public Character getById(int id) {
        String sql = """
                SELECT id, project_id, name, description, traits, story_id, created_at, updated_at
                FROM characters
                WHERE id = ?;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapCharacter(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get character by id.", e);
        }
    }

    public List<Character> getAllByProjectId(int projectId) {
        String sql = """
                SELECT id, project_id, name, description, traits, story_id, created_at, updated_at
                FROM characters
                WHERE project_id = ?
                ORDER BY updated_at DESC;
                """;
        List<Character> characters = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    characters.add(mapCharacter(rs));
                }
            }
            return characters;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get characters.", e);
        }
    }

    public List<Character> getByStoryId(int storyId) {
        String sql = """
                SELECT id, project_id, name, description, traits, story_id, created_at, updated_at
                FROM characters
                WHERE story_id = ?
                ORDER BY updated_at DESC;
                """;
        List<Character> characters = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, storyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    characters.add(mapCharacter(rs));
                }
            }
            return characters;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get characters by story id.", e);
        }
    }

    public boolean update(Character character) {
        String sql = """
                UPDATE characters
                SET name = ?,
                    description = ?,
                    traits = ?,
                    story_id = ?,
                    project_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, character.getName());
            statement.setString(2, character.getDescription());
            statement.setString(3, character.getTraits());
            if (character.getStoryId() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, character.getStoryId());
            }
            if (character.getProjectId() == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, character.getProjectId());
            }
            statement.setInt(6, character.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update character.", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM characters WHERE id = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete character.", e);
        }
    }

    private Character mapCharacter(ResultSet rs) throws SQLException {
        int storyValue = rs.getInt("story_id");
        Integer storyId = rs.wasNull() ? null : storyValue;
        return new Character(
                rs.getInt("id"),
                getNullableInt(rs, "project_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("traits"),
                storyId,
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
