package attendance.service;

import attendance.model.Student;
import attendance.model.Attendance;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentService {
    private DatabaseService databaseService;
    
    public StudentService() {
        databaseService = new DatabaseService();
    }
    
    public Student getStudentByUserId(int userId) {
        System.out.println("Searching for student with user_id: " + userId);
        
        String sql = "SELECT s.* FROM students s WHERE s.user_id = ?";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setStudentId(rs.getString("student_id"));
                student.setFirstName(rs.getString("first_name"));
                student.setMiddleName(rs.getString("middle_name"));
                student.setLastName(rs.getString("last_name"));
                student.setEmail(rs.getString("email"));
                student.setClassName(rs.getString("class_name"));
                student.setDepartment(rs.getString("department"));
                student.setUserId(rs.getInt("user_id"));
                student.setClassId(rs.getInt("class_id")); // ADDED: classId
                System.out.println("Found student: " + student.getFullName());
                return student;
            } else {
                System.out.println("No student found for user_id: " + userId);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting student: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean assignStudentToClass(int studentId, Integer classId) {
        String sql = "UPDATE students SET class_id = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (classId != null && classId > 0) {
                pstmt.setInt(1, classId);
            } else {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            }
            pstmt.setInt(2, studentId);
            
            int affectedRows = pstmt.executeUpdate();
            System.out.println("✅ Assigned student ID " + studentId + " to class ID " + classId);
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error assigning student to class: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean assignMultipleStudentsToClass(List<Integer> studentIds, int classId) {
        Connection conn = null;
        
        try {
            conn = databaseService.getConnectionSafe();
            conn.setAutoCommit(false);
            
            String sql = "UPDATE students SET class_id = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int studentId : studentIds) {
                    pstmt.setInt(1, classId);
                    pstmt.setInt(2, studentId);
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                
                int successCount = 0;
                for (int result : results) {
                    if (result > 0) successCount++;
                }
                
                System.out.println("✅ Assigned " + successCount + " students to class ID " + classId);
                return successCount > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error assigning multiple students to class: " + e.getMessage());
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
    
    public List<Student> getStudentsByClassId(int classId) {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE class_id = ? ORDER BY student_id";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, classId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setStudentId(rs.getString("student_id"));
                student.setFirstName(rs.getString("first_name"));
                student.setMiddleName(rs.getString("middle_name"));
                student.setLastName(rs.getString("last_name"));
                student.setEmail(rs.getString("email"));
                student.setClassName(rs.getString("class_name"));
                student.setDepartment(rs.getString("department"));
                student.setUserId(rs.getInt("user_id"));
                student.setClassId(rs.getInt("class_id"));
                students.add(student);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting students by class: " + e.getMessage());
            e.printStackTrace();
        }
        
        return students;
    }
    
    public List<Student> getStudentsByClassName(String className) {
        List<Student> students = new ArrayList<>();
        String sql = """
            SELECT s.* 
            FROM students s 
            JOIN classes c ON s.class_id = c.id 
            WHERE c.class_name = ? 
            ORDER BY s.student_id
        """;
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setStudentId(rs.getString("student_id"));
                student.setFirstName(rs.getString("first_name"));
                student.setMiddleName(rs.getString("middle_name"));
                student.setLastName(rs.getString("last_name"));
                student.setEmail(rs.getString("email"));
                student.setClassName(rs.getString("class_name"));
                student.setDepartment(rs.getString("department"));
                student.setUserId(rs.getInt("user_id"));
                student.setClassId(rs.getInt("class_id"));
                students.add(student);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting students by class name: " + e.getMessage());
            e.printStackTrace();
        }
        
        return students;
    }
    
    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET student_id = ?, first_name = ?, middle_name = ?, last_name = ?, " +
                    "email = ?, class_name = ?, department = ?, class_id = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, student.getStudentId());
            pstmt.setString(2, student.getFirstName());
            pstmt.setString(3, student.getMiddleName());
            pstmt.setString(4, student.getLastName());
            pstmt.setString(5, student.getEmail());
            pstmt.setString(6, student.getClassName());
            pstmt.setString(7, student.getDepartment());
            
            if (student.getClassId() > 0) {
                pstmt.setInt(8, student.getClassId());
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }
            
            pstmt.setInt(9, student.getId());
            
            int result = pstmt.executeUpdate();
            System.out.println("Updated student ID " + student.getId() + 
                             ", Class ID: " + student.getClassId() +
                             ", Rows affected: " + result);
            
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteStudent(int studentId) {
        System.out.println("🚨 DELETE OPERATION STARTED for student ID: " + studentId);
        
        Connection conn = null;
        
        try {
            conn = databaseService.getConnectionSafe();
            conn.setAutoCommit(false);
            
            int userId = -1;
            String getUserIdSql = "SELECT user_id FROM students WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(getUserIdSql)) {
                pstmt.setInt(1, studentId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                    System.out.println("👤 Found associated user_id: " + userId);
                } else {
                    System.out.println("❌ Student not found with ID: " + studentId);
                    conn.rollback();
                    return false;
                }
            }
            
            // Delete attendance records first (due to foreign key constraint)
            String deleteAttendanceSql = "DELETE FROM attendance WHERE student_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteAttendanceSql)) {
                pstmt.setInt(1, studentId);
                int attendanceDeleted = pstmt.executeUpdate();
                System.out.println("🗑️ Deleted " + attendanceDeleted + " attendance records");
            }
            
            // Delete the student
            String deleteStudentSql = "DELETE FROM students WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteStudentSql)) {
                pstmt.setInt(1, studentId);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    System.out.println("✅ Student record deleted successfully");
                    
                    // Delete the user account if it exists
                    if (userId > 0) {
                        String deleteUserSql = "DELETE FROM users WHERE id = ? AND role = 'STUDENT'";
                        try (PreparedStatement userStmt = conn.prepareStatement(deleteUserSql)) {
                            userStmt.setInt(1, userId);
                            int userDeleted = userStmt.executeUpdate();
                            System.out.println("👤 User account deleted: " + userDeleted + " rows affected");
                        }
                    }
                    
                    conn.commit();
                    System.out.println("✅ Transaction committed successfully");
                    return true;
                } else {
                    System.out.println("❌ Student not found or could not be deleted");
                    conn.rollback();
                    return false;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ SQL Error deleting student: " + e.getMessage());
            e.printStackTrace();
            
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("❌ Transaction rolled back due to error");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public Student getStudentById(int id) {
        String sql = "SELECT * FROM students WHERE id = ?";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setStudentId(rs.getString("student_id"));
                student.setFirstName(rs.getString("first_name"));
                student.setMiddleName(rs.getString("middle_name"));
                student.setLastName(rs.getString("last_name"));
                student.setEmail(rs.getString("email"));
                student.setClassName(rs.getString("class_name"));
                student.setDepartment(rs.getString("department"));
                student.setUserId(rs.getInt("user_id"));
                student.setClassId(rs.getInt("class_id")); // ADDED: classId
                return student;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting student by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean studentIdExists(String studentId) {
        String sql = "SELECT COUNT(*) as count FROM students WHERE student_id = ?";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking student ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean studentIdExistsExcluding(String studentId, int excludeStudentId) {
        String sql = "SELECT COUNT(*) as count FROM students WHERE student_id = ? AND id != ?";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, studentId);
            pstmt.setInt(2, excludeStudentId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking student ID (excluding): " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) as count FROM students WHERE email = ?";
        
        try (Connection conn = databaseService.getConnectionSafe();
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
    
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY student_id";
        
        try (Connection conn = databaseService.getConnectionSafe();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setStudentId(rs.getString("student_id"));
                student.setFirstName(rs.getString("first_name"));
                student.setMiddleName(rs.getString("middle_name"));
                student.setLastName(rs.getString("last_name"));
                student.setEmail(rs.getString("email"));
                student.setClassName(rs.getString("class_name"));
                student.setDepartment(rs.getString("department"));
                student.setUserId(rs.getInt("user_id"));
                student.setClassId(rs.getInt("class_id"));
                students.add(student);
            }
            
            System.out.println("✅ Retrieved " + students.size() + " students from database");
            
        } catch (SQLException e) {
            System.err.println("Error getting all students: " + e.getMessage());
            e.printStackTrace();
        }
        
        return students;
    }

    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (student_id, first_name, middle_name, last_name, " +
                    "email, class_name, department, user_id, class_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, student.getStudentId());
            pstmt.setString(2, student.getFirstName());
            pstmt.setString(3, student.getMiddleName());
            pstmt.setString(4, student.getLastName());
            pstmt.setString(5, student.getEmail());
            pstmt.setString(6, student.getClassName());
            pstmt.setString(7, student.getDepartment());
            pstmt.setInt(8, student.getUserId());
            
            if (student.getClassId() > 0) {
                pstmt.setInt(9, student.getClassId());
            } else {
                pstmt.setNull(9, java.sql.Types.INTEGER);
            }
            
            int result = pstmt.executeUpdate();
            System.out.println("Added student: " + student.getFullName() + 
                             ", Student ID: " + student.getStudentId() + 
                             ", Class ID: " + student.getClassId() +
                             ", Rows affected: " + result);
            
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Attendance> getAttendanceForStudent(int studentId) {
        System.out.println("Getting attendance for student ID: " + studentId);
        List<Attendance> attendanceList = new ArrayList<>();
        
        String sql = "SELECT id, student_id, date, check_in, check_out, status, remarks " +
                    "FROM attendance WHERE student_id = ? ORDER BY date DESC";
        
        try (Connection conn = databaseService.getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Attendance attendance = new Attendance();
                attendance.setId(rs.getInt("id"));
                attendance.setStudentId(rs.getInt("student_id"));
                
                String dateStr = rs.getString("date");
                if (dateStr != null) {
                    try {
                        attendance.setDate(java.time.LocalDate.parse(dateStr));
                    } catch (Exception e) {
                        System.err.println("Error parsing date: " + dateStr);
                        attendance.setDate(java.time.LocalDate.now());
                    }
                }
                
                attendance.setStatus(rs.getString("status"));
                attendance.setRemarks(rs.getString("remarks"));
                attendanceList.add(attendance);
            }
            
            System.out.println("Found " + attendanceList.size() + " attendance records");
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance: " + e.getMessage());
            e.printStackTrace();
        }
        
        return attendanceList;
    }
}