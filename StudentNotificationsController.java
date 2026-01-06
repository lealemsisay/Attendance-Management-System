package attendance.controller;

import attendance.model.Notification;
import attendance.model.User;
import attendance.service.NotificationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class StudentNotificationsController implements Initializable {

    @FXML private VBox notificationsContainer;
    @FXML private Label titleLabel;
    
    private NotificationService notificationService;
    private User currentUser;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        notificationService = new NotificationService();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadNotifications();
    }
    
    private void loadNotifications() {
        if (currentUser != null) {
            List<Notification> notifications = notificationService.getNotifications(currentUser.getId());
            
            notificationsContainer.getChildren().clear();
            
            if (notifications.isEmpty()) {
                Label noNotifications = new Label("No notifications");
                noNotifications.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 20px;");
                notificationsContainer.getChildren().add(noNotifications);
            } else {
                for (Notification notification : notifications) {
                    VBox notificationCard = createNotificationCard(notification);
                    notificationsContainer.getChildren().add(notificationCard);
                }
            }
            
            titleLabel.setText("Notifications (" + notifications.size() + ")");
        }
    }
    
    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                     "-fx-background-radius: 5; -fx-padding: 10; -fx-margin: 5;");
        card.setPrefWidth(400);
        
        // Title
        Label titleLabel = new Label(notification.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Message
        Label messageLabel = new Label(notification.getMessage());
        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        messageLabel.setWrapText(true);
        messageLabel.setPrefWidth(380);
        
        // Footer with sender and time
        HBox footer = new HBox(10);
        footer.setStyle("-fx-alignment: center-left;");
        
        Label senderLabel = new Label("From: " + (notification.getSenderName() != null ? 
                                                notification.getSenderName() : "System"));
        senderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        
        Label timeLabel = new Label(notification.getTimestamp().format(formatter));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        
        footer.getChildren().addAll(senderLabel, timeLabel);
        
        // Status indicator
        if (!notification.isRead()) {
            Label newLabel = new Label("NEW");
            newLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: #4a90e2; " +
                            "-fx-padding: 2 5; -fx-background-radius: 10;");
            footer.getChildren().add(newLabel);
        }
        
        card.getChildren().addAll(titleLabel, messageLabel, footer);
        return card;
    }
    
    @FXML
    private void handleBack() {
        // This will be handled by the navigation
        System.out.println("Go back to dashboard");
    }
    
    @FXML
    private void handleMarkAllAsRead() {
        if (currentUser != null) {
            notificationService.markAllAsRead(currentUser.getId());
            loadNotifications();
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadNotifications();
    }
}