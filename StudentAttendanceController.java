package attendance.controller;

import attendance.model.User;
import attendance.service.DatabaseService;
import attendance.service.StudentService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class StudentAttendanceController {
    
    @FXML private Label studentNameLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label studentClassLabel;
    
    @FXML private Label presentCountLabel;
    @FXML private Label absentCountLabel;
    @FXML private Label lateCountLabel;
    @FXML private Label attendanceRateLabel;
    @FXML private Label summaryLabel;
    
    @FXML private ComboBox<String> monthFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> dateColumn;
    @FXML private TableColumn<AttendanceRecord, String> dayColumn;
    @FXML private TableColumn<AttendanceRecord, String> statusColumn;
    @FXML private TableColumn<AttendanceRecord, String> checkInColumn;
    @FXML private TableColumn<AttendanceRecord, String> checkOutColumn;
    @FXML private TableColumn<AttendanceRecord, String> remarksColumn;
    
    private User currentUser;
    private DatabaseService databaseService;
    private int studentDbId;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.databaseService = new DatabaseService();
        loadStudentData();
    }
    
    @FXML
    public void initialize() {
        // Configure table columns
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        checkInColumn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        checkOutColumn.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        remarksColumn.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        
        // Style status column
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
        
        // Setup filter combos
        ObservableList<String> months = FXCollections.observableArrayList(
            "All Months", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        );
        monthFilterCombo.setItems(months);
        monthFilterCombo.getSelectionModel().select(0);
        
        ObservableList<String> statuses = FXCollections.observableArrayList(
            "All Status", "PRESENT", "ABSENT", "LATE"
        );
        statusFilterCombo.setItems(statuses);
        statusFilterCombo.getSelectionModel().select(0);
        
        // Add listeners to filters
        monthFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> loadAttendanceData());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> loadAttendanceData());
    }
    
    private void loadStudentData() {
        if (currentUser == null) return;
        
        try {
            // Get student info
            String sql = "SELECT s.id, s.student_id, s.first_name, s.last_name, s.class_name " +
                        "FROM students s " +
                        "WHERE s.user_id = ?";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    studentDbId = rs.getInt("id");
                    String studentId = rs.getString("student_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String className = rs.getString("class_name");
                    
                    studentNameLabel.setText(firstName + " " + lastName);
                    studentIdLabel.setText("ID: " + studentId);
                    studentClassLabel.setText("Class: " + className);
                    
                    loadAttendanceStats();
                    loadAttendanceData();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading student data: " + e.getMessage());
        }
    }
    
    private void loadAttendanceStats() {
        try {
            String sql = "SELECT " +
                        "COUNT(*) as total_days, " +
                        "SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) as present_days, " +
                        "SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absent_days, " +
                        "SUM(CASE WHEN status = 'LATE' THEN 1 ELSE 0 END) as late_days " +
                        "FROM attendance WHERE student_id = ?";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, studentDbId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    int total = rs.getInt("total_days");
                    int present = rs.getInt("present_days");
                    int absent = rs.getInt("absent_days");
                    int late = rs.getInt("late_days");
                    
                    presentCountLabel.setText(String.valueOf(present));
                    absentCountLabel.setText(String.valueOf(absent));
                    lateCountLabel.setText(String.valueOf(late));
                    
                    if (total > 0) {
                        double rate = ((present + late) * 100.0) / total;
                        attendanceRateLabel.setText(String.format("%.1f%%", rate));
                        
                        String summary = String.format(
                            "You have attended %d out of %d days (%.1f%%). %s",
                            present + late, total, rate,
                            rate >= 90 ? "Excellent attendance! 🎉" :
                            rate >= 75 ? "Good attendance. Keep it up! 👍" :
                            "Try to improve your attendance."
                        );
                        summaryLabel.setText(summary);
                    } else {
                        attendanceRateLabel.setText("0%");
                        summaryLabel.setText("No attendance records found.");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading attendance stats: " + e.getMessage());
        }
    }
    
    private void loadAttendanceData() {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT date, check_in, check_out, status, remarks ")
               .append("FROM attendance ")
               .append("WHERE student_id = ? ");
            
            String selectedMonth = monthFilterCombo.getValue();
            String selectedStatus = statusFilterCombo.getValue();
            
            if (selectedMonth != null && !selectedMonth.equals("All Months")) {
                sql.append("AND strftime('%m', date) = ? ");
            }
            
            if (selectedStatus != null && !selectedStatus.equals("All Status")) {
                sql.append("AND status = ? ");
            }
            
            sql.append("ORDER BY date DESC");
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                
                pstmt.setInt(1, studentDbId);
                int paramIndex = 2;
                
                if (selectedMonth != null && !selectedMonth.equals("All Months")) {
                    int monthNumber = getMonthNumber(selectedMonth);
                    pstmt.setString(paramIndex++, String.format("%02d", monthNumber));
                }
                
                if (selectedStatus != null && !selectedStatus.equals("All Status")) {
                    pstmt.setString(paramIndex, selectedStatus);
                }
                
                ResultSet rs = pstmt.executeQuery();
                ObservableList<AttendanceRecord> records = FXCollections.observableArrayList();
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                
                while (rs.next()) {
                    String dateStr = rs.getString("date");
                    String checkIn = rs.getString("check_in");
                    String checkOut = rs.getString("check_out");
                    String status = rs.getString("status");
                    String remarks = rs.getString("remarks");
                    
                    LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                    String dayOfWeek = date.getDayOfWeek().toString();
                    String formattedDate = date.format(displayFormatter);
                    
                    AttendanceRecord record = new AttendanceRecord(
                        formattedDate,
                        dayOfWeek.substring(0, 1) + dayOfWeek.substring(1).toLowerCase(),
                        status != null ? status : "ABSENT",
                        checkIn != null ? checkIn : "-",
                        checkOut != null ? checkOut : "-",
                        remarks != null ? remarks : ""
                    );
                    
                    records.add(record);
                }
                
                attendanceTable.setItems(records);
            }
        } catch (SQLException e) {
            System.err.println("Error loading attendance data: " + e.getMessage());
        }
    }
    
    private int getMonthNumber(String monthName) {
        Map<String, Integer> months = new HashMap<>();
        months.put("January", 1);
        months.put("February", 2);
        months.put("March", 3);
        months.put("April", 4);
        months.put("May", 5);
        months.put("June", 6);
        months.put("July", 7);
        months.put("August", 8);
        months.put("September", 9);
        months.put("October", 10);
        months.put("November", 11);
        months.put("December", 12);
        return months.getOrDefault(monthName, 1);
    }
    
    @FXML
    private void handleRefresh() {
        loadStudentData();
    }
    
    @FXML
    private void handleExport() {
        showAlert("Export", "Export feature will be available soon!");
    }
    
    @FXML
    private void handleRequestLeave() {
        showAlert("Request Leave", "Leave request feature will be available soon!");
    }
    
    @FXML
    private void handleViewAnalytics() {
        showAlert("Analytics", "Attendance analytics will be available soon!");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class for table data
    public static class AttendanceRecord {
        private String date;
        private String day;
        private String status;
        private String checkIn;
        private String checkOut;
        private String remarks;
        
        public AttendanceRecord(String date, String day, String status, String checkIn, String checkOut, String remarks) {
            this.date = date;
            this.day = day;
            this.status = status;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.remarks = remarks;
        }
        
        public String getDate() { return date; }
        public String getDay() { return day; }
        public String getStatus() { return status; }
        public String getCheckIn() { return checkIn; }
        public String getCheckOut() { return checkOut; }
        public String getRemarks() { return remarks; }
    }
}