package attendance.test;

import attendance.service.DatabaseService;
import attendance.service.NotificationService;
import java.sql.*;

public class TestNotifications {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Notification System ===\n");
            
            // 1. Test database connection
            DatabaseService db = new DatabaseService();
            System.out.println("1. Database connection: ✓");
            
            // 2. Check notifications table
            try (Connection conn = db.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='notifications'"
                );
                if (rs.next()) {
                    System.out.println("2. Notifications table exists: ✓");
                } else {
                    System.out.println("2. Notifications table exists: ✗");
                }
            }
            
            // 3. Count users by role
            try (Connection conn = db.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("\n3. User Count by Role:");
                ResultSet rs = stmt.executeQuery(
                    "SELECT role, COUNT(*) as count FROM users GROUP BY role"
                );
                while (rs.next()) {
                    System.out.println("   " + rs.getString("role") + ": " + rs.getInt("count"));
                }
            }
            
            // 4. Test notification service
            NotificationService ns = new NotificationService();
            System.out.println("\n4. Notification Service initialized: ✓");
            
            // 5. Get admin ID (sender)
            int adminId = getAdminId(db);
            System.out.println("5. Admin ID: " + adminId);
            
            // 6. Send test notification
            boolean sent = ns.sendNotificationToAllStudents(
                adminId, 
                "Test System Notification", 
                "This is a system test notification."
            );
            System.out.println("6. Test notification sent: " + (sent ? "✓" : "✗"));
            
            // 7. Check notifications in database
            try (Connection conn = db.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM notifications");
                if (rs.next()) {
                    System.out.println("7. Total notifications in DB: " + rs.getInt(1));
                }
            }
            
            System.out.println("\n=== Test Complete ===");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static int getAdminId(DatabaseService db) throws SQLException {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE username = 'admin'");
            if (rs.next()) {
                return rs.getInt("id");
            }
            return 1; // Default to ID 1 if admin not found
        }
    }
}