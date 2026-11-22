import java.sql.*;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String URL =
            "jdbc:mysql://localhost:3306/securelink_db";

    private static final String USER = "securelink_user";

    private static final String PASSWORD =
            System.getenv("SECURELINK_DB_PASSWORD");

    private static final int ITERATIONS = 210000;

    // Establish database connection
    public static Connection getConnection() throws SQLException {

        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("Database password not configured");
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Generate a secure password hash
    private static String hashPassword(String password)
            throws Exception {

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, ITERATIONS, 256
        );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] hash = factory.generateSecret(spec).getEncoded();
        spec.clearPassword();

        return ITERATIONS + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    // Verify password against stored hash
    private static boolean verifyPassword(
            String password, String storedHash) throws Exception {

        String[] parts = storedHash.split(":");

        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, iterations, 256
        );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] actualHash = factory.generateSecret(spec).getEncoded();
        spec.clearPassword();

        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    // Register a new employee
    public static boolean registerUser(
            String username, String password) {

        if (username == null ||
                !username.matches("[a-zA-Z0-9_]{3,20}") ||
                password == null || password.length() < 8) {
            return false;
        }

        String sql = "INSERT INTO users "
                + "(username, password_hash, role) "
                + "VALUES (?, ?, 'EMPLOYEE')";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashPassword(password));

            return ps.executeUpdate() == 1;

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Username already exists.");
            return false;

        } catch (Exception e) {
            System.out.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    // Authenticate an existing user and return their role
    public static String authenticateUser(
            String username, String password) {

        String sql = "SELECT password_hash, role, is_active "
                + "FROM users WHERE username = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next() && rs.getBoolean("is_active")) {

                    String storedHash = rs.getString("password_hash");

                    if (verifyPassword(password, storedHash)) {
                        return rs.getString("role");
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
        }

        return null;
    }

    // Test database connection
    public static void testConnection() {

        try (Connection con = getConnection()) {
            System.out.println(
                    "SecureLink database connected successfully!"
            );
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public static void recordConnection(String username, String ipAddress) {

        String sql = """
            INSERT INTO connections (user_id, ip_address)
            SELECT user_id, ? FROM users WHERE username = ?
            """;

        try (Connection conn = getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ipAddress);
            stmt.setString(2, username);
            stmt.executeUpdate();

            System.out.println("Connection recorded for " + username);

        } catch (SQLException e) {
            System.out.println("Connection logging failed: " + e.getMessage());
        }
    }

    public static void closeConnection(String username) {

        String sql = """
            UPDATE connections c
            JOIN users u ON c.user_id = u.user_id
            SET c.disconnected_at = CURRENT_TIMESTAMP,
                c.status = 'DISCONNECTED'
            WHERE u.username = ?
              AND c.status = 'CONNECTED'
            """;

        try (Connection conn = getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.executeUpdate();

            System.out.println("Disconnection recorded for " + username);

        } catch (SQLException e) {
            System.out.println("Disconnection logging failed: " + e.getMessage());
        }
    }

    public static void logMessage(
            String sender,
            String receiver,
            String encryptedPayload) {

        String sql = """
            INSERT INTO network_logs
            (sender_id, receiver_id, event_type,
             encryption_algorithm, encrypted_payload, status)
            VALUES (
                (SELECT user_id FROM users WHERE username = ?),
                (SELECT user_id FROM users WHERE username = ?),
                'MESSAGE',
                'AES-GCM',
                ?,
                'FORWARDED'
            )
            """;

        try (Connection conn = getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, encryptedPayload);
            stmt.executeUpdate();

            System.out.println("Message activity saved to database.");

        } catch (SQLException e) {
            System.out.println("Message logging failed: " + e.getMessage());
        }
    }

    public static List<String[]> getAllUsers() {

        List<String[]> users = new ArrayList<>();

        String sql = """
            SELECT username, role, is_active
            FROM users
            ORDER BY username
            """;

        try (Connection conn = getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(new String[]{
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getBoolean("is_active") ? "Active" : "Inactive"
                });
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve users: " + e.getMessage());
        }

        return users;
    }

    public static List<String[]> getRecentLogs() {

        List<String[]> logs = new ArrayList<>();

        String sql = """
            SELECT
                COALESCE(s.username, 'Unknown') AS sender,
                COALESCE(r.username, 'Unknown') AS receiver,
                n.encryption_algorithm,
                n.status,
                n.created_at
            FROM network_logs n
            LEFT JOIN users s ON n.sender_id = s.user_id
            LEFT JOIN users r ON n.receiver_id = r.user_id
            ORDER BY n.created_at DESC
            LIMIT 50
            """;

        try (Connection conn = getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                logs.add(new String[]{
                    rs.getString("sender"),
                    rs.getString("receiver"),
                    rs.getString("encryption_algorithm"),
                    rs.getString("status"),
                    rs.getString("created_at")
                });
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve logs: " + e.getMessage());
        }

        return logs;
    }


}
