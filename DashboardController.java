package attendance.controller;

import attendance.model.User;
import attendance.service.DatabaseService;
import attendance.service.StudentService;
import attendance.service.ClassService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

public class DashboardController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label attendanceTodayLabel;
    
    private User currentUser;
    private DatabaseService databaseService;
    private StudentService studentService;
    private ClassService classService;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        databaseService = new DatabaseService();
        studentService = new StudentService();
        classService = new ClassService();
        initializeDashboard();
    }
    
    private void initializeDashboard() {
        try {
            // Update welcome message
            if (currentUser != null) {
                welcomeLabel.setText("Welcome back, " + currentUser.getFirstName() + " " + currentUser.getLastName() + "!");
            }
            
            // Load statistics
            loadStatistics();
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadStatistics() {
        try {
            // Get total students count
            int totalStudents = studentService.getAllStudents().size();
            totalStudentsLabel.setText(String.valueOf(totalStudents));
            
            // Get today's attendance percentage
            double attendancePercentage = calculateTodayAttendance();
            attendanceTodayLabel.setText(String.format("%.1f%%", attendancePercentage));
            
        } catch (Exception e) {
            System.err.println("❌ Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private double calculateTodayAttendance() {
        try {
            String today = LocalDate.now().toString();
            // This is a simplified calculation - you should replace with actual attendance logic
            int totalStudents = studentService.getAllStudents().size();
            if (totalStudents == 0) return 0.0;
            
            // For demo, assume 80% attendance
            return 80.0;
            
        } catch (Exception e) {
            System.err.println("❌ Error calculating attendance: " + e.getMessage());
            return 0.0;
        }
    }
    
    @FXML
    private void handleManageStudents() {
        try {
            System.out.println("👨‍🎓 Loading Manage Students...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/manage_students.fxml"));
            Parent root = loader.load();
            
            // Pass current user to the controller if needed
            Object controller = loader.getController();
            if (controller instanceof AdminManageStudentsController) {
                ((AdminManageStudentsController) controller).setCurrentUser(currentUser);
            }
            
            Scene scene = new Scene(root, 1200, 800);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Manage Students - Attendance System");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("❌ Error loading Manage Students: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load Manage Students page.");
        }
    }
    
    @FXML
    private void handleManageClasses() {
        try {
            System.out.println("🏫 Loading Manage Classes...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/manage_classes.fxml"));
            Parent root = loader.load();
            
            // Pass current user to ManageClassesController
            ManageClassesController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            Scene scene = new Scene(root, 1200, 800);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Manage Classes - Attendance System");
            stage.centerOnScreen();
            
            System.out.println("✅ Manage Classes loaded successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading Manage Classes: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load Manage Classes page.\nError: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleManageTeachers() {
        try {
            System.out.println("👨‍🏫 Loading Manage Teachers...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/manage_teachers.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Manage Teachers - Attendance System");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("❌ Error loading Manage Teachers: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load Manage Teachers page.");
        }
    }
    
    @FXML
    private void handleViewReports() {
        try {
            System.out.println("📊 Loading Reports...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/reports.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Reports - Attendance System");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("❌ Error loading Reports: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load Reports page.");
        }
    }
    
    @FXML
    private void handleQuickActions() {
        // This could be a modal or quick action panel
        showAlert("Quick Actions", "Quick actions feature is coming soon!");
    }
    
    @FXML
    private void handleRecentActivity() {
        // Load full activity log
        try {
            System.out.println("📝 Loading Recent Activity...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/activity_log.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Activity Log - Attendance System");
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("❌ Error loading Activity Log: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load Activity Log page.");
        }
    }
    
    
    public class AdminDashboardController {
        @FXML
        private StackPane contentPane;
        
        // Method to load any view into the center
        public void loadView(String fxmlPath) {
            try {
                contentPane.getChildren().clear();
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent view = loader.load();
                contentPane.getChildren().add(view);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @FXML
        private void loadManageClasses() {
            loadView("/attendance/view/manage_classes.fxml");
        }
        
        @FXML 
        private void loadManageTeachers() {
            loadView("/attendance/view/manage_teachers.fxml");  // You need to create this
        }
        
        @FXML
        private void loadDashboardContent() {
            loadView("/attendance/view/dashboard_content.fxml");  // Simple dashboard content
        }
    }
    
    
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Logout");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                // Return to login page
                attendance.Main.showLoginPage();
                
            } catch (Exception e) {
                System.err.println("❌ Error during logout: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Method to refresh dashboard data (can be called from other controllers when returning)
    public void refreshDashboard() {
        loadStatistics();
    }
}