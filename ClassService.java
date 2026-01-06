package attendance.service;

import java.sql.*;
import java.util.*;

public class ClassService {
    
    private DatabaseService databaseService;
    
    public ClassService() {
        databaseService = new DatabaseService();
    }
    
    // Get all classes with teacher names and student counts
    public List<Map<String, Object>> getAllClasses() {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = """
            SELECT DISTINCT 
                c.id, 
                c.class_name, 
                c.room,
                c.schedule,
                c.teacher_id,
                CASE 
                    WHEN u.first_name IS NULL THEN 'No Teacher Assigned'
                    ELSE u.first_name || ' ' || u.last_name 
                END as teacher_name,
                (SELECT COUNT(DISTINCT s.id) FROM students s WHERE s.class_id = c.id) as student_count
            FROM classes c
            LEFT JOIN users u ON c.teacher_id = u.id
            WHERE c.class_name IS NOT NULL AND c.class_name != ''
            ORDER BY c.class_name
        """;
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", rs.getInt("id"));
                classData.put("className", rs.getString("class_name"));
                classData.put("room", rs.getString("room"));
                classData.put("schedule", rs.getString("schedule"));
                classData.put("teacherId", rs.getInt("teacher_id"));
                
                String teacherName = rs.getString("teacher_name");
                classData.put("teacherName", teacherName != null ? teacherName : "No Teacher Assigned");
                
                classData.put("studentCount", rs.getInt("student_count"));
                classes.add(classData);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting all classes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return classes;
    }
    
    // Get all teachers for dropdown - FIXED VERSION
    public List<Map<String, Object>> getAllTeachers() {
        List<Map<String, Object>> teachers = new ArrayList<>();
        String sql = """
            SELECT DISTINCT 
                u.id, 
                u.first_name || ' ' || u.last_name as name 
            FROM users u
            LEFT JOIN teachers t ON u.id = t.user_id 
            WHERE u.role = 'TEACHER' 
            ORDER BY u.first_name, u.last_name
        """;
        
        System.out.println("🔍 DEBUG: Fetching teachers from database...");
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                Map<String, Object> teacher = new HashMap<>();
                int teacherId = rs.getInt("id");
                String teacherName = rs.getString("name");
                
                teacher.put("id", teacherId);
                teacher.put("name", teacherName);
                teachers.add(teacher);
                
                System.out.println("  Teacher #" + (++count) + ": ID=" + teacherId + ", Name=" + teacherName);
            }
            
