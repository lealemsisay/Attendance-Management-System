package attendance.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import attendance.model.User;
import attendance.model.Notification;
import attendance.service.NotificationService;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class StudentMainController implements Initializable {
    
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Label unreadCountLabel;
    
    private User currentUser;
    private NotificationService notificationService;
    private Timeline notificationCheckTimer;
    
    
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data will be initialized when setCurrentUser is called
    }
    
    // Start checking for notifications every 30 seconds
    private void startNotificationCheck() {
        if (notificationCheckTimer != null) {
            notificationCheckTimer.stop();
        }
        
        notificationCheckTimer = new Timeline(
            new KeyFrame(Duration.seconds(30), e -> checkForNotifications())
        );
        notificationCheckTimer.setCycleCount(Timeline.INDEFINITE);
        notificationCheckTimer.play();
        
        // Check immediately
        checkForNotifications();
    }
    
  
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.notificationService = new NotificationService();
        
        System.out.println("\n=== STUDENT LOGIN ===");
        System.out.println("Student ID: " + currentUser.getId());
        System.out.println("Student Username: " + currentUser.getUsername());
        
        // Check for notifications immediately
        checkForNotifications();
        
        // Also get all notifications for this student
        List<Notification> allNotifs = notificationService.getNotifications(currentUser.getId());
        System.out.println("Total notifications for student: " + allNotifs.size());
        for (Notification n : allNotifs) {
            System.out.println("  - " + n.getTitle() + " (Read: " + n.isRead() + ")");
        }
        
        startNotificationCheck();
        loadView("student_home", "Student Dashboard");
    }
    
    
    private void checkForNotifications() {
        if (currentUser != null && notificationService != null) {
            System.out.println("DEBUG: Checking notifications for user ID: " + currentUser.getId());
            int unreadCount = notificationService.getUnreadCount(currentUser.getId());
            System.out.println("DEBUG: Found " + unreadCount + " unread notifications");
            
            if (unreadCountLabel != null) {
                unreadCountLabel.setText(String.valueOf(unreadCount));
                unreadCountLabel.setVisible(unreadCount > 0);
                
                if (unreadCount > 0) {
                    System.out.println("🎯 Student has " + unreadCount + " unread notification(s)!");
                    // List all notifications for this user
                    List<Notification> notifications = notificationService.getNotifications(currentUser.getId());
                    for (Notification n : notifications) {
                        System.out.println("📨 ID: " + n.getId() + 
                                         ", Title: " + n.getTitle() + 
                                         ", Read: " + n.isRead());
                    }
                }
            }
        }
    }
    
    private void showNotificationAlert(int unreadCount) {
        // This shows a popup when there are new notifications
        // You can customize this as needed
        System.out.println("You have " + unreadCount + " unread notifications");
        
        // Optionally show a non-intrusive alert
        if (unreadCount == 1) {
            // Show alert for first notification
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("New Notification");
            alert.setHeaderText("You have a new notification");
            alert.setContentText("Check the notification bell for details.");
            alert.show();
        }
    }
    
    @FXML
    private void showNotifications() {
        // When student clicks the notification bell
        try {
            // Load a simple notification view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/student_notifications.fxml"));
            Parent view = loader.load();
            
            StudentNotificationsController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(currentUser);
            }
            
            contentArea.getChildren().setAll(view);
            
            // Mark all notifications as read when viewed
            if (currentUser != null) {
                notificationService.markAllAsRead(currentUser.getId());
                checkForNotifications(); // Update the count
            }
            
        } catch (Exception e) {
            System.err.println("Error loading notifications view");
            e.printStackTrace();
            showPlaceholder("Notifications");
        }
    }
    
    
    
    
    
    @FXML
    private void loadHome() {
        loadView("student_home", "Student Dashboard");
    }
    
    @FXML
    private void showViewAttendance() {
        loadView("view_attendance_student", "View My Attendance");
    }
    
    private void loadView(String viewName, String title) {
        try {
            String fxmlFile = "/attendance/view/" + viewName + ".fxml";
            URL resource = getClass().getResource(fxmlFile);
            
            if (resource == null) {
                System.out.println("FXML file not found: " + fxmlFile);
                showPlaceholder(title);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            
            // Set current user for student views that need it
            Object controller = loader.getController();
            if (controller instanceof StudentDashboardController) {
                ((StudentDashboardController) controller).setCurrentUser(currentUser);
            }
            
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
            System.err.println("Error loading view: " + viewName);
            e.printStackTrace();
            showPlaceholder(title);
        }
    }
    
    private void showPlaceholder(String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/placeholder.fxml"));
            Parent view = loader.load();
            PlaceholderController controller = loader.getController();
            controller.setTitle(title + " - Under Construction");
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("Error loading placeholder view");
            e.printStackTrace();
        }
    }
    
    
    
    @FXML
    private void toggleSidebar() {
        if (sidebar != null) {
            sidebar.setVisible(!sidebar.isVisible());
            sidebar.setManaged(!sidebar.isManaged());
        }
    }
    
    @FXML
    private void logout() {
        // Stop the notification timer
        if (notificationCheckTimer != null) {
            notificationCheckTimer.stop();
        }
        
        try {
            attendance.Main.showLoginPage();
        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}