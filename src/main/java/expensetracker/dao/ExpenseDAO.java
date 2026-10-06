package expensetracker.dao;

import expensetracker.db.Database;
import expensetracker.model.CategorySummary;
import expensetracker.model.Expense;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExpenseDAO {
    private ExpenseDAO() { }

    // ---------------- CRUD ----------------
    public static void add(Expense e) throws SQLException {
        String sql = "INSERT INTO expenses(user_id, category_id, amount, description, expense_date) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, e.userId());
            ps.setInt(2, e.categoryId());
            ps.setDouble(3, e.amount());
            ps.setString(4, e.description());
            ps.setString(5, e.date().toString());
            ps.executeUpdate();
        }
    }

    public static void update(Expense e) throws SQLException {
        String sql = "UPDATE expenses SET category_id=?, amount=?, description=?, expense_date=? WHERE id=? AND user_id=?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, e.categoryId());
            ps.setDouble(2, e.amount());
            ps.setString(3, e.description());
            ps.setString(4, e.date().toString());
            ps.setInt(5, e.id());
            ps.setInt(6, e.userId());
            ps.executeUpdate();
        }
    }

    public static void delete(int id, int userId) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM expenses WHERE id=? AND user_id=?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Date-filtered list (inclusive). categoryId may be null for all categories. */
    public static List<Expense> search(int userId, LocalDate from, LocalDate to, Integer categoryId) throws SQLException {
        String sql = "SELECT e.id, e.user_id, e.category_id, c.name, e.amount, e.description, e.expense_date "
                + "FROM expenses e JOIN categories c ON c.id = e.category_id "
                + "WHERE e.user_id = ? AND e.expense_date BETWEEN ? AND ? "
                + (categoryId != null ? "AND e.category_id = ? " : "")
                + "ORDER BY e.expense_date DESC, e.id DESC";
        List<Expense> out = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, from.toString());
            ps.setString(3, to.toString());
            if (categoryId != null) ps.setInt(4, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Expense(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4),
                            rs.getDouble(5), rs.getString(6), LocalDate.parse(rs.getString(7))));
                }
            }
        }
        return out;
    }

    // ---------------- aggregation queries ----------------

    /** Category-wise breakdown: total spent per category in a date range, alongside its monthly limit. */
    public static List<CategorySummary> categoryBreakdown(int userId, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT c.name, c.monthly_limit, COALESCE(SUM(e.amount), 0) AS spent "
                + "FROM categories c LEFT JOIN expenses e "
                + "  ON e.category_id = c.id AND e.expense_date BETWEEN ? AND ? "
                + "WHERE c.user_id = ? GROUP BY c.id ORDER BY spent DESC, c.name";
        List<CategorySummary> out = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new CategorySummary(rs.getString(1), rs.getDouble(2), rs.getDouble(3)));
            }
        }
        return out;
    }

    /** Monthly totals for the most recent N months that have data (newest first). */
    public static Map<String, Double> monthlyTotals(int userId, int months) throws SQLException {
        String sql = "SELECT substr(expense_date, 1, 7) AS month, SUM(amount) FROM expenses "
                + "WHERE user_id = ? GROUP BY month ORDER BY month DESC LIMIT ?";
        Map<String, Double> out = new LinkedHashMap<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getDouble(2));
            }
        }
        return out;
    }

    /** Spending limit check: how much one category has spent in a given month. */
    public static double spentInMonth(int userId, int categoryId, YearMonth month) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses "
                + "WHERE user_id = ? AND category_id = ? AND expense_date BETWEEN ? AND ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            ps.setString(3, month.atDay(1).toString());
            ps.setString(4, month.atEndOfMonth().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }
}
