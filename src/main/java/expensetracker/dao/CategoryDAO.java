package expensetracker.dao;

import expensetracker.db.Database;
import expensetracker.model.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CategoryDAO {
    private CategoryDAO() { }

    private static final String[] DEFAULTS = {"Food", "Transport", "Rent", "Entertainment", "Shopping", "Other"};

    public static void seedDefaults(int userId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO categories(user_id, name, monthly_limit) VALUES(?, ?, 0)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            for (String name : DEFAULTS) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.executeUpdate();
            }
        }
    }

    public static List<Category> list(int userId) throws SQLException {
        String sql = "SELECT id, user_id, name, monthly_limit FROM categories WHERE user_id = ? ORDER BY name";
        List<Category> out = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Category(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getDouble(4)));
                }
            }
        }
        return out;
    }

    public static void add(int userId, String name, double limit) throws SQLException {
        String sql = "INSERT INTO categories(user_id, name, monthly_limit) VALUES(?, ?, ?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setDouble(3, limit);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (String.valueOf(e.getMessage()).contains("UNIQUE")) {
                throw new IllegalArgumentException("A category named '" + name + "' already exists.");
            }
            throw e;
        }
    }

    public static void setLimit(int categoryId, int userId, double limit) throws SQLException {
        String sql = "UPDATE categories SET monthly_limit = ? WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setDouble(1, limit);
            ps.setInt(2, categoryId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public static void delete(int categoryId, int userId) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (String.valueOf(e.getMessage()).contains("FOREIGN KEY")) {
                throw new IllegalArgumentException("This category still has expenses. Delete or edit them first.");
            }
            throw e;
        }
    }
}
