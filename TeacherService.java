package attendance.service;

import attendance.model.Teacher;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherService {
    private DatabaseService databaseService;
    
    public TeacherService() {
        databaseService = new DatabaseService();
    }
    
    // MARK: TEACHER CRUD METHODS
    
    /**
     * Get all teachers from database
     */
    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT t.*, COUNT(c.id) as classes_count " +
                     "FROM teachers t " +
                     "LEFT JOIN classes c ON t.id = c.teacher_id " +
                     "GROUP BY t.id " +
                     "ORDER BY t.last_name, t.first_name";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Teacher teacher = new Teacher();
                teacher.setId(rs.getInt("id"));
                teacher.setFirstName(rs.getString("first_name"));
                teacher.setLastName(rs.getString("last_name"));
                teacher.setEmail(rs.getString("email"));
                teacher.setDepartment(rs.getString("department"));
                teacher.setUserId(rs.getInt("user_id"));
                teacher.setClassesAssigned(rs.getInt("classes_count"));
                teachers.add(teacher);
            }
            System.out.println("✅ Retrieved " + teachers.size() + " teachers from database");
        } catch (SQLException e) {
            System.err.println("❌ Error getting all teachers: " + e.getMessage());
            e.printStackTrace();
        }
        return teachers;
    }
    
    /**
     * Add a new teacher to the system - UPDATED to set first_name and last_name in users table
     */
    public boolean addTeacher(Teacher teacher, String password) {
        // First, get the next teacher_id value
        int nextTeacherId = getNextTeacherId();
        if (nextTeacherId <= 0) {
            System.err.println("❌ Error getting next teacher ID");
            return false;
        }
        
        String insertUserSQL = "INSERT INTO users (username, password, role, first_name, last_name, created_at) VALUES (?, ?, 'TEACHER', ?, ?, datetime('now'))";
        String insertTeacherSQL = "INSERT INTO teachers (teacher_id, user_id, first_name, last_name, email, department) VALUES (?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insert into users table with first_name and last_name
            PreparedStatement userStmt = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS);
            userStmt.setString(1, teacher.getEmail());
            userStmt.setString(2, password);
            userStmt.setString(3, teacher.getFirstName());
            userStmt.setString(4, teacher.getLastName());
            
            int userRows = userStmt.executeUpdate();
            if (userRows == 0) {
                conn.rollback();
                return false;
            }
            
            ResultSet generatedKeys = userStmt.getGeneratedKeys();
            int userId = -1;
            if (generatedKeys.next()) {
                userId = generatedKeys.getInt(1);
            }
            userStmt.close();
            
            if (userId == -1) {
                conn.rollback();
                return false;
            }
            
            // 2. Insert into teachers table with teacher_id
            PreparedStatement teacherStmt = conn.prepareStatement(insertTeacherSQL);
            teacherStmt.setInt(1, nextTeacherId);
            teacherStmt.setInt(2, userId);
            teacherStmt.setString(3, teacher.getFirstName());
            teacherStmt.setString(4, teacher.getLastName());
            teacherStmt.setString(5, teacher.getEmail());
            teacherStmt.setString(6, teacher.getDepartment());
            
            int teacherRows = teacherStmt.executeUpdate();
            teacherStmt.close();
            
            if (teacherRows == 0) {
                conn.rollback();
                return false;
            }
            
            conn.commit();
            System.out.println("✅ Added new teacher: ID=" + nextTeacherId + ", Name=" + 
                             teacher.getFirstName() + " " + teacher.getLastName() + 
                             ", UserID=" + userId);
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("❌ Error adding teacher: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Get the next available teacher_id
     */
    private int getNextTeacherId() {
        String sql = "SELECT COALESCE(MAX(teacher_id), 0) + 1 as next_id FROM teachers";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("next_id");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting next teacher ID: " + e.getMessage());
        }
        return 1; // Default to 1 if table is empty
    }
    
    /**
     * Update an existing teacher - UPDATED to set first_name and last_name in users table
     */
    public boolean updateTeacher(Teacher teacher, String newPassword) {
        String updateTeacherSQL = "UPDATE teachers SET first_name = ?, last_name = ?, email = ?, department = ? WHERE id = ?";
        String updateUserSQL = "UPDATE users SET username = ?, first_name = ?, last_name = ? WHERE id = (SELECT user_id FROM teachers WHERE id = ?)";
        String updatePasswordSQL = "UPDATE users SET password = ? WHERE id = (SELECT user_id FROM teachers WHERE id = ?)";
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Update teacher information
            PreparedStatement teacherStmt = conn.prepareStatement(updateTeacherSQL);
            teacherStmt.setString(1, teacher.getFirstName());
            teacherStmt.setString(2, teacher.getLastName());
            teacherStmt.setString(3, teacher.getEmail());
            teacherStmt.setString(4, teacher.getDepartment());
            teacherStmt.setInt(5, teacher.getId());
            
            int teacherRows = teacherStmt.executeUpdate();
            teacherStmt.close();
            
            if (teacherRows == 0) {
                conn.rollback();
                return false;
            }
            
            // 2. Update user information (username and name)
            PreparedStatement userStmt = conn.prepareStatement(updateUserSQL);
            userStmt.setString(1, teacher.getEmail());
            userStmt.setString(2, teacher.getFirstName());
            userStmt.setString(3, teacher.getLastName());
            userStmt.setInt(4, teacher.getId());
            userStmt.executeUpdate();
            userStmt.close();
            
            // 3. Update password if provided
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                PreparedStatement passStmt = conn.prepareStatement(updatePasswordSQL);
                passStmt.setString(1, newPassword);
                passStmt.setInt(2, teacher.getId());
                passStmt.executeUpdate();
                passStmt.close();
            }
            
            conn.commit();
            System.out.println("✅ Updated teacher ID: " + teacher.getId());
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("❌ Error updating teacher: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Delete a teacher from the system
     * Note: Will fail if teacher is assigned to classes
     */
    public boolean deleteTeacher(int teacherId) {
        // First check if teacher is assigned to any classes
        String checkClassesSQL = "SELECT COUNT(*) as class_count FROM classes WHERE teacher_id = ?";
        String deleteTeacherSQL = "DELETE FROM teachers WHERE id = ?";
        String deleteUserSQL = "DELETE FROM users WHERE id = (SELECT user_id FROM teachers WHERE id = ?)";
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            
            // Check if teacher has classes assigned
            PreparedStatement checkStmt = conn.prepareStatement(checkClassesSQL);
            checkStmt.setInt(1, teacherId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt("class_count") > 0) {
                System.err.println("❌ Cannot delete teacher: Assigned to classes");
                return false;
            }
            checkStmt.close();
            
            // Get user_id before deleting teacher
            String getUserIdSQL = "SELECT user_id FROM teachers WHERE id = ?";
            PreparedStatement getUserIdStmt = conn.prepareStatement(getUserIdSQL);
            getUserIdStmt.setInt(1, teacherId);
            ResultSet userIdRs = getUserIdStmt.executeQuery();
            int userId = -1;
            if (userIdRs.next()) {
                userId = userIdRs.getInt("user_id");
            }
            getUserIdStmt.close();
            
            if (userId == -1) {
                System.err.println("❌ Teacher not found");
                return false;
            }
            
            // Start transaction for deletion
            conn.setAutoCommit(false);
            
            // Delete teacher record
            PreparedStatement deleteTeacherStmt = conn.prepareStatement(deleteTeacherSQL);
            deleteTeacherStmt.setInt(1, teacherId);
            int teacherRows = deleteTeacherStmt.executeUpdate();
            deleteTeacherStmt.close();
            
            if (teacherRows == 0) {
                conn.rollback();
                return false;
            }
            
            // Delete user record
            PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSQL);
            deleteUserStmt.setInt(1, teacherId);
            int userRows = deleteUserStmt.executeUpdate();
            deleteUserStmt.close();
            
            if (userRows == 0) {
                conn.rollback();
                System.err.println("❌ User record not found");
                return false;
            }
            
            conn.commit();
            System.out.println("✅ Deleted teacher ID: " + teacherId);
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("❌ Error deleting teacher: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public Teacher getTeacherById(int id) {
        String sql = "SELECT t.*, COUNT(c.id) as classes_count " +
                     "FROM teachers t " +
                     "LEFT JOIN classes c ON t.id = c.teacher_id " +
                     "WHERE t.id = ? " +
                     "GROUP BY t.id";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Teacher teacher = new Teacher();
                teacher.setId(rs.getInt("id"));
                teacher.setFirstName(rs.getString("first_name"));
                teacher.setLastName(rs.getString("last_name"));
                teacher.setEmail(rs.getString("email"));
                teacher.setDepartment(rs.getString("department"));
                teacher.setUserId(rs.getInt("user_id"));
                teacher.setClassesAssigned(rs.getInt("classes_count"));
                return teacher;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting teacher by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // MARK: EXISTING METHODS (keep these as they are)
    
    public int getAssignedClassCount(int teacherId) {
        String sql = "SELECT COUNT(*) as count FROM classes WHERE teacher_id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting class count: " + e.getMessage());
        }
        return 0;
    }
    
    public int getStudentCount(int teacherId) {
        String sql = "SELECT COUNT(DISTINCT s.id) as count "
                   + "FROM students s "
                   + "JOIN classes c ON s.class_name = c.class_name "
                   + "WHERE c.teacher_id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting student count: " + e.getMessage());
        }
        return 0;
    }
    
    public double getAverageAttendanceRate(int teacherId) {
        String sql = "SELECT AVG(CASE WHEN a.status = 'PRESENT' THEN 100 "
                   + "WHEN a.status = 'LATE' THEN 80 "
                   + "ELSE 0 END) as avg_rate "
                   + "FROM attendance a "
                   + "JOIN students s ON a.student_id = s.id "
                   + "JOIN classes c ON s.class_name = c.class_name "
                   + "WHERE c.teacher_id = ? AND date(a.date) = date('now')";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("avg_rate");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting attendance rate: " + e.getMessage());
        }
        return 0.0;
    }
    
    public List<Map<String, Object>> getTeacherClasses(int teacherId) {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = "SELECT id, class_name, room_number, schedule, student_count FROM classes WHERE teacher_id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", rs.getInt("id"));
                classData.put("className", rs.getString("class_name"));
                classData.put("roomNumber", rs.getString("room_number"));
                classData.put("schedule", rs.getString("schedule"));
                classData.put("studentCount", rs.getInt("student_count"));
                classes.add(classData);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting teacher classes: " + e.getMessage());
        }
        return classes;
    }
    
    public List<Map<String, Object>> getStudentsInClass(String className) {
        List<Map<String, Object>> students = new ArrayList<>();
        String sql = "SELECT id, student_id, first_name, last_name, email FROM students WHERE class_name = ? ORDER BY last_name, first_name";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("id", rs.getInt("id"));
                studentData.put("studentId", rs.getString("student_id"));
                studentData.put("firstName", rs.getString("first_name"));
                studentData.put("lastName", rs.getString("last_name"));
                studentData.put("email", rs.getString("email"));
                students.add(studentData);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting students in class: " + e.getMessage());
        }
        return students;
    }
    
    public boolean markAttendance(int studentId, String date, String status, String remarks) {
        String checkSql = "SELECT id FROM attendance WHERE student_id = ? AND date = ?";
        String insertSql = "INSERT INTO attendance (student_id, date, status, remarks) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE attendance SET status = ?, remarks = ? WHERE student_id = ? AND date = ?";
        
        try (Connection conn = databaseService.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, studentId);
            checkStmt.setString(2, date);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, status);
                updateStmt.setString(2, remarks);
                updateStmt.setInt(3, studentId);
                updateStmt.setString(4, date);
                
                int rowsUpdated = updateStmt.executeUpdate();
                updateStmt.close();
                return rowsUpdated > 0;
            } else {
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, studentId);
                insertStmt.setString(2, date);
                insertStmt.setString(3, status);
                insertStmt.setString(4, remarks);
                
                int rowsInserted = insertStmt.executeUpdate();
                insertStmt.close();
                return rowsInserted > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error marking attendance: " + e.getMessage());
            return false;
        }
    }
    
    public List<Map<String, Object>> getClassAttendance(String className, String date) {
        List<Map<String, Object>> attendance = new ArrayList<>();
        String sql = "SELECT s.student_id, s.first_name, s.last_name, a.status, a.remarks "
                   + "FROM students s "
                   + "LEFT JOIN attendance a ON s.id = a.student_id AND a.date = ? "
                   + "WHERE s.class_name = ? "
                   + "ORDER BY s.last_name, s.first_name";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, date);
            pstmt.setString(2, className);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("studentId", rs.getString("student_id"));
                record.put("firstName", rs.getString("first_name"));
                record.put("lastName", rs.getString("last_name"));
                record.put("status", rs.getString("status"));
                record.put("remarks", rs.getString("remarks"));
                attendance.add(record);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting class attendance: " + e.getMessage());
        }
        return attendance;
    }
    
    public Map<String, Object> getAttendanceStatistics(int teacherId, String startDate, String endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        String sql = "SELECT "
                   + "COUNT(DISTINCT a.id) as total_records, "
                   + "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_count, "
                   + "SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) as absent_count, "
                   + "SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) as late_count "
                   + "FROM attendance a "
                   + "JOIN students s ON a.student_id = s.id "
                   + "JOIN classes c ON s.class_name = c.class_name "
                   + "WHERE c.teacher_id = ? AND a.date BETWEEN ? AND ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherId);
            pstmt.setString(2, startDate);
            pstmt.setString(3, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                stats.put("totalRecords", rs.getInt("total_records"));
                stats.put("presentCount", rs.getInt("present_count"));
                stats.put("absentCount", rs.getInt("absent_count"));
                stats.put("lateCount", rs.getInt("late_count"));
                
                int total = rs.getInt("total_records");
                if (total > 0) {
                    stats.put("presentRate", (rs.getInt("present_count") * 100.0) / total);
                    stats.put("absentRate", (rs.getInt("absent_count") * 100.0) / total);
                    stats.put("lateRate", (rs.getInt("late_count") * 100.0) / total);
                } else {
                    stats.put("presentRate", 0.0);
                    stats.put("absentRate", 0.0);
                    stats.put("lateRate", 0.0);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting attendance statistics: " + e.getMessage());
        }
        return stats;
    }
    
    /**
     * NEW: Get teacher ID from user ID for class assignment
     */
  
    public Integer getTeacherIdFromUserId(int userId) {
        String sql = "SELECT id FROM teachers WHERE user_id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting teacher ID from user ID: " + e.getMessage());
        }
        return null;
    }
}