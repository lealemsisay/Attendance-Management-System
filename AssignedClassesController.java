package attendance.controller;

import attendance.model.User;
import attendance.service.ClassService;
import attendance.service.TeacherService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignedClassesController {
    
    @FXML private TableView<Map<String, Object>> classesTable;
    @FXML private TableColumn<Map<String, Object>, String> classNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> roomColumn;
    @FXML private TableColumn<Map<String, Object>, String> scheduleColumn;
    @FXML private TableColumn<Map<String, Object>, Integer> studentCountColumn;
    
    @FXML private Label totalClassesLabel;
    @FXML private Label totalStudentsLabel;
    
    private User currentUser;
    private TeacherService teacherService;
    private ClassService classService;
    private ObservableList<Map<String, Object>> classesList;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ AssignedClassesController: User set - " + 
                          (user != null ? user.getFirstName() : "null"));
        initializeData();
    }
    
    @FXML
    public void initialize() {
        System.out.println("✅ AssignedClassesController.initialize() called");
        teacherService = new TeacherService();
        classService = new ClassService();
        classesList = FXCollections.observableArrayList();
        
        // Setup table columns
        setupTableColumns();
        
        classesTable.setItems(classesList);
        System.out.println("✅ AssignedClassesController initialized successfully");
    }
    
    private void setupTableColumns() {
        // Setup cell value factories
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
    }
    
    private void initializeData() {
        if (currentUser != null) {
            // Get teacher ID from user ID
            Integer teacherId = teacherService.getTeacherIdFromUserId(currentUser.getId());
            
            if (teacherId != null) {
                System.out.println("✅ Teacher ID found: " + teacherId);
                loadAssignedClasses(teacherId);
                updateStatistics();
            } else {
                System.out.println("❌ Teacher ID not found for user ID: " + currentUser.getId());
                showAlert("Error", "Teacher profile not found. Please contact administrator.");
            }
        }
    }
    
    private void loadAssignedClasses(int teacherId) {
        System.out.println("🔍 Loading assigned classes for teacher ID: " + teacherId);
        classesList.clear();
        
        // Get classes assigned to this teacher
        List<Map<String, Object>> classes = classService.getClassesByTeacher(teacherId);
        
        if (classes != null && !classes.isEmpty()) {
            System.out.println("✅ Found " + classes.size() + " assigned classes");
            
            // Convert to proper format for table
            for (Map<String, Object> cls : classes) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", cls.get("id"));
                classData.put("className", cls.get("className"));
                classData.put("room", cls.get("room"));
                classData.put("schedule", cls.get("schedule"));
                
                // Get student count
                Integer studentCount = (Integer) cls.get("studentCount");
                classData.put("studentCount", studentCount != null ? studentCount : 0);
                
                classesList.add(classData);
            }
        } else {
            System.out.println("⚠️ No classes assigned to this teacher");
            classesList.clear();
            
            // Show message in table
            Map<String, Object> noClassesRow = new HashMap<>();
            noClassesRow.put("className", "No classes assigned");
            noClassesRow.put("room", "N/A");
            noClassesRow.put("schedule", "N/A");
            noClassesRow.put("studentCount", 0);
            classesList.add(noClassesRow);
        }
        
        // Update classes count
        totalClassesLabel.setText(String.valueOf(classesList.size()));
    }
    
    private void updateStatistics() {
        // Calculate total students across all classes
        int totalStudents = 0;
        for (Map<String, Object> cls : classesList) {
            Object studentCountObj = cls.get("studentCount");
            if (studentCountObj instanceof Integer) {
                totalStudents += (Integer) studentCountObj;
            }
        }
        totalStudentsLabel.setText(String.valueOf(totalStudents));
    }
    
    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Refreshing assigned classes...");
        
        if (currentUser != null) {
            Integer teacherId = teacherService.getTeacherIdFromUserId(currentUser.getId());
            if (teacherId != null) {
                loadAssignedClasses(teacherId);
                updateStatistics();
                showAlert("Refreshed", "Assigned classes have been refreshed.");
            }
        }
    }
    
    @FXML
    private void handleTakeAttendance() {
        System.out.println("✅ Take Attendance button clicked");
        
        // Get selected class
        Map<String, Object> selectedClass = classesTable.getSelectionModel().getSelectedItem();
        
        if (selectedClass == null || "No classes assigned".equals(selectedClass.get("className"))) {
            showAlert("No Selection", "Please select a class to take attendance for.");
            return;
        }
        
        String className = (String) selectedClass.get("className");
        System.out.println("✅ Taking attendance for class: " + className);
        
        // Here you would navigate to take attendance page
        showAlert("Take Attendance", 
                 "Taking attendance for: " + className + "\n\n" +
                 "This feature will open the attendance taking interface.");
    }
    
    @FXML
    private void handleViewAttendance() {
        System.out.println("✅ View Attendance button clicked");
        
        // Get selected class
        Map<String, Object> selectedClass = classesTable.getSelectionModel().getSelectedItem();
        
        if (selectedClass == null || "No classes assigned".equals(selectedClass.get("className"))) {
            showAlert("No Selection", "Please select a class to view attendance.");
            return;
        }
        
        String className = (String) selectedClass.get("className");
        System.out.println("✅ Viewing attendance for class: " + className);
        
        // Here you would navigate to view attendance page
        showAlert("View Attendance", 
                 "Viewing attendance for: " + className + "\n\n" +
                 "This feature will show attendance statistics for this class.");
    }
    
    @FXML
    private void handleViewStudents() {
        System.out.println("✅ View Students button clicked");
        
        // Get selected class
        Map<String, Object> selectedClass = classesTable.getSelectionModel().getSelectedItem();
        
        if (selectedClass == null || "No classes assigned".equals(selectedClass.get("className"))) {
            showAlert("No Selection", "Please select a class to view students.");
            return;
        }
        
        String className = (String) selectedClass.get("className");
        System.out.println("✅ Viewing students for class: " + className);
        
        // Get students in this class
        List<Map<String, Object>> students = teacherService.getStudentsInClass(className);
        
        if (students != null && !students.isEmpty()) {
            StringBuilder studentList = new StringBuilder();
            studentList.append("Students in ").append(className).append(":\n\n");
            
            int count = 1;
            for (Map<String, Object> student : students) {
                String studentId = (String) student.get("studentId");
                String firstName = (String) student.get("firstName");
                String lastName = (String) student.get("lastName");
                studentList.append(count).append(". ").append(firstName).append(" ").append(lastName)
                          .append(" (").append(studentId).append(")\n");
                count++;
            }
            
            showAlert("Students in " + className, studentList.toString());
        } else {
            showAlert("No Students", "No students are enrolled in " + className);
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}