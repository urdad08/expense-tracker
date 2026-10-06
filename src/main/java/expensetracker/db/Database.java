package expensetracker.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Single shared JDBC connection to a local SQLite file; creates the schema on first use. */
public final class Database {
    private static final String URL = "jdbc:sqlite:expense_tracker.db";
    private static Connection conn;

    private Database() { }

    public static synchronized Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(URL);
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            initSchema();
        }
        return conn;
    }

    private static void initSchema() throws SQLException {
        String script;
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new SQLException("schema.sql not found on classpath");
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Could not read schema.sql", e);
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\n")) {
            if (!line.trim().startsWith("--")) cleaned.append(line).append('\n');
        }
        try (Statement st = conn.createStatement()) {
            for (String sql : cleaned.toString().split(";")) {
                if (!sql.isBlank()) st.execute(sql);
            }
        }
    }
}
