package attendance.service;

import attendance.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private DatabaseService databaseService;
    
    public UserService() {
        databaseService = new DatabaseService();
    }
    
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    
 // Add this method to UserService.java (check if you already have it):
    public List<User> getTeachersForClassAssignment() {
        List<User> teachers = new ArrayList<>();
        String sql = """
            SELECT u.* FROM users u 
            WHERE u.role = 'TEACHER' 
            ORDER BY u.first_name, u.last_name
        """;
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                User teacher = new User();
                teacher.setId(rs.getInt("id"));
                teacher.setUsername(rs.getString("username"));
                teacher.setPassword(rs.getString("password"));
                teacher.setRole(rs.getString("role"));
                teacher.setFirstName(rs.getString("first_name"));
                teacher.setLastName(rs.getString("last_name"));
                teacher.setEmail(rs.getString("email"));
                teachers.add(teacher);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting teachers for class assignment: " + e.getMessage());
            e.printStackTrace();
        }
        
        return teachers;
    }

    
    
    // UPDATED: Added proper error handling and logging
    public int createUser(User user) {
        String sql = "INSERT INTO users (username, password, role, first_name, last_name, email) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getFirstName());
            pstmt.setString(5, user.getLastName());
            pstmt.setString(6, user.getEmail());
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    System.out.println("✅ User created with ID: " + userId + 
                                     ", Username: " + user.getUsername() + 
                                     ", Role: " + user.getRole());
                    return userId;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error creating user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    // UPDATED: Added transaction support for better reliability
    public boolean deleteUser(int userId) {
        Connection conn = null;
        
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            String sql = "DELETE FROM users WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
                    System.out.println("✅ Deleted user with ID: " + userId);
                    return true;
                } else {
                    conn.rollback();
                    System.out.println("❌ User not found with ID: " + userId);
                    return false;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error deleting user: " + e.getMessage());
            e.printStackTrace();
            
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // Get user by ID (for reset password)
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    // UPDATED: Update user including password AND username
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username = ?, password = ?, role = ?, first_name = ?, last_name = ?, email = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getFirstName());
            pstmt.setString(5, user.getLastName());
            pstmt.setString(6, user.getEmail());
            pstmt.setInt(7, user.getId());
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Updated user ID " + user.getId() + 
                             ", New username: " + user.getUsername() +
                             ", Rows affected: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Update only password (keeps username unchanged)
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Updated password for user ID " + userId + 
                             ", Rows affected: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // NEW: Update both username and password
    public boolean updateUsernameAndPassword(int userId, String newUsername, String newPassword) {
        String sql = "UPDATE users SET username = ?, password = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newUsername);
            pstmt.setString(2, newPassword);
            pstmt.setInt(3, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Updated username and password for user ID " + userId + 
                             ", New username: " + newUsername +
                             ", Rows affected: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating username and password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // NEW: Update only username (keeps password unchanged)
    public boolean updateUsername(int userId, String newUsername) {
        String sql = "UPDATE users SET username = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newUsername);
            pstmt.setInt(2, userId);
            
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Updated username for user ID " + userId + 
                             ", New username: " + newUsername +
                             ", Rows affected: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating username: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("🔍 Checking username '" + username + "' - exists: " + (count > 0));
                return count > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // NEW: Check if username exists excluding a specific user ID
    public boolean usernameExistsExcluding(String username, int excludeUserId) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ? AND id != ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setInt(2, excludeUserId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("🔍 Checking username '" + username + "' (excluding user ID " + excludeUserId + 
                                 ") - exists: " + (count > 0));
                return count > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking username (excluding): " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE email = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                users.add(user);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }
    
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user by username: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    // NEW: Get user by email
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                return user;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    // NEW: Get users by role
    public List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY username";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                users.add(user);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting users by role: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }
    
    // NEW: Get students (users with role STUDENT)
    public List<User> getStudents() {
        return getUsersByRole("STUDENT");
    }
    
    // NEW: Get teachers (users with role TEACHER)
    public List<User> getTeachers() {
        return getUsersByRole("TEACHER");
    }
    
    // NEW: Get admins (users with role ADMIN)
    public List<User> getAdmins() {
        return getUsersByRole("ADMIN");
    }
    
    // NEW: Check if user exists by ID
    public boolean userExists(int userId) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking if user exists: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // NEW: Get user count by role
    public int getUserCountByRole(String role) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE role = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting user count by role: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    // NEW: Get total user count
    public int getTotalUserCount() {
        String sql = "SELECT COUNT(*) as count FROM users";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting total user count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
}