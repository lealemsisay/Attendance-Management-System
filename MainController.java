package attendance.controller;

import attendance.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Button menuToggleButton;
    @FXML private MenuButton userMenu;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;
    
    @FXML private Button dashboardBtn;
    @FXML private Button studentsBtn;
    @FXML private Button classesBtn;
    @FXML private Button teachersBtn;
    @FXML private Button reportsBtn;
    @FXML private Button assignedClassesBtn;
    
    private Button activeButton;
    private boolean sidebarCollapsed = false;
    private User currentUser;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MainController initialized");
        
        // Initialize active button
        activeButton = dashboardBtn;
        
        // Set default view (will be overridden by role)
        loadView("/attendance/view/admin_dashboard.fxml");
        setActiveButton(dashboardBtn);
        
        // Setup button actions
        setupButtonActions();
    }
    
    private void setupButtonActions() {
        dashboardBtn.setOnAction(e -> showDashboard());
        studentsBtn.setOnAction(e -> showManageStudents());
        classesBtn.setOnAction(e -> showManageClasses());
        teachersBtn.setOnAction(e -> showManageTeachers());
        reportsBtn.setOnAction(e -> showViewAttendance());
        
        // Setup assigned classes button if it exists
        if (assignedClassesBtn != null) {
            assignedClassesBtn.setOnAction(e -> showAssignedClasses());
        }
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            sidebarUserName.setText(user.getFirstName() + " " + user.getLastName());
            sidebarUserRole.setText(user.getRole());
            userMenu.setText("👤 " + user.getFirstName());
            
            // Show/hide buttons based on role
            updateSidebarForRole(user.getRole());
            
            // Load appropriate dashboard
            showDashboard();
        }
    }
    
    private void updateSidebarForRole(String role) {
        if ("TEACHER".equals(role)) {
            // For teachers, show only relevant buttons
            dashboardBtn.setText("📊 Dashboard");
            studentsBtn.setVisible(false);
            studentsBtn.setManaged(false);
            classesBtn.setVisible(false);
            classesBtn.setManaged(false);
            teachersBtn.setVisible(false);
            teachersBtn.setManaged(false);
            assignedClassesBtn.setVisible(true);
            assignedClassesBtn.setManaged(true);
            assignedClassesBtn.setText("📚 My Classes");
            reportsBtn.setText("📝 Take Attendance");
        } else if ("STUDENT".equals(role)) {
            // For students
            dashboardBtn.setText("📊 Dashboard");
            studentsBtn.setVisible(false);
            studentsBtn.setManaged(false);
            classesBtn.setVisible(false);
            classesBtn.setManaged(false);
            teachersBtn.setVisible(false);
            teachersBtn.setManaged(false);
            assignedClassesBtn.setVisible(false);
            assignedClassesBtn.setManaged(false);
            reportsBtn.setText("📊 My Attendance");
        } else {
            // For admins (default)
            dashboardBtn.setText("📊 Dashboard");
            studentsBtn.setVisible(true);
            studentsBtn.setManaged(true);
            classesBtn.setVisible(true);
            classesBtn.setManaged(true);
            teachersBtn.setVisible(true);
            teachersBtn.setManaged(true);
            assignedClassesBtn.setVisible(false);
            assignedClassesBtn.setManaged(false);
            reportsBtn.setText("📊 Reports");
        }
    }
    
    @FXML
    private void toggleSidebar() {
        if (sidebarCollapsed) {
            sidebar.setPrefWidth(260);
            sidebar.setMinWidth(260);
            menuToggleButton.setText("☰");
        } else {
            sidebar.setPrefWidth(70);
            sidebar.setMinWidth(70);
            menuToggleButton.setText("→");
        }
        sidebarCollapsed = !sidebarCollapsed;
    }
    
    @FXML
    private void showDashboard() {
        if (currentUser != null) {
            switch (currentUser.getRole()) {
                case "TEACHER":
                    loadTeacherDashboard();
                    break;
                case "STUDENT":
                    loadStudentDashboard();
                    break;
                default:
                    loadView("/attendance/view/admin_dashboard.fxml");
                    break;
            }
        } else {
            loadView("/attendance/view/admin_dashboard.fxml");
        }
        setActiveButton(dashboardBtn);
    }
    
    @FXML
    private void showManageStudents() {
        loadView("/attendance/view/admin_manage_students.fxml");
        setActiveButton(studentsBtn);
    }
    
    @FXML
    private void showManageClasses() {
        loadView("/attendance/view/manage_classes.fxml");
        setActiveButton(classesBtn);
    }
    
    @FXML
    private void showManageTeachers() {
        loadView("/attendance/view/manage_teachers.fxml");
        setActiveButton(teachersBtn);
    }
    
    @FXML
    private void showViewAttendance() {
        if (currentUser != null && "TEACHER".equals(currentUser.getRole())) {
            // For teachers, show take attendance page
            loadView("/attendance/view/take_attendance.fxml");
        } else if (currentUser != null && "STUDENT".equals(currentUser.getRole())) {
            // For students, show their attendance
            loadView("/attendance/view/view_attendance_student.fxml");
        } else {
            // For admins, show reports
            loadView("/attendance/view/view_attendance.fxml");
        }
        setActiveButton(reportsBtn);
    }
    
    @FXML
    private void showAssignedClasses() {
        System.out.println("📚 Loading assigned classes for teacher...");
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/assigned_classes.fxml"));
            Parent view = loader.load();
            
            // Get controller and set current user
            AssignedClassesController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            // Load into content area
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
            setActiveButton(assignedClassesBtn);
            
            System.out.println("✅ Assigned classes loaded successfully");
        } catch (Exception e) {
            System.err.println("❌ Error loading assigned classes: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load assigned classes: " + e.getMessage());
        }
    }
    
    private void loadTeacherDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/teacher_dashboard.fxml"));
            Parent view = loader.load();
            
            // Get controller and set current user
            TeacherDashboardController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
            System.out.println("✅ Teacher dashboard loaded successfully");
        } catch (Exception e) {
            System.err.println("❌ Error loading teacher dashboard: " + e.getMessage());
            e.printStackTrace();
            loadView("/attendance/view/placeholder.fxml");
        }
    }
    
    private void loadStudentDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/student_dashboard.fxml"));
            Parent view = loader.load();
            
            // Get controller and set current user if needed
            Object controller = loader.getController();
            if (controller instanceof StudentDashboardController) {
                ((StudentDashboardController) controller).setCurrentUser(currentUser);
            }
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
            System.out.println("✅ Student dashboard loaded successfully");
        } catch (Exception e) {
            System.err.println("❌ Error loading student dashboard: " + e.getMessage());
            e.printStackTrace();
            loadView("/attendance/view/placeholder.fxml");
        }
    }
    
    @FXML
    private void showProfile() {
        showAlert("Profile", "User profile feature coming soon!");
    }
    
    @FXML
    private void showSettings() {
        showAlert("Settings", "System settings feature coming soon!");
    }
    
    @FXML
    private void handleLogout() {
        try {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Logout");
            confirm.setHeaderText("Are you sure you want to logout?");
            confirm.setContentText("You will be redirected to the login screen.");
            
            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                Parent root = FXMLLoader.load(getClass().getResource("/attendance/view/login.fxml"));
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Login - Attendance System");
                stage.centerOnScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Logout Error", "Failed to logout: " + e.getMessage());
        }
    }
    
    private void loadView(String fxmlPath) {
        try {
            System.out.println("\n=== Loading view: " + fxmlPath + " ===");
            
            // Clear current content
            contentArea.getChildren().clear();
            
            // Load new FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            
            // Set current user if controller has setCurrentUser method
            Object controller = loader.getController();
            if (controller instanceof ManageClassesController) {
                ((ManageClassesController) controller).setCurrentUser(currentUser);
            }
            
            contentArea.getChildren().add(view);
            System.out.println("✓ View loaded successfully: " + fxmlPath);
            
        } catch (IOException e) {
            System.err.println("❌ ERROR loading view: " + fxmlPath);
            System.err.println("Error details: " + e.getMessage());
            
            // Show error message
            showErrorView(fxmlPath, e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error loading view: " + fxmlPath);
            e.printStackTrace();
            showErrorView(fxmlPath, e.getMessage());
        }
    }

    private void showErrorView(String fxmlPath, String error) {
        VBox errorBox = new VBox(20);
        errorBox.setStyle("-fx-padding: 40; -fx-alignment: center;");
        
        Label errorLabel = new Label("Error loading: " + fxmlPath);
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label detailLabel = new Label("Error: " + error);
        detailLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        
        Button retryButton = new Button("Try Again");
        retryButton.setOnAction(e -> {
            if (fxmlPath.contains("classes")) showManageClasses();
            else if (fxmlPath.contains("dashboard")) showDashboard();
            else showDashboard();
        });
        
        errorBox.getChildren().addAll(errorLabel, detailLabel, retryButton);
        contentArea.getChildren().add(errorBox);
    }
    
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        button.getStyleClass().add("active");
        activeButton = button;
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
}