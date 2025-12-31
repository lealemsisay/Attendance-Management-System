package src.attendance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import src.attendance.controller.MainController;
import src.attendance.model.User;
import src.attendance.service.DatabaseService;
import src.attendance.service.DatabaseUpdateService;

public class Main extends Application {
    
    private static Stage primaryStage;
    private static User currentUser;
    
    @Override
    public void start(Stage primaryStage) {
        Main.primaryStage = primaryStage;
        
        try {
            System.out.println("🚀 Starting application with MySQL...");
            
            // Initialize MySQL database
            DatabaseService dbService = new DatabaseService();
            dbService.initializeDatabase();
            
            // ✅ ADDED: Check and update database schema
            System.out.println("🔧 Checking and updating database schema...");
            DatabaseUpdateService updateService = new DatabaseUpdateService();
            updateService.checkAndUpdateSchema();
            
            // Check if database already exists (MySQL version)
            if (!dbService.tableExists("students")) {
                System.out.println("🔄 Creating fresh database...");
                dbService.initializeDatabase();
                updateService.checkAndUpdateSchema(); // Run again after fresh creation
            } else {
                System.out.println("✅ Database already exists");
                System.out.println("✅ MySQL database schema is verified");
            }
            
            System.out.println("✅ MySQL Database ready");
            
            showLoginPage();
            
        } catch (Exception e) {
            System.err.println("❌ Error starting application: " + e.getMessage());
            e.printStackTrace();
            
            // Show error dialog
            showErrorDialog("Startup Error", 
                "Failed to start application: " + e.getMessage() + 
                "\n\nCheck if:\n" +
                "1. MySQL service is running\n" +
                "2. Password is correct in DatabaseService.java\n" +
                "3. Database 'attendance_db' exists");
        }
    }
    
    private void showErrorDialog(String title, String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Could not show error dialog: " + e.getMessage());
        }
    }
    
    public static void showLoginPage() {
        try {
            System.out.println("🔐 Loading login page...");
            
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/attendance/view/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 800, 600);
            
            try {
                scene.getStylesheets().add(Main.class.getResource("/attendance/style/login.css").toExternalForm());
                System.out.println("✅ Login CSS loaded");
            } catch (NullPointerException e) {
                System.err.println("⚠️ Login CSS not found, continuing without styles");
            }
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("Attendance Management System - Login");
            primaryStage.setResizable(false);
            primaryStage.show();
            
            System.out.println("✅ Login page loaded successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading login page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // SINGLE METHOD FOR ALL ROLES
    public static void showMainDashboard(User user) {
        try {
            currentUser = user;
            System.out.println("🚀 Loading Dashboard for: " + user.getFirstName() + " (Role: " + user.getRole() + ")");
            
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/attendance/view/main.fxml"));
            Parent root = loader.load();
            
            MainController mainController = loader.getController();
            mainController.setCurrentUser(user);
            
            Scene scene = new Scene(root, 1200, 800);
            
            try {
                // Load main CSS
                scene.getStylesheets().add(Main.class.getResource("/attendance/style/main.css").toExternalForm());
                System.out.println("✅ Main CSS loaded");
                
                // Load role-specific CSS
                switch (user.getRole()) {
                    case "ADMIN":
                        scene.getStylesheets().add(Main.class.getResource("/attendance/style/admin_dashboard.css").toExternalForm());
                        System.out.println("✅ Admin Dashboard CSS loaded");
                        break;
                    case "TEACHER":
                        scene.getStylesheets().add(Main.class.getResource("/attendance/style/teacher_dashboard.css").toExternalForm());
                        System.out.println("✅ Teacher Dashboard CSS loaded");
                        break;
                    case "STUDENT":
                        scene.getStylesheets().add(Main.class.getResource("/attendance/style/student_dashboard.css").toExternalForm());
                        System.out.println("✅ Student Dashboard CSS loaded");
                        break;
                }
                
            } catch (NullPointerException e) {
                System.err.println("⚠️ CSS not found, continuing without styles");
            }
            
            primaryStage.setScene(scene);
            primaryStage.setTitle(user.getFirstName() + " " + user.getLastName() + " - Attendance System");
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            
            System.out.println("✅ Dashboard loaded successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Keep these methods for backward compatibility (they all call showMainDashboard)
    public static void showAdminDashboard(User user) {
        showMainDashboard(user);
    }
    
    public static void showTeacherDashboard(User user) {
        showMainDashboard(user);
    }
    
    public static void showStudentDashboard(User user) {
        showMainDashboard(user);
    }
    
    public static void refreshCurrentUser(User user) {
        currentUser = user;
    }
    
    public static User getCurrentUser() {
        return currentUser;
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void main(String[] args) {
        System.out.println("🎯 Attendance Management System - MySQL Version");
        System.out.println("Starting with MySQL database...");
        launch(args);
    }
}