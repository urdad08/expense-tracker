package expensetracker.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Input validation. Every method throws IllegalArgumentException with a user-friendly message. */
public final class Validator {
    private Validator() { }

    public static LocalDate parseDate(String s, String field) {
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must be a valid date in yyyy-MM-dd format.");
        }
    }

    public static YearMonth parseMonth(String s) {
        try {
            return YearMonth.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Month must be in yyyy-MM format, e.g. 2026-10.");
        }
    }

    public static double parseAmount(String s) {
        double v;
        try {
            v = Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number.");
        }
        if (!(v > 0) || v > 10_000_000) {
            throw new IllegalArgumentException("Amount must be greater than 0 and at most 10,000,000.");
        }
        return Math.round(v * 100) / 100.0;
    }

    /** Blank means "no limit" (0). */
    public static double parseLimit(String s) {
        if (s == null || s.isBlank()) return 0;
        double v;
        try {
            v = Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Limit must be a number (leave blank for no limit).");
        }
        if (!(v >= 0) || v > 100_000_000) {
            throw new IllegalArgumentException("Limit must be 0 or more.");
        }
        return Math.round(v * 100) / 100.0;
    }

    public static String description(String s) {
        String d = s == null ? "" : s.trim();
        if (d.length() > 100) throw new IllegalArgumentException("Description must be 100 characters or fewer.");
        return d;
    }

    public static String categoryName(String s) {
        String n = s == null ? "" : s.trim();
        if (n.isEmpty() || n.length() > 30) {
            throw new IllegalArgumentException("Category name must be 1 to 30 characters.");
        }
        return n;
    }

    public static String username(String s) {
        String u = s == null ? "" : s.trim();
        if (!u.matches("[A-Za-z0-9_]{3,20}")) {
            throw new IllegalArgumentException("Username must be 3-20 letters, digits or underscores.");
        }
        return u;
    }

    public static String password(String s) {
        if (s == null || s.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        return s;
    }
}
