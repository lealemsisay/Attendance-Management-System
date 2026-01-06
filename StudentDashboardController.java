package attendance.controller;

import attendance.model.User;
import attendance.model.Student;
import attendance.model.Attendance;
import attendance.service.StudentService;
import attendance.service.NotificationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class StudentDashboardController implements Initializable {
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalClassesLabel;
    @FXML private Label presentCountLabel;
    @FXML private Label attendancePercentageLabel;
    
    private User currentUser;
    private Student currentStudent;
    private StudentService studentService;
    private NotificationService notificationService;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.studentService = new StudentService();
        this.notificationService = new NotificationService();
        
        initializeData();
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data will be initialized when setCurrentUser is called
    }
    
    private void initializeData() {
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getFirstName() + " " + currentUser.getLastName());
            
            // Load student data
            currentStudent = studentService.getStudentByUserId(currentUser.getId());
            if (currentStudent != null) {
                updateDashboardStats();
            } else {
                System.out.println("No student record found for user: " + currentUser.getUsername());
                // Set default values
                setDefaultStats();
            }
        }
    }
    
    private void updateDashboardStats() {
        if (currentStudent != null) {
            try {
                // Calculate attendance statistics
                List<Attendance> attendanceList = studentService.getAttendanceForStudent(currentStudent.getId());
                int totalClasses = attendanceList.size();
                int presentCount = (int) attendanceList.stream()
                        .filter(a -> a != null && "PRESENT".equals(a.getStatus()))
                        .count();
                double percentage = totalClasses > 0 ? (presentCount * 100.0 / totalClasses) : 0;
                
                totalClassesLabel.setText("Total Classes: " + totalClasses);
                presentCountLabel.setText("Present: " + presentCount);
                attendancePercentageLabel.setText(String.format("Percentage: %.1f%%", percentage));
                
                // Check for unread notifications
                int unreadNotifications = notificationService.getUnreadCount(currentUser.getId());
                if (unreadNotifications > 0) {
                    System.out.println("You have " + unreadNotifications + " unread notifications");
                }
                
            } catch (Exception e) {
                System.err.println("Error updating dashboard stats: " + e.getMessage());
                e.printStackTrace();
                setDefaultStats();
            }
        } else {
            setDefaultStats();
        }
    }
    
    private void setDefaultStats() {
        totalClassesLabel.setText("Total Classes: 0");
        presentCountLabel.setText("Present: 0");
        attendancePercentageLabel.setText("Percentage: 0%");
    }
    
    @FXML
    private void handleViewAttendance() {
        // This will be handled by the main controller navigation
        System.out.println("Navigate to view attendance");
    }
    
    // ADD THIS METHOD - It was missing and causing the error
    @FXML
    private void handleMessages() {
        System.out.println("Messages button clicked");
        // Show notifications view - this should be handled by StudentMainController
        // For now, just print a message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Messages");
        alert.setHeaderText("Messages Feature");
        alert.setContentText("The messages feature will be available soon!");
        alert.showAndWait();
    }
}