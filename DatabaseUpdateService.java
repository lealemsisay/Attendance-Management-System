package attendance.service;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseUpdateService {
    private DatabaseService databaseService;
    
    public DatabaseUpdateService() {
        databaseService = new DatabaseService();
    }
    
    public void addDepartmentColumn() {
        System.out.println("🔄 Checking and updating database structure...");
        
        String sql = "ALTER TABLE students ADD COLUMN department VARCHAR(100)";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sql);
            System.out.println("✅ Added 'department' column to students table");
            
        } catch (Exception e) {
            if (e.getMessage().contains("duplicate column name")) {
                System.out.println("✅ Department column already exists");
            } else {
                System.err.println("❌ Error updating database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public void updateExistingStudents() {
        System.out.println("🔄 Updating existing students with default department...");
        
        String sql = "UPDATE students SET department = 'General' WHERE department IS NULL";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            
            int rowsAffected = stmt.executeUpdate(sql);
            System.out.println("✅ Updated " + rowsAffected + " students with default department");
            
        } catch (Exception e) {
            System.err.println("❌ Error updating students: " + e.getMessage());
            e.printStackTrace();
        }
    }
}