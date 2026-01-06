package attendance.service;

import java.sql.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:attendance.db";
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    public synchronized Connection getConnectionSafe() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = 3000");
            stmt.execute("PRAGMA journal_mode = WAL");
        }
        
        return conn;
    }
    
    public void initializeDatabase() {
        System.out.println("🔄 Creating fresh database...");
        
        synchronized (DatabaseService.class) {
            try (Connection conn = getConnectionSafe(); 
                 Statement stmt = conn.createStatement()) {
                
                // First, check if tables exist and fix them if needed
                fixDatabaseSchema(conn);
                
                // Create users table
                String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT UNIQUE NOT NULL," +
                        "password TEXT NOT NULL," +
                        "role TEXT NOT NULL," +
                        "first_name TEXT," +
                        "last_name TEXT," +
                        "email TEXT UNIQUE," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
                stmt.execute(createUsersTable);
                System.out.println("✅ Users table created/verified");
                
                // Create students table WITH middle_name column
                String createStudentsTable = "CREATE TABLE IF NOT EXISTS students (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "student_id TEXT UNIQUE NOT NULL," +
                        "first_name TEXT NOT NULL," +
                        "middle_name TEXT," +
                        "last_name TEXT NOT NULL," +
                        "email TEXT UNIQUE NOT NULL," +
                        "class_name TEXT," +
                        "department TEXT," +
                        "user_id INTEGER," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
                stmt.execute(createStudentsTable);
                System.out.println("✅ Students table created with middle_name column");
                
                // Create teachers table
                String createTeachersTable = "CREATE TABLE IF NOT EXISTS teachers (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "teacher_id TEXT UNIQUE NOT NULL," +
                        "first_name TEXT NOT NULL," +
                        "last_name TEXT NOT NULL," +
                        "email TEXT UNIQUE NOT NULL," +
                        "department TEXT," +
                        "subject TEXT," +
                        "user_id INTEGER," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
                stmt.execute(createTeachersTable);
                System.out.println("✅ Teachers table created");
                
                // Create classes table
                String createClassesTable = "CREATE TABLE IF NOT EXISTS classes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "class_name TEXT NOT NULL," +
                        "subject TEXT," +
                        "teacher_id INTEGER," +
                        "schedule TEXT," +
                        "room TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
                stmt.execute(createClassesTable);
                System.out.println("✅ Classes table created");
                
                // Create attendance table
                String createAttendanceTable = "CREATE TABLE IF NOT EXISTS attendance (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "student_id INTEGER NOT NULL," +
                        "class_id INTEGER," +
                        "date DATE NOT NULL," +
                        "check_in TIME," +
                        "check_out TIME," +
                        "status TEXT DEFAULT 'ABSENT'," +
                        "remarks TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE)";
                stmt.execute(createAttendanceTable);
                System.out.println("✅ Attendance table created");
                
                // Create notifications table
                String createNotificationsTable = "CREATE TABLE IF NOT EXISTS notifications (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "title TEXT NOT NULL," +
                        "message TEXT NOT NULL," +
                        "type TEXT," +
                        "recipient_id INTEGER," +
                        "recipient_role TEXT," +
                        "is_read BOOLEAN DEFAULT FALSE," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
                stmt.execute(createNotificationsTable);
                System.out.println("✅ Notifications table created");
                
                System.out.println("✅ All tables created successfully!");
                
                // Check if database is empty before inserting default data
                if (isDatabaseEmpty(conn)) {
                    System.out.println("📦 Database is empty, inserting default data...");
                    insertDefaultUsers(conn);
                    insertSampleData(conn);
                    System.out.println("✅ Default data inserted successfully!");
                } else {
                    System.out.println("✅ Database already has data, skipping default data insertion");
                    // Still ensure we have admin user
                    ensureAdminUserExists(conn);
                }
                
            } catch (SQLException e) {
                System.err.println("❌ Error creating database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isDatabaseEmpty(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count") == 0;
            }
        }
        return true;
    }

    private void ensureAdminUserExists(Connection conn) throws SQLException {
        String checkAdmin = "SELECT COUNT(*) as count FROM users WHERE username = 'admin'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkAdmin)) {
            if (rs.next() && rs.getInt("count") == 0) {
                System.out.println("➕ Creating admin user...");
                String insertAdmin = "INSERT INTO users (username, password, role, first_name, last_name, email) " +
                                   "VALUES ('admin', 'admin123', 'ADMIN', 'System', 'Administrator', 'admin@du.edu.et')";
                stmt.executeUpdate(insertAdmin);
                System.out.println("✅ Admin user created");
            } else {
                System.out.println("✅ Admin user already exists");
            }
        }
    }
    public void fixDatabaseSchema() throws SQLException {
        System.out.println("🔧 Fixing database schema...");
        
        synchronized (DatabaseService.class) {
            try (Connection conn = getConnectionSafe()) {
                fixDatabaseSchema(conn);
                System.out.println("✅ Database schema fixed successfully");
            } catch (SQLException e) {
                System.err.println("❌ Error fixing database schema: " + e.getMessage());
                throw e;
            }
        }
    }
    
    private void fixDatabaseSchema(Connection conn) throws SQLException {
        System.out.println("🔧 Checking and fixing database schema...");
        
        try (Statement stmt = conn.createStatement()) {
            // Check if students table exists
            boolean studentsTableExists = false;
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='students'")) {
                studentsTableExists = rs.next();
            }
            
            if (studentsTableExists) {
                // Check for middle_name column
                boolean hasMiddleName = false;
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(students)")) {
                    while (rs.next()) {
                        if ("middle_name".equals(rs.getString("name"))) {
                            hasMiddleName = true;
                            break;
                        }
                    }
                }
                
                if (!hasMiddleName) {
                    System.out.println("➕ Adding middle_name column to students table...");
                    try {
                        stmt.execute("ALTER TABLE students ADD COLUMN middle_name TEXT");
                        System.out.println("✅ middle_name column added successfully");
                    } catch (SQLException e) {
                        System.err.println("❌ Error adding middle_name column: " + e.getMessage());
                        // Table might be locked or column already exists with different type
                        // Try a different approach
                        try {
                            stmt.execute("ALTER TABLE students ADD COLUMN middle_name VARCHAR(100)");
                            System.out.println("✅ middle_name column added successfully (VARCHAR)");
                        } catch (SQLException e2) {
                            System.err.println("❌ Failed to add middle_name column: " + e2.getMessage());
                            throw e2;
                        }
                    }
                } else {
                    System.out.println("✅ middle_name column already exists");
                }
                
                // Check for department column
                boolean hasDepartment = false;
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(students)")) {
                    while (rs.next()) {
                        if ("department".equals(rs.getString("name"))) {
                            hasDepartment = true;
                            break;
                        }
                    }
                }
                
                if (!hasDepartment) {
                    System.out.println("➕ Adding department column to students table...");
                    try {
                        stmt.execute("ALTER TABLE students ADD COLUMN department TEXT");
                        System.out.println("✅ Department column added successfully");
                    } catch (SQLException e) {
                        System.err.println("❌ Error adding department column: " + e.getMessage());
                        // Try a different approach
                        try {
                            stmt.execute("ALTER TABLE students ADD COLUMN department VARCHAR(100)");
                            System.out.println("✅ Department column added successfully (VARCHAR)");
                        } catch (SQLException e2) {
                            System.err.println("❌ Failed to add department column: " + e2.getMessage());
                            throw e2;
                        }
                    }
                } else {
                    System.out.println("✅ Department column already exists");
                }
            } else {
                System.out.println("⚠️ Students table doesn't exist yet, it will be created");
            }
      
        
         // In the fixDatabaseSchema(Connection conn) method, add this code:
         // AFTER checking for department column, BEFORE the closing brace:

         // Check for class_id column
         boolean hasClassId = false;
         try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(students)")) {
             while (rs.next()) {
                 if ("class_id".equals(rs.getString("name"))) {
                     hasClassId = true;
                     break;
                 }
             }
         }

         if (!hasClassId) {
             System.out.println("➕ Adding class_id column to students table...");
             try {
                 stmt.execute("ALTER TABLE students ADD COLUMN class_id INTEGER REFERENCES classes(id) ON DELETE SET NULL");
                 System.out.println("✅ class_id column added successfully");
             } catch (SQLException e) {
                 System.err.println("❌ Error adding class_id column: " + e.getMessage());
             }
         } else {
             System.out.println("✅ class_id column already exists");
         }
        
        }
   
    
    
    
    }
    
    public void resetDatabase() throws SQLException {
        System.out.println("⚠️ WARNING: Resetting database - ALL DATA WILL BE LOST!");
        System.out.println("⚠️ This action will delete ALL data including users, students, teachers, and attendance records!");
        
        synchronized (DatabaseService.class) {
            try (Connection conn = getConnectionSafe();
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("🗑️ Dropping tables...");
                stmt.execute("DROP TABLE IF EXISTS attendance");
                stmt.execute("DROP TABLE IF EXISTS notifications");
                stmt.execute("DROP TABLE IF EXISTS classes");
                stmt.execute("DROP TABLE IF EXISTS teachers");
                stmt.execute("DROP TABLE IF EXISTS students");
                stmt.execute("DROP TABLE IF EXISTS users");
                
                System.out.println("✅ All tables dropped");
                initializeDatabase();
                
                System.out.println("✅ Database reset complete with fresh tables");
                
            } catch (SQLException e) {
                System.err.println("❌ Error resetting database: " + e.getMessage());
                throw e;
            }
        }
    }
    
    public int fixMissingUserAccounts() throws SQLException {
        System.out.println("👤 Fixing missing user accounts...");
        int fixedCount = 0;
        
        synchronized (DatabaseService.class) {
            Connection conn = null;
            try {
                conn = getConnectionSafe();
                conn.setAutoCommit(false);
                
                // First ensure schema is fixed
                fixDatabaseSchema(conn);
                
                String findStudentsSql = "SELECT s.id, s.student_id, s.first_name, s.middle_name, s.last_name, s.email " +
                                       "FROM students s " +
                                       "WHERE s.user_id IS NULL OR s.user_id = 0 OR s.user_id NOT IN (SELECT id FROM users)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(findStudentsSql);
                     ResultSet rs = pstmt.executeQuery()) {
                    
                    while (rs.next()) {
                        int studentId = rs.getInt("id");
                        String studentNum = rs.getString("student_id");
                        String firstName = rs.getString("first_name");
                        String middleName = rs.getString("middle_name");
                        String lastName = rs.getString("last_name");
                        String email = rs.getString("email");
                        
                        String fullName = firstName + 
                                         (middleName != null && !middleName.isEmpty() ? " " + middleName : "") + 
                                         " " + lastName;
                        
                        System.out.println("🔍 Found student without user account: " + fullName + " (" + email + ")");
                        
                        String checkUserSql = "SELECT id FROM users WHERE email = ?";
                        int userId = 0;
                        
                        try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
                            checkStmt.setString(1, email);
                            ResultSet userRs = checkStmt.executeQuery();
                            if (userRs.next()) {
                                userId = userRs.getInt("id");
                                System.out.println("✅ User already exists for email: " + email);
                            }
                        }
                        
                        if (userId == 0) {
                            String username = (firstName.charAt(0) + lastName).toLowerCase();
                            String baseUsername = username;
                            int counter = 1;
                            while (usernameExists(conn, username)) {
                                username = baseUsername + counter;
                                counter++;
                            }
                            
                            String insertUserSql = "INSERT INTO users (username, password, role, first_name, last_name, email) " +
                                                 "VALUES (?, 'Student@123', 'STUDENT', ?, ?, ?)";
                            
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                                insertStmt.setString(1, username);
                                insertStmt.setString(2, firstName);
                                insertStmt.setString(3, lastName);
                                insertStmt.setString(4, email);
                                insertStmt.executeUpdate();
                                
                                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                                if (generatedKeys.next()) {
                                    userId = generatedKeys.getInt(1);
                                    System.out.println("✅ Created user account for: " + fullName + " (ID: " + userId + ")");
                                }
                            }
                        }
                        
                        if (userId > 0) {
                            String updateStudentSql = "UPDATE students SET user_id = ? WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateStudentSql)) {
                                updateStmt.setInt(1, userId);
                                updateStmt.setInt(2, studentId);
                                int rowsUpdated = updateStmt.executeUpdate();
                                
                                if (rowsUpdated > 0) {
                                    fixedCount++;
                                    System.out.println("✅ Linked student " + studentNum + " to user ID: " + userId);
                                }
                            }
                        }
                    }
                }
                
                conn.commit();
                System.out.println("✅ Successfully fixed " + fixedCount + " user accounts");
                
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("Error rolling back transaction: " + ex.getMessage());
                    }
                }
                throw e;
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing connection: " + e.getMessage());
                    }
                }
            }
        }
        
        return fixedCount;
    }
    
    private boolean usernameExists(Connection conn, String username) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("count") > 0;
        }
    }
    
    private void insertDefaultUsers(Connection conn) throws SQLException {
        System.out.println("🔄 Creating default users...");
        
        String insertAdmin = "INSERT OR IGNORE INTO users (username, password, role, first_name, last_name, email) " +
                           "VALUES ('admin', 'admin123', 'ADMIN', 'System', 'Administrator', 'admin@du.edu.et')";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(insertAdmin);
            System.out.println("✅ Admin user created");
        }
        
        String insertTeacher = "INSERT OR IGNORE INTO users (username, password, role, first_name, last_name, email) " +
                             "VALUES ('teacher', 'teacher123', 'TEACHER', 'John', 'Doe', 'teacher@du.edu.et')";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(insertTeacher);
            System.out.println("✅ Teacher user created");
        }
        
        String insertStudent = "INSERT OR IGNORE INTO users (username, password, role, first_name, last_name, email) " +
                             "VALUES ('student', 'student123', 'STUDENT', 'Jane', 'Smith', 'student@du.edu.et')";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(insertStudent);
            System.out.println("✅ Student user created");
        }
    }
    
    private void insertSampleData(Connection conn) throws SQLException {
        System.out.println("📚 Inserting sample data...");
        
        // First, ensure the schema is up to date
        fixDatabaseSchema(conn);
        
        int teacherId = 0;
        String getTeacherId = "SELECT id FROM users WHERE username = 'teacher'";
        try (Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(getTeacherId)) {
            if (rs.next()) {
                teacherId = rs.getInt("id");
                System.out.println("Found teacher ID for classes: " + teacherId);
            } else {
                // Create teacher user if doesn't exist
                String createTeacher = "INSERT INTO users (username, password, role, first_name, last_name, email) " +
                                     "VALUES ('teacher', 'teacher123', 'TEACHER', 'John', 'Doe', 'teacher@du.edu.et')";
                stmt.executeUpdate(createTeacher);
                System.out.println("✅ Teacher user created");
                
                // Get the new teacher ID
                try (ResultSet rs2 = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs2.next()) {
                        teacherId = rs2.getInt(1);
                    }
                }
            }
        }
        
        // Check if classes already exist
        String checkClasses = "SELECT COUNT(*) as count FROM classes";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkClasses)) {
            if (rs.next() && rs.getInt("count") == 0) {
            	System.out.println("📝 Creating sample classes WITHOUT assigning teachers by default...");
                
                String insertClasses = "INSERT INTO classes (class_name, subject, teacher_id, schedule, room) VALUES " +
                        "('Class 10A', 'Mathematics', ?, 'Mon-Wed-Fri 9:00-10:00', 'Room 101'), " +
                        "('Class 10B', 'Science', ?, 'Tue-Thu 10:00-11:00', 'Room 102'), " +
                        "('Class 11A', 'English', ?, 'Mon-Wed 11:00-12:00', 'Room 103')";
              
                stmt.executeUpdate(insertClasses);
                System.out.println("✅ Sample classes inserted (no teachers assigned by default)");
            } else {
                System.out.println("✅ Classes already exist, skipping class insertion");
            }
        }
        
        // Check if sample students already exist
        String checkSampleStudents = "SELECT COUNT(*) as count FROM students WHERE student_id IN ('STU001', 'STU002', 'STU003', 'STU004', 'STU005')";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSampleStudents)) {
            if (rs.next() && rs.getInt("count") > 0) {
                System.out.println("✅ Sample students already exist, skipping student insertion");
                return;
            }
        }
        
        // Only insert sample students if they don't exist
        System.out.println("📝 Inserting sample students...");
        
        // Check if students table has middle_name column
        boolean hasMiddleNameColumn = false;
        try (ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(students)")) {
            while (rs.next()) {
                if ("middle_name".equals(rs.getString("name"))) {
                    hasMiddleNameColumn = true;
                    break;
                }
            }
        }
        
        String[][] additionalStudents = {
            {"STU001", "Jane", "", "Smith", "student@du.edu.et", "Class 10A", "Computer Science"},
            {"STU002", "Michael", "James", "Johnson", "michael.johnson@du.edu.et", "Class 10A", "Computer Science"},
            {"STU003", "Sarah", "Marie", "Williams", "sarah.williams@du.edu.et", "Class 10B", "Physics"},
            {"STU004", "David", "Lee", "Brown", "david.brown@du.edu.et", "Class 11A", "English"},
            {"STU005", "Emily", "Rose", "Davis", "emily.davis@du.edu.et", "Class 10A", "Computer Science"}
        };
        
        for (String[] studentData : additionalStudents) {
            String email = studentData[4];
            
            // Check if user already exists for this email
            int userId = 0;
            String checkUserSql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("id");
                    System.out.println("✅ User already exists for email: " + email);
                }
            }
            
            if (userId == 0) {
                String username = (studentData[1].charAt(0) + studentData[3]).toLowerCase();
                String baseUsername = username;
                int counter = 1;
                while (usernameExists(conn, username)) {
                    username = baseUsername + counter;
                    counter++;
                }
                
                String insertUser = "INSERT INTO users (username, password, role, first_name, last_name, email) " +
                                  "VALUES (?, 'Student@123', 'STUDENT', ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, studentData[1]);
                    pstmt.setString(3, studentData[3]);
                    pstmt.setString(4, email);
                    pstmt.executeUpdate();
                    
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                        System.out.println("✅ Created user account for: " + studentData[1] + " " + studentData[3] + 
                                         " (ID: " + userId + ")");
                    }
                }
            }
            
            // Insert student record
            if (hasMiddleNameColumn) {
                String insertStudentRecord = "INSERT INTO students (student_id, first_name, middle_name, last_name, email, class_name, department, user_id) " +
                                           "SELECT ?, ?, ?, ?, ?, ?, ?, ? " +
                                           "WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = ? OR email = ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertStudentRecord)) {
                    pstmt.setString(1, studentData[0]);
                    pstmt.setString(2, studentData[1]);
                    pstmt.setString(3, studentData[2]);
                    pstmt.setString(4, studentData[3]);
                    pstmt.setString(5, studentData[4]);
                    pstmt.setString(6, studentData[5]);
                    pstmt.setString(7, studentData[6]);
                    pstmt.setInt(8, userId);
                    pstmt.setString(9, studentData[0]);
                    pstmt.setString(10, studentData[4]);
                    
                    int rowsInserted = pstmt.executeUpdate();
                    if (rowsInserted > 0) {
                        System.out.println("✅ Created student: " + studentData[1] + " " + 
                                         (studentData[2].isEmpty() ? "" : studentData[2] + " ") + 
                                         studentData[3] + " (user_id: " + userId + ")");
                    } else {
                        System.out.println("⚠️ Student already exists: " + studentData[0] + " - " + studentData[1] + " " + studentData[3]);
                    }
                }
            } else {
                // Fallback for old schema without middle_name column
                String insertStudentRecord = "INSERT INTO students (student_id, first_name, last_name, email, class_name, department, user_id) " +
                                           "SELECT ?, ?, ?, ?, ?, ?, ? " +
                                           "WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = ? OR email = ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertStudentRecord)) {
                    pstmt.setString(1, studentData[0]);
                    pstmt.setString(2, studentData[1]);
                    pstmt.setString(3, studentData[3]);  // Skip middle name
                    pstmt.setString(4, studentData[4]);
                    pstmt.setString(5, studentData[5]);
                    pstmt.setString(6, studentData[6]);
                    pstmt.setInt(7, userId);
                    pstmt.setString(8, studentData[0]);
                    pstmt.setString(9, studentData[4]);
                    
                    int rowsInserted = pstmt.executeUpdate();
                    if (rowsInserted > 0) {
                        System.out.println("✅ Created student (without middle name): " + studentData[1] + " " + 
                                         studentData[3] + " (user_id: " + userId + ")");
                    } else {
                        System.out.println("⚠️ Student already exists: " + studentData[0] + " - " + studentData[1] + " " + studentData[3]);
                    }
                }
            }
        }
        
        // Insert sample attendance for first student if they exist
        int studentId = 0;
        String getStudentId = "SELECT id FROM students WHERE student_id = 'STU001'";
        try (Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(getStudentId)) {
            if (rs.next()) {
                studentId = rs.getInt("id");
                System.out.println("Student ID for Jane Smith: " + studentId);
                
                // Check if attendance already exists for today
                String checkAttendance = "SELECT COUNT(*) as count FROM attendance WHERE student_id = ? AND date = DATE('now')";
                try (PreparedStatement pstmt = conn.prepareStatement(checkAttendance)) {
                    pstmt.setInt(1, studentId);
                    ResultSet rs2 = pstmt.executeQuery();
                    if (rs2.next() && rs2.getInt("count") == 0) {
                        String insertAttendance = "INSERT INTO attendance (student_id, date, status) VALUES " +
                                "(?, DATE('now'), 'PRESENT'), " +
                                "(?, DATE('now', '-1 day'), 'PRESENT'), " +
                                "(?, DATE('now', '-2 days'), 'ABSENT')";
                        try (PreparedStatement pstmt2 = conn.prepareStatement(insertAttendance)) {
                            pstmt2.setInt(1, studentId);
                            pstmt2.setInt(2, studentId);
                            pstmt2.setInt(3, studentId);
                            pstmt2.executeUpdate();
                            System.out.println("✅ Sample attendance records inserted");
                        }
                    } else {
                        System.out.println("✅ Attendance already exists for today");
                    }
                }
            }
        }
        
        // Insert notifications if none exist
        String checkNotifications = "SELECT COUNT(*) as count FROM notifications";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkNotifications)) {
            if (rs.next() && rs.getInt("count") == 0) {
                String insertNotifications = "INSERT INTO notifications (title, message, type, recipient_role, is_read) VALUES " +
                        "('Welcome to Attendance System', 'The system has been successfully initialized.', 'INFO', 'ALL', FALSE), " +
                        "('New Student Added', 'Jane Smith has been registered in Class 10A.', 'INFO', 'TEACHER', FALSE), " +
                        "('Attendance Reminder', 'Please mark attendance for today.', 'REMINDER', 'TEACHER', FALSE)";
                stmt.executeUpdate(insertNotifications);
                System.out.println("✅ Sample notifications inserted");
            } else {
                System.out.println("✅ Notifications already exist");
            }
        }
        
        System.out.println("✅ Sample data insertion completed!");
    }
    
    public boolean tableExists(String tableName) {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        
        try (Connection conn = getConnectionSafe();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tableName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
            
        } catch (SQLException e) {
            System.err.println("Error checking if table exists: " + e.getMessage());
            return false;
        }
    }
    
    public boolean columnExists(String tableName, String columnName) {
        String sql = "PRAGMA table_info(" + tableName + ")";
        
        try (Connection conn = getConnectionSafe();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                if (columnName.equals(rs.getString("name"))) {
                    return true;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking if column exists: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean backupDatabase(String backupPath) {
        System.out.println("💾 Creating database backup to: " + backupPath);
        
        synchronized (DatabaseService.class) {
            try {
                File sourceFile = new File("attendance.db");
                File backupFile = new File(backupPath);
                
                Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                System.out.println("✅ Database backup created successfully: " + backupPath);
                return true;
                
            } catch (IOException e) {
                System.err.println("❌ Error creating database backup: " + e.getMessage());
                return false;
            }
        }
    }
    
    public boolean testConnection() {
        try (Connection conn = getConnectionSafe()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    
 // Add this method to your existing DatabaseService.java class
    public void cleanDuplicateClasses() {
        String sql = """
            DELETE FROM classes 
            WHERE id NOT IN (
                SELECT MIN(id) 
                FROM classes 
                WHERE class_name IS NOT NULL 
                GROUP BY class_name
            )
        """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            int deleted = stmt.executeUpdate(sql);
            if (deleted > 0) {
                System.out.println("🧹 Cleaned " + deleted + " duplicate class entries");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error cleaning duplicate classes: " + e.getMessage());
        }
    }
    
    
    public void cleanDuplicateTeachers() {
        System.out.println("🧹 Cleaning up duplicate teacher assignments...");
        
        try (Connection conn = getConnectionSafe();
             Statement stmt = conn.createStatement()) {
            // Find classes with the same teacher assigned multiple times
            String findDuplicates = "SELECT teacher_id, COUNT(*) as count FROM classes WHERE teacher_id IS NOT NULL GROUP BY teacher_id HAVING COUNT(*) > 1";
            
            ResultSet rs = stmt.executeQuery(findDuplicates);
            while (rs.next()) {
                int teacherId = rs.getInt("teacher_id");
                int count = rs.getInt("count");
                System.out.println("⚠️ Teacher ID " + teacherId + " assigned to " + count + " classes");
                
                // Keep this teacher in only one class, unassign from others
                String keepOneClass = "UPDATE classes SET teacher_id = NULL WHERE teacher_id = ? AND id NOT IN (SELECT MIN(id) FROM classes WHERE teacher_id = ?)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(keepOneClass)) {
                    pstmt.setInt(1, teacherId);
                    pstmt.setInt(2, teacherId);
                    int updated = pstmt.executeUpdate();
                    System.out.println("✅ Unassigned teacher from " + updated + " duplicate classes");
                }
            }
            
            System.out.println("✅ Duplicate teacher assignments cleaned up");
            
        } catch (SQLException e) {
            System.err.println("❌ Error cleaning duplicate teachers: " + e.getMessage());
        }
    }   
    
    public void shutdown() {
        System.out.println("🔌 Database service shutting down...");
    }
}