            System.out.println("✅ Found " + count + " teachers in database");
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting teachers: " + e.getMessage());
            e.printStackTrace();
        }
        
        return teachers;
    }
    
    // Add a new class
    public boolean addClass(String className, String room, String schedule, Integer teacherId) {
        // First check if class already exists
        if (classNameExists(className)) {
            System.err.println("❌ Class '" + className + "' already exists");
            return false;
        }
        
        String sql = "INSERT INTO classes (class_name, room, schedule, teacher_id) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            pstmt.setString(2, room);
            pstmt.setString(3, schedule);
            
            if (teacherId != null && teacherId > 0) {
                pstmt.setInt(4, teacherId);
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            int affectedRows = pstmt.executeUpdate();
            System.out.println("✅ Added class '" + className + "', affected rows: " + affectedRows);
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error adding class: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Update an existing class - FIXED VERSION
    public boolean updateClass(int classId, String className, String room, String schedule, Integer teacherId) {
        System.out.println("\n=== updateClass() called ===");
        System.out.println("Parameters:");
        System.out.println("  classId: " + classId);
        System.out.println("  className: '" + className + "'");
        System.out.println("  room: '" + room + "'");
        System.out.println("  schedule: '" + schedule + "'");
        System.out.println("  teacherId: " + teacherId);
        
        // Get current class data
        Map<String, Object> currentClass = getClassById(classId);
        if (currentClass == null) {
            System.err.println("❌ Class ID " + classId + " not found");
            return false;
        }
        
        // Get current values
        String currentClassName = (String) currentClass.get("className");
        String currentRoom = (String) currentClass.get("room");
        String currentSchedule = (String) currentClass.get("schedule");
        Integer currentTeacherId = (Integer) currentClass.get("teacherId");
        
        // Normalize values to handle nulls
        if (currentRoom == null) currentRoom = "";
        if (currentSchedule == null) currentSchedule = "";
        if (teacherId == null) teacherId = 0;
        if (currentTeacherId == null) currentTeacherId = 0;
        
        System.out.println("Current class data:");
        System.out.println("  currentClassName: '" + currentClassName + "'");
        System.out.println("  currentRoom: '" + currentRoom + "'");
        System.out.println("  currentSchedule: '" + currentSchedule + "'");
        System.out.println("  currentTeacherId: " + currentTeacherId);
        
        // Check if we're actually changing anything
        boolean hasChanges = false;
        
        if (!className.equals(currentClassName)) {
            System.out.println("✓ Class name changed: '" + currentClassName + "' -> '" + className + "'");
            hasChanges = true;
        }
        if (!room.equals(currentRoom)) {
            System.out.println("✓ Room changed: '" + currentRoom + "' -> '" + room + "'");
            hasChanges = true;
        }
        if (!schedule.equals(currentSchedule)) {
            System.out.println("✓ Schedule changed: '" + currentSchedule + "' -> '" + schedule + "'");
            hasChanges = true;
        }
        if (!teacherId.equals(currentTeacherId)) {
            System.out.println("✓ Teacher ID changed: " + currentTeacherId + " -> " + teacherId);
            hasChanges = true;
        }
        
        // If nothing changed, return false - don't update!
        if (!hasChanges) {
            System.out.println("❌ No changes detected for class ID " + classId + ", skipping update");
            return false;
        }
        
        // Check for duplicate class name (excluding current class)
        if (!className.equals(currentClassName) && classNameExistsExcluding(className, classId)) {
            System.err.println("❌ Class name '" + className + "' already exists (excluding class ID " + classId + ")");
            return false;
        }
        
        String sql = "UPDATE classes SET class_name = ?, room = ?, schedule = ?, teacher_id = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            pstmt.setString(2, room);
            pstmt.setString(3, schedule);
            
            if (teacherId != null && teacherId > 0) {
                pstmt.setInt(4, teacherId);
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            
            pstmt.setInt(5, classId);
            
            int affectedRows = pstmt.executeUpdate();
            System.out.println("✅ Updated class ID " + classId + " to '" + className + "', affected rows: " + affectedRows);
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating class: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Delete a class
    public boolean deleteClass(int classId) {
        Connection conn = null;
        
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            // First, unassign students from this class
            String unassignStudentsSql = "UPDATE students SET class_id = NULL WHERE class_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(unassignStudentsSql)) {
                pstmt.setInt(1, classId);
                int unassigned = pstmt.executeUpdate();
                System.out.println("ℹ️ Unassigned " + unassigned + " students from class ID " + classId);
            }
            
            // Delete the class
            String deleteClassSql = "DELETE FROM classes WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteClassSql)) {
                pstmt.setInt(1, classId);
                int affectedRows = pstmt.executeUpdate();
                
                conn.commit();
                System.out.println("✅ Deleted class ID " + classId + ", affected rows: " + affectedRows);
                return affectedRows > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error deleting class: " + e.getMessage());
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
    
    // Get class by name
    public Map<String, Object> getClassByName(String className) {
        String sql = "SELECT * FROM classes WHERE class_name = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", rs.getInt("id"));
                classData.put("className", rs.getString("class_name"));
                classData.put("room", rs.getString("room"));
                classData.put("schedule", rs.getString("schedule"));
                classData.put("teacherId", rs.getInt("teacher_id"));
                return classData;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting class by name: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    // Get class by ID
    public Map<String, Object> getClassById(int classId) {
        String sql = """
            SELECT 
                c.*, 
                CASE 
                    WHEN u.first_name IS NULL THEN 'No Teacher Assigned'
                    ELSE u.first_name || ' ' || u.last_name 
                END as teacher_name
            FROM classes c
            LEFT JOIN users u ON c.teacher_id = u.id
            WHERE c.id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, classId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", rs.getInt("id"));
                classData.put("className", rs.getString("class_name"));
                classData.put("room", rs.getString("room"));
                classData.put("schedule", rs.getString("schedule"));
                classData.put("teacherId", rs.getInt("teacher_id"));
                
                String teacherName = rs.getString("teacher_name");
                classData.put("teacherName", teacherName != null ? teacherName : "No Teacher Assigned");
                
                return classData;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting class by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    // Get all classes for dropdown/combobox
    public List<Map<String, Object>> getClassesForDropdown() {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = "SELECT DISTINCT id, class_name FROM classes WHERE class_name IS NOT NULL ORDER BY class_name";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", rs.getInt("id"));
                classData.put("name", rs.getString("class_name"));
                classes.add(classData);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting classes for dropdown: " + e.getMessage());
            e.printStackTrace();
        }
        
        return classes;
    }
    
    // Get classes assigned to a teacher
    public List<Map<String, Object>> getClassesByTeacher(int teacherId) {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = """
            SELECT DISTINCT c.*, COUNT(s.id) as student_count
            FROM classes c
            LEFT JOIN students s ON c.id = s.class_id
            WHERE c.teacher_id = ?
            GROUP BY c.id, c.class_name, c.room, c.schedule
            ORDER BY c.class_name
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", rs.getInt("id"));
                classData.put("className", rs.getString("class_name"));
                classData.put("room", rs.getString("room"));
                classData.put("schedule", rs.getString("schedule"));
                classData.put("studentCount", rs.getInt("student_count"));
                classes.add(classData);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting classes by teacher: " + e.getMessage());
            e.printStackTrace();
        }
        
        return classes;
    }
    
    // Check if class name already exists
    public boolean classNameExists(String className) {
        String sql = "SELECT COUNT(*) as count FROM classes WHERE LOWER(TRIM(class_name)) = LOWER(TRIM(?))";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("ℹ️ Class name '" + className + "' exists " + count + " times");
                return count > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error checking class name: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // Check if class name exists excluding a specific class
    public boolean classNameExistsExcluding(String className, int excludeClassId) {
        String sql = "SELECT COUNT(*) as count FROM classes WHERE LOWER(TRIM(class_name)) = LOWER(TRIM(?)) AND id != ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, className);
            pstmt.setInt(2, excludeClassId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error checking class name (excluding): " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // Get class names for auto-completion
    public List<String> getClassNames() {
        List<String> classNames = new ArrayList<>();
        String sql = "SELECT DISTINCT class_name FROM classes WHERE class_name IS NOT NULL ORDER BY class_name";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                classNames.add(rs.getString("class_name"));
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting class names: " + e.getMessage());
            e.printStackTrace();
        }
        
        return classNames;
    }
    
    // Assign students to class
    public boolean assignStudentsToClass(List<Integer> studentIds, int classId) {
        Connection conn = null;
        
        try {
            conn = databaseService.getConnection();
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
            System.err.println("❌ Error assigning students to class: " + e.getMessage());
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
    
    // Check for duplicate classes in database
    public void checkForDuplicateClasses() {
        String sql = """
            SELECT class_name, COUNT(*) as duplicate_count, 
                   GROUP_CONCAT(id) as ids
            FROM classes 
            WHERE class_name IS NOT NULL AND class_name != ''
            GROUP BY class_name
            HAVING COUNT(*) > 1
        """;
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("🔍 Checking for duplicate classes...");
            boolean hasDuplicates = false;
            while (rs.next()) {
                hasDuplicates = true;
                System.out.println("⚠️ Duplicate found: '" + rs.getString("class_name") + 
                                 "' (" + rs.getInt("duplicate_count") + " entries) - IDs: " + rs.getString("ids"));
            }
            
            if (!hasDuplicates) {
                System.out.println("✅ No duplicate classes found");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error checking duplicates: " + e.getMessage());
        }
    }
    
    // Get a list of duplicate classes
    public List<Map<String, Object>> getDuplicateClasses() {
        List<Map<String, Object>> duplicates = new ArrayList<>();
        String sql = """
            SELECT class_name, COUNT(*) as count, MIN(id) as keep_id
            FROM classes 
            WHERE class_name IS NOT NULL
            GROUP BY class_name
            HAVING COUNT(*) > 1
        """;
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> duplicate = new HashMap<>();
                duplicate.put("className", rs.getString("class_name"));
                duplicate.put("count", rs.getInt("count"));
                duplicate.put("keepId", rs.getInt("keep_id"));
                duplicates.add(duplicate);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting duplicate classes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return duplicates;
    }
    
    // NEW: Check if class has students assigned
    public boolean hasStudents(int classId) {
        String sql = "SELECT COUNT(*) as count FROM students WHERE class_id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, classId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error checking if class has students: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // NEW: Get teacher name by ID
    public String getTeacherNameById(int teacherId) {
        if (teacherId <= 0) return "No Teacher Assigned";
        
        String sql = "SELECT first_name || ' ' || last_name as name FROM users WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error getting teacher name: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "No Teacher Assigned";
    }
}