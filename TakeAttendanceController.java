package attendance.controller;

import attendance.model.User;
import attendance.service.DatabaseService;
import attendance.service.TeacherService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TakeAttendanceController {
    
    @FXML private ComboBox<String> classComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Button startAttendanceButton;
    @FXML private Label studentCountLabel;
    @FXML private Label summaryLabel;
    
    @FXML private TableView<StudentAttendance> attendanceTable;
    @FXML private TableColumn<StudentAttendance, String> studentIdColumn;
    @FXML private TableColumn<StudentAttendance, String> nameColumn;
    @FXML private TableColumn<StudentAttendance, String> statusColumn;
    @FXML private TableColumn<StudentAttendance, String> remarksColumn;
    
    private User currentUser;
    private DatabaseService databaseService;
    private TeacherService teacherService;
    private Map<Integer, ComboBox<String>> statusComboBoxes = new HashMap<>();
    private Map<Integer, TextField> remarksFields = new HashMap<>();
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.databaseService = new DatabaseService();
        this.teacherService = new TeacherService();
        
        // Set default date to today
        datePicker.setValue(LocalDate.now());
        
        // Load teacher's classes
        loadTeacherClasses();
    }
    
    @FXML
    public void initialize() {
        // Configure table columns
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        remarksColumn.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        // Set up cell factory for status column
        statusColumn.setCellFactory(column -> new TableCell<StudentAttendance, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    // Remove all status styles first
                    getStyleClass().removeAll("status-present", "status-absent");
                    
                    // Add appropriate status style
                    switch (status) {
                        case "PRESENT":
                            getStyleClass().add("status-present");
                            break;
                        case "ABSENT":
                            getStyleClass().add("status-absent");
                            break;
                    }
                }
            }
        });
        
        // Style remarks column
        remarksColumn.setCellFactory(column -> new TableCell<StudentAttendance, String>() {
            private final TextField textField = new TextField();
            
            @Override
            protected void updateItem(String remarks, boolean empty) {
                super.updateItem(remarks, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    StudentAttendance student = getTableView().getItems().get(getIndex());
                    if (student != null) {
                        textField.setText(remarks);
                        textField.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 4px; -fx-padding: 5px;");
                        textField.textProperty().addListener((obs, oldVal, newVal) -> {
                            student.setRemarks(newVal);
                        });
                        remarksFields.put(student.getStudentDbId(), textField);
                        setGraphic(textField);
                    }
                }
            }
        });
    }
    
    private void loadTeacherClasses() {
        if (currentUser == null) return;
        
        try {
            String sql = "SELECT class_name FROM classes WHERE teacher_id = ? ORDER BY class_name";
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                
                ObservableList<String> classes = FXCollections.observableArrayList();
                while (rs.next()) {
                    classes.add(rs.getString("class_name"));
                }
                
                classComboBox.setItems(classes);
                if (!classes.isEmpty()) {
                    classComboBox.getSelectionModel().select(0);
                    // Auto-load students for first class
                    handleStartAttendance();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading classes: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleStartAttendance() {
        String selectedClass = classComboBox.getValue();
        LocalDate selectedDate = datePicker.getValue();
        
        if (selectedClass == null || selectedDate == null) {
            showAlert("Error", "Please select both class and date");
            return;
        }
        
        loadStudentsForAttendance(selectedClass, selectedDate.toString());
    }
    
    private void loadStudentsForAttendance(String className, String date) {
        try {
            // Get students in the selected class
            String sql = "SELECT s.id, s.student_id, s.first_name, s.last_name, a.status as current_status, a.remarks "
                       + "FROM students s "
                       + "LEFT JOIN attendance a ON s.id = a.student_id AND a.date = ? "
                       + "WHERE s.class_name = ? "
                       + "ORDER BY s.last_name, s.first_name";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, date);
                pstmt.setString(2, className);
                ResultSet rs = pstmt.executeQuery();
                
                ObservableList<StudentAttendance> students = FXCollections.observableArrayList();
                statusComboBoxes.clear();
                remarksFields.clear();
                
                int studentCount = 0;
                int presentCount = 0;
                int absentCount = 0;
                
                while (rs.next()) {
                    studentCount++;
                    int studentId = rs.getInt("id");
                    String studentIdStr = rs.getString("student_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String currentStatus = rs.getString("current_status");
                    String currentRemarks = rs.getString("remarks");
                    
                    // Count statuses
                    if ("PRESENT".equals(currentStatus)) presentCount++;
                    else if ("ABSENT".equals(currentStatus)) absentCount++;
                    
                    // Create StudentAttendance object
                    StudentAttendance student = new StudentAttendance(
                        studentId,
                        studentIdStr,
                        firstName + " " + lastName,
                        currentStatus != null ? currentStatus : "PRESENT",
                        currentRemarks != null ? currentRemarks : ""
                    );
                    
                    students.add(student);
                }
                
                attendanceTable.setItems(students);
                studentCountLabel.setText(studentCount + " students loaded");
                
                // Update summary
                String summary = String.format(
                    "Total: %d students | Present: %d | Absent: %d",
                    studentCount, presentCount, absentCount
                );
                summaryLabel.setText(summary);
                
            }
        } catch (SQLException e) {
            System.err.println("Error loading students: " + e.getMessage());
            showAlert("Error", "Failed to load students: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleMarkAllPresent() {
        for (StudentAttendance student : attendanceTable.getItems()) {
            student.setStatus("PRESENT");
        }
        attendanceTable.refresh();
        updateSummary();
    }
    
    @FXML
    private void handleMarkAllAbsent() {
        for (StudentAttendance student : attendanceTable.getItems()) {
            student.setStatus("ABSENT");
        }
        attendanceTable.refresh();
        updateSummary();
    }
    
    @FXML
    private void handleSaveAttendance() {
        String selectedClass = classComboBox.getValue();
        LocalDate selectedDate = datePicker.getValue();
        
        if (selectedClass == null || selectedDate == null) {
            showAlert("Error", "Please select both class and date");
            return;
        }
        
        int savedCount = 0;
        int errorCount = 0;
        String dateStr = selectedDate.toString();
        
        for (StudentAttendance student : attendanceTable.getItems()) {
            try {
                boolean success = teacherService.markAttendance(
                    student.getStudentDbId(),
                    dateStr,
                    student.getStatus(),
                    student.getRemarks()
                );
                
                if (success) {
                    savedCount++;
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                errorCount++;
                System.err.println("Error saving attendance for student " + student.getStudentId() + ": " + e.getMessage());
            }
        }
        
        String message;
        if (errorCount == 0) {
            message = "Attendance successfully saved for " + savedCount + " students!";
            showAlert("Success", message);
        } else {
            message = "Saved " + savedCount + " records, " + errorCount + " errors occurred.";
            showAlert("Warning", message);
        }
        
        // Reload to reflect saved status
        handleStartAttendance();
    }
    
    @FXML
    private void handleSubmitAttendance() {
        handleSaveAttendance();
    }
    
    @FXML
    private void handlePrintReport() {
        showAlert("Print", "Print feature will be available soon!");
    }
    
    private void updateSummary() {
        int presentCount = 0;
        int absentCount = 0;
        
        for (StudentAttendance student : attendanceTable.getItems()) {
            if ("PRESENT".equals(student.getStatus())) {
                presentCount++;
            } else if ("ABSENT".equals(student.getStatus())) {
                absentCount++;
            }
        }
        
        int total = attendanceTable.getItems().size();
        String summary = String.format(
            "Total: %d students | Present: %d | Absent: %d",
            total, presentCount, absentCount
        );
        summaryLabel.setText(summary);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class for table data
    public static class StudentAttendance {
        private int studentDbId;
        private String studentId;
        private String fullName;
        private String status;
        private String remarks;
        
        public StudentAttendance(int studentDbId, String studentId, String fullName, String status, String remarks) {
            this.studentDbId = studentDbId;
            this.studentId = studentId;
            this.fullName = fullName;
            this.status = status;
            this.remarks = remarks;
        }
        
        public int getStudentDbId() { return studentDbId; }
        public String getStudentId() { return studentId; }
        public String getFullName() { return fullName; }
        public String getStatus() { return status; }
        public String getRemarks() { return remarks; }
        
        public void setStatus(String status) { this.status = status; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
    
    
    
}