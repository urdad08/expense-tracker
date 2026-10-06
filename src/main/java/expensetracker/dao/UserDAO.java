package expensetracker.dao;

import expensetracker.db.Database;
import expensetracker.model.User;
import expensetracker.util.Validator;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class UserDAO {
    private UserDAO() { }

    public static User register(String username, String password) throws SQLException {
        String u = Validator.username(username);
        Validator.password(password);

        try (PreparedStatement ps = Database.get().prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, u);
            if (ps.executeQuery().next()) throw new IllegalArgumentException("That username is already taken.");
        }

        String sql = "INSERT INTO users(username, password_hash) VALUES(?, ?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u);
            ps.setString(2, hash(password));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                CategoryDAO.seedDefaults(id);
                return new User(id, u);
            }
        }
    }

    public static Optional<User> login(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && verify(password, rs.getString("password_hash"))) {
                    return Optional.of(new User(rs.getInt("id"), rs.getString("username")));
                }
            }
        }
        return Optional.empty();
    }

    // ---- password hashing: PBKDF2 with a random per-user salt, stored as "salt:hash" ----
    private static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + pbkdf2(password, salt);
    }

    private static boolean verify(String password, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 2) return false;
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return pbkdf2(password, salt).equals(parts[1]);
    }

    private static String pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65_536, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        }
    }
}
