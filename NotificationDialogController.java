package attendance.controller;

import attendance.model.User;
import attendance.service.NotificationService;
import attendance.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationDialogController implements Initializable {

    @FXML private ComboBox<String> recipientTypeCombo;
    @FXML private ComboBox<User> specificRecipientCombo;
    @FXML private TextField titleField;
    @FXML private TextArea messageArea;
    @FXML private Label statusLabel;
    
    private NotificationService notificationService;
    private UserService userService;
    private User currentUser;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        notificationService = new NotificationService();
        userService = new UserService();
        
        setupRecipientTypeCombo();
        setupSpecificRecipientCombo();
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    private void setupRecipientTypeCombo() {
        recipientTypeCombo.getItems().addAll(
            "Specific User",
            "All Students", 
            "All Teachers",
            "Everyone"
        );
        recipientTypeCombo.setValue("Specific User");
        
        recipientTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            specificRecipientCombo.setDisable(!"Specific User".equals(newVal));
        });
    }
    
    private void setupSpecificRecipientCombo() {
        specificRecipientCombo.setCellFactory(param -> new javafx.scene.control.ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFirstName() + " " + user.getLastName() + " (" + user.getRole() + ")");
                }
            }
        });
        
        // Load all users except current user
        specificRecipientCombo.getItems().addAll(userService.getAllUsers());
    }
    
    @FXML
    private void handleSendNotification() {
        if (validateInput()) {
            String recipientType = recipientTypeCombo.getValue();
            String title = titleField.getText().trim();
            String message = messageArea.getText().trim();
            boolean success = false;
            
            try {
                if ("Specific User".equals(recipientType)) {
                    User recipient = specificRecipientCombo.getValue();
                    if (recipient != null) {
                        success = notificationService.sendNotification(
                            currentUser.getId(), 
                            recipient.getId(), 
                            title, 
                            message
                        );
                        statusLabel.setText("Notification sent to " + recipient.getFirstName());
                    }
                } else if ("All Students".equals(recipientType)) {
                    success = notificationService.sendNotificationToAllStudents(currentUser.getId(), title, message);
                    statusLabel.setText("Notification sent to all students");
                } else if ("All Teachers".equals(recipientType)) {
                    success = notificationService.sendNotificationToAllTeachers(currentUser.getId(), title, message);
                    statusLabel.setText("Notification sent to all teachers");
                } else if ("Everyone".equals(recipientType)) {
                    success = notificationService.sendBroadcastNotification(currentUser.getId(), title, message);
                    statusLabel.setText("Notification broadcasted to everyone");
                }
                
                if (success) {
                    statusLabel.setStyle("-fx-text-fill: green;");
                    clearForm();
                } else {
                    statusLabel.setText("Failed to send notification. Stored locally.");
                    statusLabel.setStyle("-fx-text-fill: orange;");
                }
                
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }
    
    private boolean validateInput() {
        if (recipientTypeCombo.getValue() == null) {
            statusLabel.setText("Please select recipient type.");
            return false;
        }
        if (titleField.getText().trim().isEmpty()) {
            statusLabel.setText("Please enter a title.");
            return false;
        }
        if (messageArea.getText().trim().isEmpty()) {
            statusLabel.setText("Please enter a message.");
            return false;
        }
        if ("Specific User".equals(recipientTypeCombo.getValue()) && 
            specificRecipientCombo.getValue() == null) {
            statusLabel.setText("Please select a recipient.");
            return false;
        }
        return true;
    }
    
    private void clearForm() {
        titleField.clear();
        messageArea.clear();
        specificRecipientCombo.setValue(null);
        recipientTypeCombo.setValue("Specific User");
    }
    
    @FXML
    private void handleCancel() {
        titleField.getScene().getWindow().hide();
    }
}