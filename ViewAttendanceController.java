package attendance.controller;

import attendance.model.User;
import attendance.service.DatabaseService;
import attendance.service.NotificationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ViewAttendanceController {
    
    @FXML private ComboBox<String> classComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button generateReportButton;
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> dateColumn;
    @FXML private TableColumn<AttendanceRecord, String> studentIdColumn;
    @FXML private TableColumn<AttendanceRecord, String> nameColumn;
    @FXML private TableColumn<AttendanceRecord, String> statusColumn;
    @FXML private TableColumn<AttendanceRecord, String> remarksColumn;
    
    private User currentUser;
    private DatabaseService databaseService;
    private NotificationService notificationService;
    private List<AttendanceRecord> currentReportData = new ArrayList<>();
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.databaseService = new DatabaseService();
        this.notificationService = new NotificationService();
        
        // Set default dates (last 7 days)
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(7));
        
        // Load teacher's classes
        loadTeacherClasses();
    }
    
    @FXML
    public void initialize() {
        // Configure table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        remarksColumn.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        // Style status column cells
        statusColumn.setCellFactory(column -> new TableCell<AttendanceRecord, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "PRESENT":
                            setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 2px 6px;");
                            break;
                        case "ABSENT":
                            setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 2px 6px;");
                            break;
                        case "LATE":
                            setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 2px 6px;");
                            break;
                        default:
                            setStyle("");
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
                classes.add("All Classes"); // Add option for all classes
                while (rs.next()) {
                    classes.add(rs.getString("class_name"));
                }
                
                classComboBox.setItems(classes);
                if (!classes.isEmpty()) {
                    classComboBox.getSelectionModel().select(0);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading classes: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleGenerateReport() {
        String selectedClass = classComboBox.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        
        if (selectedClass == null || fromDate == null || toDate == null) {
            showAlert("Error", "Please select class and date range");
            return;
        }
        
        if (fromDate.isAfter(toDate)) {
            showAlert("Error", "From date must be before or equal to To date");
            return;
        }
        
        loadAttendanceData(selectedClass, fromDate.toString(), toDate.toString());
    }
    
    private void loadAttendanceData(String className, String fromDate, String toDate) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT a.date, s.student_id, s.first_name, s.last_name, a.status, a.remarks ")
               .append("FROM attendance a ")
               .append("JOIN students s ON a.student_id = s.id ")
               .append("WHERE a.date BETWEEN ? AND ? ");
            
            if (!className.equals("All Classes")) {
                sql.append("AND s.class_name = ? ");
            }
            
            sql.append("ORDER BY a.date DESC, s.last_name, s.first_name");
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                
                pstmt.setString(1, fromDate);
                pstmt.setString(2, toDate);
                
                if (!className.equals("All Classes")) {
                    pstmt.setString(3, className);
                }
                
                ResultSet rs = pstmt.executeQuery();
                
                currentReportData.clear();
                ObservableList<AttendanceRecord> records = FXCollections.observableArrayList();
                while (rs.next()) {
                    String date = rs.getString("date");
                    String studentId = rs.getString("student_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String status = rs.getString("status");
                    String remarks = rs.getString("remarks");
                    
                    AttendanceRecord record = new AttendanceRecord(
                        date,
                        studentId,
                        firstName + " " + lastName,
                        status != null ? status : "ABSENT",
                        remarks != null ? remarks : ""
                    );
                    
                    records.add(record);
                    currentReportData.add(record);
                }
                
                attendanceTable.setItems(records);
                
                if (records.isEmpty()) {
                    showAlert("Info", "No attendance records found for the selected criteria.");
                } else {
                    showAlert("Success", "Report generated successfully! Found " + records.size() + " records.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading attendance data: " + e.getMessage());
            showAlert("Error", "Failed to load attendance data: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleViewReport() {
        if (currentReportData.isEmpty()) {
            showAlert("Error", "Please generate a report first using 'Generate Report' button.");
            return;
        }
        
        // Calculate summary statistics
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;
        int totalCount = currentReportData.size();
        
        for (AttendanceRecord record : currentReportData) {
            switch (record.getStatus()) {
                case "PRESENT":
                    presentCount++;
                    break;
                case "ABSENT":
                    absentCount++;
                    break;
                case "LATE":
                    lateCount++;
                    break;
            }
        }
        
        double attendanceRate = totalCount > 0 ? (presentCount * 100.0) / totalCount : 0;
        
        // Display detailed report
        String report = String.format(
            "ATTENDANCE REPORT SUMMARY\n" +
            "==========================\n" +
            "Class: %s\n" +
            "Date Range: %s to %s\n" +
            "Total Records: %d\n" +
            "Present: %d (%.1f%%)\n" +
            "Absent: %d (%.1f%%)\n" +
            "Late: %d (%.1f%%)\n" +
            "Attendance Rate: %.1f%%\n" +
            "\nGenerated by: %s\n" +
            "Date: %s",
            classComboBox.getValue(),
            fromDatePicker.getValue(),
            toDatePicker.getValue(),
            totalCount,
            presentCount, (presentCount * 100.0) / totalCount,
            absentCount, (absentCount * 100.0) / totalCount,
            lateCount, (lateCount * 100.0) / totalCount,
            attendanceRate,
            currentUser.getFullName(), // Now works with the updated User class
            LocalDate.now()
        );
        
        TextArea textArea = new TextArea(report);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(600, 400);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Attendance Report");
        alert.setHeaderText("Detailed Attendance Report");
        alert.getDialogPane().setContent(scrollPane);
        alert.showAndWait();
    }
    
    @FXML
    private void handlePrintReport() {
        if (currentReportData.isEmpty()) {
            showAlert("Error", "No data to print. Please generate a report first.");
            return;
        }
        
        showAlert("Print", "Print functionality will be implemented in the next update.");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void handleSendToAdmin() {
        if (currentReportData.isEmpty()) {
            showAlert("Error", "Please generate a report first using 'Generate Report' button.");
            return;
        }
        
        String selectedClass = classComboBox.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        
        if (selectedClass == null || fromDate == null || toDate == null) {
            showAlert("Error", "Please select class and date range.");
            return;
        }
        
        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Save the report to database
            String reportSql = "INSERT INTO attendance_reports (teacher_id, class_name, from_date, to_date, " +
                              "submitted_date, report_data, status) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            int reportId;
            try (PreparedStatement pstmt = conn.prepareStatement(reportSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, currentUser.getId());
                pstmt.setString(2, selectedClass);
                pstmt.setString(3, fromDate.toString());
                pstmt.setString(4, toDate.toString());
                pstmt.setString(5, LocalDate.now().toString());
                
                // Convert report data to JSON string
                StringBuilder reportData = new StringBuilder();
                reportData.append("Total Records: ").append(currentReportData.size()).append("\n");
                reportData.append("Generated at: ").append(LocalDateTime.now()).append("\n");
                reportData.append("Class: ").append(selectedClass).append("\n");
                reportData.append("Date Range: ").append(fromDate).append(" to ").append(toDate).append("\n\n");
                
                int presentCount = 0, absentCount = 0, lateCount = 0;
                for (AttendanceRecord record : currentReportData) {
                    switch (record.getStatus()) {
                        case "PRESENT": presentCount++; break;
                        case "ABSENT": absentCount++; break;
                        case "LATE": lateCount++; break;
                    }
                }
                
                reportData.append("Summary:\n");
                reportData.append("- Present: ").append(presentCount).append("\n");
                reportData.append("- Absent: ").append(absentCount).append("\n");
                reportData.append("- Late: ").append(lateCount).append("\n");
                reportData.append("- Total: ").append(currentReportData.size()).append("\n");
                
                pstmt.setString(6, reportData.toString());
                pstmt.setString(7, "SUBMITTED");
                
                pstmt.executeUpdate();
                
                // Get the generated report ID
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    reportId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to get report ID");
                }
            }
            
            // 2. Send notification to admin using NotificationService
            notificationService.sendAttendanceReportToAdmin(
                currentUser.getId(),
                currentUser.getFullName(), // Now works with the updated User class
                selectedClass,
                fromDate.toString(),
                toDate.toString(),
                reportId
            );
            
            // 3. Notify students
            if (!selectedClass.equals("All Classes")) {
                notificationService.notifyStudentsAboutAttendance(
                    selectedClass,
                    fromDate.toString(),
                    toDate.toString()
                );
            }
            
            conn.commit();
            
            showAlert("Success ✅", 
                "Attendance report has been successfully submitted!\n\n" +
                "✅ Sent to admin for review\n" +
                "✅ Notified students about their attendance\n" +
                "✅ Report ID: #" + reportId + "\n" +
                "✅ Class: " + selectedClass + "\n" +
                "✅ Period: " + fromDate + " to " + toDate + "\n\n" +
                "Students can now view their attendance in their dashboard."
            );
            
        } catch (SQLException e) {
            System.err.println("❌ Error sending report: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to send report: " + e.getMessage());
        }
    }
    
    // Inner class for table data
    public static class AttendanceRecord {
        private String date;
        private String studentId;
        private String studentName;
        private String status;
        private String remarks;
        
        public AttendanceRecord(String date, String studentId, String studentName, String status, String remarks) {
            this.date = date;
            this.studentId = studentId;
            this.studentName = studentName;
            this.status = status;
            this.remarks = remarks;
        }
        
        public String getDate() { return date; }
        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getStatus() { return status; }
        public String getRemarks() { return remarks; }
    }
}