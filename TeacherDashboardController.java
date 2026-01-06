package attendance.controller;

import attendance.model.User;
import attendance.service.ClassService;
import attendance.service.TeacherService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class TeacherDashboardController {
    
    @FXML private Label welcomeLabel;
    @FXML private Label todayDateLabel;
    @FXML private Label totalClassesLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label attendanceRateLabel;
    
    // Assigned Classes Section
    @FXML private TableView<Map<String, Object>> classesTable;
    @FXML private TableColumn<Map<String, Object>, String> classNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> roomColumn;
    @FXML private TableColumn<Map<String, Object>, String> scheduleColumn;
    @FXML private TableColumn<Map<String, Object>, Integer> studentCountColumn;
    
    @FXML private Button takeAttendanceBtn;
    @FXML private Button viewAttendanceBtn;
    @FXML private Button refreshClassesBtn;
    @FXML private VBox assignedClassesBox;
    
    private User currentUser;
    private TeacherService teacherService;
    private ClassService classService;
    private ObservableList<Map<String, Object>> classesList;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ TeacherDashboardController: User set - " + 
                          (user != null ? user.getFirstName() : "null"));
        initializeDashboard();
    }
    
    @FXML
    public void initialize() {
        System.out.println("✅ TeacherDashboardController.initialize() called");
        teacherService = new TeacherService();
        classService = new ClassService();
        classesList = FXCollections.observableArrayList();
        
        // Initialize UI
        setupDate();
        setupTableColumns();
        
        // Set up button actions
        takeAttendanceBtn.setOnAction(e -> handleTakeAttendance());
        viewAttendanceBtn.setOnAction(e -> handleViewAttendance());
        refreshClassesBtn.setOnAction(e -> handleRefreshClasses());
        
        System.out.println("✅ TeacherDashboardController initialized successfully");
    }
    
    private void initializeDashboard() {
        if (currentUser != null) {
            // Update welcome message
            welcomeLabel.setText("Welcome, " + currentUser.getFirstName() + " " + currentUser.getLastName());
            
            // Get teacher ID from user ID
            Integer teacherId = teacherService.getTeacherIdFromUserId(currentUser.getId());
            
            if (teacherId != null) {
                System.out.println("✅ Teacher ID found: " + teacherId);
                // Load assigned classes
                loadAssignedClasses(teacherId);
                
                // Update statistics
                updateStatistics(teacherId);
            } else {
                System.out.println("❌ Teacher ID not found for user ID: " + currentUser.getId());
                showAlert("Error", "Teacher profile not found. Please contact administrator.");
            }
        }
    }
    
    private void setupDate() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        todayDateLabel.setText("Today is " + today.format(formatter));
    }
    
    private void setupTableColumns() {
        classNameColumn.setCellValueFactory(data -> {
            Map<String, Object> rowData = data.getValue();
            String className = (String) rowData.get("className");
            return new javafx.beans.property.SimpleStringProperty(className != null ? className : "");
        });
        
        roomColumn.setCellValueFactory(data -> {
            Map<String, Object> rowData = data.getValue();
            String room = (String) rowData.get("room");
            return new javafx.beans.property.SimpleStringProperty(room != null ? room : "");
        });
        
        scheduleColumn.setCellValueFactory(data -> {
            Map<String, Object> rowData = data.getValue();
            String schedule = (String) rowData.get("schedule");
            return new javafx.beans.property.SimpleStringProperty(schedule != null ? schedule : "");
        });
        
        studentCountColumn.setCellValueFactory(data -> {
            Map<String, Object> rowData = data.getValue();
            Integer studentCount = (Integer) rowData.get("studentCount");
            return new javafx.beans.property.SimpleIntegerProperty(studentCount != null ? studentCount : 0).asObject();
        });
        
        classesTable.setItems(classesList);
    }
    
    private void loadAssignedClasses(int teacherId) {
        System.out.println("🔍 Loading assigned classes for teacher ID: " + teacherId);
        classesList.clear();
        
        // Get classes assigned to this teacher
        List<Map<String, Object>> classes = classService.getClassesByTeacher(teacherId);
        
        if (classes != null && !classes.isEmpty()) {
            System.out.println("✅ Found " + classes.size() + " assigned classes");
            classesList.addAll(classes);
            
            // Update UI to show classes
            assignedClassesBox.setVisible(true);
            assignedClassesBox.setManaged(true);
        } else {
            System.out.println("⚠️ No classes assigned to this teacher");
            classesList.clear();
            
            // Show message if no classes
            Label noClassesLabel = new Label("No classes assigned yet.");
            noClassesLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            assignedClassesBox.getChildren().clear();
            assignedClassesBox.getChildren().add(noClassesLabel);
        }
        
        // Update classes count
        totalClassesLabel.setText(String.valueOf(classesList.size()));
    }
    
    private void updateStatistics(int teacherId) {
        // Get total students across all assigned classes
        int totalStudents = 0;
        for (Map<String, Object> cls : classesList) {
            Integer studentCount = (Integer) cls.get("studentCount");
            if (studentCount != null) {
                totalStudents += studentCount;
            }
        }
        totalStudentsLabel.setText(String.valueOf(totalStudents));
        
        // Calculate attendance rate (you can implement this based on your needs)
        double attendanceRate = teacherService.getAverageAttendanceRate(teacherId);
        attendanceRateLabel.setText(String.format("%.1f%%", attendanceRate));
    }
    
    @FXML
    private void handleTakeAttendance() {
        System.out.println("✅ Take Attendance button clicked");
        
        // Get selected class
        Map<String, Object> selectedClass = classesTable.getSelectionModel().getSelectedItem();
        
        if (selectedClass == null) {
            showAlert("No Selection", "Please select a class to take attendance for.");
            return;
        }
        
        String className = (String) selectedClass.get("className");
        Integer classId = (Integer) selectedClass.get("id");
        
        System.out.println("✅ Taking attendance for class: " + className + " (ID: " + classId + ")");
        
        // Here you would navigate to the take attendance page
        // For now, show a message
        showAlert("Take Attendance", 
                 "Taking attendance for: " + className + "\n\n" +
                 "This feature will open the attendance taking interface.");
    }
    
    @FXML
    private void handleViewAttendance() {
        System.out.println("✅ View Attendance button clicked");
        
        // Get selected class
        Map<String, Object> selectedClass = classesTable.getSelectionModel().getSelectedItem();
        
        if (selectedClass == null) {
            showAlert("No Selection", "Please select a class to view attendance.");
            return;
        }
        
        String className = (String) selectedClass.get("className");
        Integer classId = (Integer) selectedClass.get("id");
        
        System.out.println("✅ Viewing attendance for class: " + className + " (ID: " + classId + ")");
        
        // Here you would navigate to the view attendance page
        // For now, show a message
        showAlert("View Attendance", 
                 "Viewing attendance for: " + className + "\n\n" +
                 "This feature will show attendance statistics for this class.");
    }
    
    @FXML
    private void handleRefreshClasses() {
        System.out.println("🔄 Refreshing assigned classes...");
        
        if (currentUser != null) {
            Integer teacherId = teacherService.getTeacherIdFromUserId(currentUser.getId());
            if (teacherId != null) {
                loadAssignedClasses(teacherId);
                updateStatistics(teacherId);
                showAlert("Refreshed", "Assigned classes have been refreshed.");
            }
        }
    }
    
    @FXML
    private void handleViewStudents() {
        System.out.println("✅ View Students button clicked");
        
        // Get selected class
        Map<String, Object> selectedClass = classesTable.getSelectionModel().getSelectedItem();
        
        if (selectedClass == null) {
            showAlert("No Selection", "Please select a class to view students.");
            return;
        }
        
        String className = (String) selectedClass.get("className");
        Integer classId = (Integer) selectedClass.get("id");
        
        System.out.println("✅ Viewing students for class: " + className + " (ID: " + classId + ")");
        
        // Get students in this class
        List<Map<String, Object>> students = teacherService.getStudentsInClass(className);
        
        if (students != null && !students.isEmpty()) {
            StringBuilder studentList = new StringBuilder();
            studentList.append("Students in ").append(className).append(":\n\n");
            
            for (Map<String, Object> student : students) {
                String studentId = (String) student.get("studentId");
                String firstName = (String) student.get("firstName");
                String lastName = (String) student.get("lastName");
                studentList.append("• ").append(firstName).append(" ").append(lastName)
                          .append(" (").append(studentId).append(")\n");
            }
            
            showAlert("Students in " + className, studentList.toString());
        } else {
            showAlert("No Students", "No students are enrolled in " + className);
        }
    }
    
    @FXML
    private void handleLogout() {
        System.out.println("🚪 Logout requested");
        // This would typically be handled by the main controller
        // For now, show a message
        showAlert("Logout", "You have been logged out. Returning to login screen.");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}