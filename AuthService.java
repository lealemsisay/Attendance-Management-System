package attendance.service;

import attendance.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    private DatabaseService databaseService;
    
    public AuthService() {
        databaseService = new DatabaseService();
    }
    
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        System.out.println("Authenticating: " + username + "/" + password);
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setEmail(rs.getString("email"));
                System.out.println("Login successful for: " + user.getFirstName());
                return user;
            } else {
                System.out.println("No user found with these credentials");
            }
            
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
}