package attendance.service;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseMigrationUtility {
    
    public static void addCascadeConstraints() {
        System.out.println("🔧 Adding cascade delete constraints...");
        
        DatabaseService dbService = new DatabaseService();
        
        try (Connection conn = dbService.getConnection()) {
            
            // Since SQLite doesn't support ALTER TABLE to add/modify foreign keys,
            // we need to recreate the tables with proper constraints
            
            // 1. Recreate attendance table with CASCADE
            recreateTableWithCascade(conn, "attendance", 
                "CREATE TABLE attendance_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_id INTEGER NOT NULL," +
                "class_id INTEGER," +
                "date DATE NOT NULL," +
                "check_in TIME," +
                "check_out TIME," +
                "status TEXT DEFAULT 'ABSENT'," +
                "remarks TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE)");
            
            // 2. Recreate students table with CASCADE
            recreateTableWithCascade(conn, "students",
                "CREATE TABLE students_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student_id TEXT UNIQUE NOT NULL," +
                "first_name TEXT NOT NULL," +
                "last_name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "class_name TEXT," +
                "department TEXT," +
                "user_id INTEGER," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            
            System.out.println("✅ Cascade constraints added successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error adding cascade constraints: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void recreateTableWithCascade(Connection conn, String tableName, String createSql) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            
            // Create backup of old table
            String backupTable = tableName + "_backup";
            stmt.execute("CREATE TABLE " + backupTable + " AS SELECT * FROM " + tableName);
            System.out.println("✅ Created backup: " + backupTable);
            
            // Drop old table
            stmt.execute("DROP TABLE " + tableName);
            System.out.println("✅ Dropped old table: " + tableName);
            
            // Create new table with cascade
            stmt.execute(createSql.replace("_new", ""));
            System.out.println("✅ Created new table with cascade: " + tableName);
            
            // Copy data from backup
            String copyData = "INSERT INTO " + tableName + " SELECT * FROM " + backupTable;
            int rowsCopied = stmt.executeUpdate(copyData);
            System.out.println("✅ Copied " + rowsCopied + " rows to new table");
            
            // Drop backup
            stmt.execute("DROP TABLE " + backupTable);
            System.out.println("✅ Dropped backup table");
            
        } catch (Exception e) {
            System.err.println("❌ Error recreating table " + tableName + ": " + e.getMessage());
            throw e;
        }
    }
}