package com.inkwell.database;

import com.inkwell.model.Group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GroupRepository {
    private final DatabaseManager databaseManager;

    public GroupRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // ─── Create ─────────────────────────────────────────

    public Group create(Group group) {
        String sql = """
                INSERT INTO groups (project_id, name, type, color, collapsed)
                VALUES (?, ?, ?, ?, ?);
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, group.getProjectId());
            stmt.setString(2, group.getName());
            stmt.setString(3, group.getType());
            stmt.setString(4, group.getColor());
            stmt.setInt(5, group.isCollapsed() ? 1 : 0);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) group.setId(keys.getInt(1));
            }
            return getById(group.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create group.", e);
        }
    }

    // ─── Read ────────────────────────────────────────────

    public Group getById(int id) {
        String sql = """
                SELECT id, project_id, name, type, color, collapsed, created_at
                FROM groups WHERE id = ?;
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Group g = mapGroup(rs);
                    loadMembers(conn, g);
                    return g;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get group by id.", e);
        }
    }

    public List<Group> getAllByProjectId(int projectId, String type) {
        String sql = """
                SELECT id, project_id, name, type, color, collapsed, created_at
                FROM groups
                WHERE project_id = ? AND type = ?
                ORDER BY name ASC;
                """;
        List<Group> groups = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            stmt.setString(2, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Group g = mapGroup(rs);
                    loadMembers(conn, g);
                    groups.add(g);
                }
            }
            return groups;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get groups.", e);
        }
    }

    // ─── Update ──────────────────────────────────────────

    public boolean update(Group group) {
        String sql = """
                UPDATE groups
                SET name = ?, color = ?, collapsed = ?
                WHERE id = ?;
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getColor());
            stmt.setInt(3, group.isCollapsed() ? 1 : 0);
            stmt.setInt(4, group.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update group.", e);
        }
    }

    public boolean updateCollapsed(int groupId, boolean collapsed) {
        String sql = "UPDATE groups SET collapsed = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, collapsed ? 1 : 0);
            stmt.setInt(2, groupId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update group collapsed state.", e);
        }
    }

    // ─── Delete ──────────────────────────────────────────

    public boolean delete(int id) {
        String sql = "DELETE FROM groups WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete group.", e);
        }
    }

    // ─── Membership ──────────────────────────────────────

    public void setMembers(int groupId, List<Integer> memberIds) {
        String deleteSql = "DELETE FROM group_members WHERE group_id = ?;";
        String insertSql = "INSERT OR IGNORE INTO group_members (group_id, member_id) VALUES (?, ?);";
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                    del.setInt(1, groupId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (int memberId : memberIds) {
                        ins.setInt(1, groupId);
                        ins.setInt(2, memberId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to set group members.", e);
        }
    }

    /** Returns the group id for a given member, or null if ungrouped. */
    public Integer getGroupIdForMember(int memberId, String type) {
        String sql = """
                SELECT gm.group_id
                FROM group_members gm
                JOIN groups g ON g.id = gm.group_id
                WHERE gm.member_id = ? AND g.type = ?
                LIMIT 1;
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memberId);
            stmt.setString(2, type);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("group_id");
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get group for member.", e);
        }
    }

    // ─── Private ─────────────────────────────────────────

    private Group mapGroup(ResultSet rs) throws SQLException {
        return new Group(
                rs.getInt("id"),
                rs.getInt("project_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("color"),
                rs.getInt("collapsed") == 1,
                rs.getString("created_at")
        );
    }

    private void loadMembers(Connection conn, Group group) throws SQLException {
        String sql = "SELECT member_id FROM group_members WHERE group_id = ? ORDER BY member_id ASC;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, group.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) group.addMemberId(rs.getInt("member_id"));
            }
        }
    }
}
