package com.inkwell.database;

import com.inkwell.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    private final DatabaseManager databaseManager;

    public ProjectRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Project create(Project project) {
        String sql = """
                INSERT INTO projects (name, description)
                VALUES (?, ?);
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, project.getName());
            statement.setString(2, project.getDescription());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    project.setId(keys.getInt(1));
                }
            }
            return getById(project.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create project.", e);
        }
    }

    public Project getById(int id) {
        String sql = """
                SELECT id, name, description, created_at, updated_at
                FROM projects
                WHERE id = ?;
                """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapProject(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get project by id.", e);
        }
    }

    public List<Project> getAll() {
        String sql = """
                SELECT id, name, description, created_at, updated_at
                FROM projects
                ORDER BY updated_at DESC;
                """;
        List<Project> projects = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                projects.add(mapProject(rs));
            }
            return projects;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get projects.", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM projects WHERE id = ?;";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete project.", e);
        }
    }

    private Project mapProject(ResultSet rs) throws SQLException {
        return new Project(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }
}
