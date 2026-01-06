package attendance.service;

import attendance.model.Notification;
import attendance.model.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private DatabaseService databaseService;
    
    public NotificationService() {
        this.databaseService = new DatabaseService();
    }
    
    // Create notification (used by ViewAttendanceController)
    public boolean createNotification(int receiverId, String message, String type) {
        return createNotification(0, receiverId, "Attendance Report", message, type, null, 0);
    }
    
    // Complete notification creation method
    public boolean createNotification(int senderId, int receiverId, String title, String message, 
                                      String type, String relatedEntityType, int relatedEntityId) {
        String sql = "INSERT INTO notifications (sender_id, receiver_id, title, message, notification_type, " +
                    "related_entity_type, related_entity_id, is_read, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, title);
            pstmt.setString(4, message);
            pstmt.setString(5, type);
            
            if (relatedEntityType != null) {
                pstmt.setString(6, relatedEntityType);
            } else {
                pstmt.setNull(6, Types.VARCHAR);
            }
            
            pstmt.setInt(7, relatedEntityId > 0 ? relatedEntityId : 0);
            pstmt.setBoolean(8, false);
            pstmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            
            int rows = pstmt.executeUpdate();
            System.out.println("📨 Notification sent to user " + receiverId + ": " + title);
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error creating notification: " + e.getMessage());
            return false;
        }
    }
    
    // Send attendance report to admin
    public boolean sendAttendanceReportToAdmin(int teacherId, String teacherName, String className, 
                                               String fromDate, String toDate, int reportId) {
        try {
            // Find admin users
            List<Integer> adminIds = new ArrayList<>();
            String adminSql = "SELECT id FROM users WHERE role = 'ADMIN'";
            
            try (Connection conn = databaseService.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(adminSql)) {
                
                while (rs.next()) {
                    adminIds.add(rs.getInt("id"));
                }
            }
            
            if (adminIds.isEmpty()) {
                System.out.println("⚠️ No admin users found");
                return false;
            }
            
            // Send notification to each admin
            for (int adminId : adminIds) {
                String title = "New Attendance Report";
                String message = String.format(
                    "Teacher %s has submitted attendance report for class '%s' (%s to %s).",
                    teacherName, className, fromDate, toDate
                );
                
                createNotification(teacherId, adminId, title, message, 
                                 "ATTENDANCE_REPORT", "REPORT", reportId);
            }
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Error sending report to admin: " + e.getMessage());
            return false;
        }
    }
    
    // Notify students about attendance
    public boolean notifyStudentsAboutAttendance(String className, String fromDate, String toDate) {
        String sql = "SELECT u.id, s.first_name, s.last_name " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.id " +
                    "WHERE s.class_name = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            ResultSet rs = pstmt.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                int studentId = rs.getInt("id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                
                String title = "Attendance Updated";
                String message = String.format(
                    "Hi %s, your attendance for class '%s' has been recorded for the period %s to %s. " +
                    "You can view your attendance report in your dashboard.",
                    firstName, className, fromDate, toDate
                );
                
                createNotification(0, studentId, title, message, 
                                 "ATTENDANCE_UPDATE", "CLASS", 0);
                count++;
            }
            
            System.out.println("✅ Notified " + count + " students in class " + className);
            return count > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error notifying students: " + e.getMessage());
            return false;
        }
    }
    
    // Get notifications for a user
    public List<Notification> getNotifications(int userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT n.*, u.username as sender_name " +
                    "FROM notifications n " +
                    "LEFT JOIN users u ON n.sender_id = u.id " +
                    "WHERE n.receiver_id = ? " +
                    "ORDER BY n.created_at DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Notification notification = new Notification();
                notification.setId(rs.getInt("id"));
                notification.setSenderId(rs.getInt("sender_id"));
                notification.setReceiverId(rs.getInt("receiver_id"));
                notification.setSenderName(rs.getString("sender_name"));
                notification.setTitle(rs.getString("title"));
                notification.setMessage(rs.getString("message"));
                notification.setType(rs.getString("notification_type"));
                notification.setRead(rs.getBoolean("is_read"));
                
                Timestamp timestamp = rs.getTimestamp("created_at");
                if (timestamp != null) {
                    notification.setTimestamp(timestamp.toLocalDateTime());
                }
                
                notifications.add(notification);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting notifications: " + e.getMessage());
        }
        
        return notifications;
    }
    
    // Mark notification as read
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error marking notification as read: " + e.getMessage());
            return false;
        }
    }
    
    // Mark all notifications as read for user
    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE receiver_id = ? AND is_read = FALSE";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int updated = pstmt.executeUpdate();
            System.out.println("✅ Marked " + updated + " notifications as read for user " + userId);
            return updated > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error marking all as read: " + e.getMessage());
            return false;
        }
    }
    
    // Get unread count
    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) as count FROM notifications WHERE receiver_id = ? AND is_read = FALSE";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting unread count: " + e.getMessage());
        }
        
        return 0;
    }
    
    // Delete notification
    public boolean deleteNotification(int notificationId) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error deleting notification: " + e.getMessage());
            return false;
        }
    }
    
    // Send attendance reminder to teacher
    public boolean sendAttendanceReminder(int teacherId, String className) {
        String title = "Attendance Reminder";
        String message = String.format("Please don't forget to mark attendance for class '%s' today.", className);
        
        return createNotification(0, teacherId, title, message, "REMINDER", "CLASS", 0);
    }
    
    // Test method
    public boolean testConnection() {
        String sql = "SELECT COUNT(*) as count FROM notifications";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return rs.next();
            
        } catch (SQLException e) {
            System.err.println("❌ NotificationService test failed: " + e.getMessage());
            return false;
        }
    }
}