package attendance.controller;

import attendance.Main;
import attendance.model.User;
import attendance.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    
    private AuthService authService;
    
    @FXML
    public void initialize() {
        System.out.println("✅ LoginController initialized");
        authService = new AuthService();
        errorLabel.setVisible(false);
        
        // Set up enter key to trigger login
        usernameField.setOnAction(e -> loginButton.fire());
        passwordField.setOnAction(e -> loginButton.fire());
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        try {
            System.out.println("🔐 Attempting login for user: " + username);
            System.out.println("Authenticating: " + username + "/" + password);
            
            User user = authService.authenticate(username, password);
            
            if (user != null) {
                System.out.println("✅ Login successful for: " + user.getFirstName());
                System.out.println("   Role: " + user.getRole());
                System.out.println("   User ID: " + user.getId());
                
                // Redirect based on role (all use the same MainController)
                System.out.println("   Redirecting to " + user.getRole() + " Dashboard...");
                
                // Use the single dashboard method for all roles
                Main.showMainDashboard(user);
                
            } else {
                System.out.println("❌ Login failed: Invalid credentials");
                errorLabel.setText("Invalid username or password!");
                errorLabel.setVisible(true);
                showAlert("Login Failed", "Invalid username or password!");
            }
        } catch (Exception e) {
            System.err.println("❌ Login error: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("Login error: " + e.getMessage());
            errorLabel.setVisible(true);
            showAlert("Login Error", "An error occurred during login: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClear() {
        usernameField.clear();
        passwordField.clear();
        errorLabel.setVisible(false);
        usernameField.requestFocus();
    }
    
    @FXML
    private void handleExit() {
        System.out.println("🚪 Exiting application...");
        System.exit(0);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}