package com.inkwell.database;

import com.inkwell.model.Relationship;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RelationshipRepository {
    private final DatabaseManager databaseManager;

    public RelationshipRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Relationship create(Relationship relationship) {
        String sql = """
                INSERT INTO relationships (source_character_id, target_character_id, story_id, type, description)
                VALUES (?, ?, ?, ?, ?);
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            setNullableInt(statement, 1, relationship.getSourceCharacterId());
            setNullableInt(statement, 2, relationship.getTargetCharacterId());
            setNullableInt(statement, 3, relationship.getStoryId());
            statement.setString(4, relationship.getType());
            statement.setString(5, relationship.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    relationship.setId(keys.getInt(1));
                }
            }
            return getById(relationship.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create relationship.", e);
        }
    }

    public Relationship getById(int id) {
        String sql = """
                SELECT id, source_character_id, target_character_id, story_id, type, description, created_at
                FROM relationships
                WHERE id = ?;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRelationship(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get relationship by id.", e);
        }
    }

    public List<Relationship> getAll() {
        String sql = """
                SELECT id, source_character_id, target_character_id, story_id, type, description, created_at
                FROM relationships
                ORDER BY created_at DESC;
                """;
        List<Relationship> relationships = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                relationships.add(mapRelationship(rs));
            }
            return relationships;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get relationships.", e);
        }
    }

    public List<Relationship> getByCharacterId(int characterId) {
        String sql = """
                SELECT id, source_character_id, target_character_id, story_id, type, description, created_at
                FROM relationships
                WHERE source_character_id = ? OR target_character_id = ?
                ORDER BY created_at DESC;
                """;
        List<Relationship> relationships = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, characterId);
            statement.setInt(2, characterId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    relationships.add(mapRelationship(rs));
                }
            }
            return relationships;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get relationships by character id.", e);
        }
    }

    public List<Relationship> getByStoryId(int storyId) {
        String sql = """
                SELECT id, source_character_id, target_character_id, story_id, type, description, created_at
                FROM relationships
                WHERE story_id = ?
                ORDER BY created_at DESC;
                """;
        List<Relationship> relationships = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, storyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    relationships.add(mapRelationship(rs));
                }
            }
            return relationships;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get relationships by story id.", e);
        }
    }

    public boolean update(Relationship relationship) {
        String sql = """
                UPDATE relationships
                SET source_character_id = ?,
                    target_character_id = ?,
                    story_id = ?,
                    type = ?,
                    description = ?
                WHERE id = ?;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableInt(statement, 1, relationship.getSourceCharacterId());
            setNullableInt(statement, 2, relationship.getTargetCharacterId());
            setNullableInt(statement, 3, relationship.getStoryId());
            statement.setString(4, relationship.getType());
            statement.setString(5, relationship.getDescription());
            statement.setInt(6, relationship.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update relationship.", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM relationships WHERE id = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete relationship.", e);
        }
    }

    private Relationship mapRelationship(ResultSet rs) throws SQLException {
        return new Relationship(
                rs.getInt("id"),
                getNullableInt(rs, "source_character_id"),
                getNullableInt(rs, "target_character_id"),
                getNullableInt(rs, "story_id"),
                rs.getString("type"),
                rs.getString("description"),
                rs.getString("created_at")
        );
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }
}
