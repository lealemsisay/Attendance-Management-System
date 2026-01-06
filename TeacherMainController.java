package attendance.controller;

import attendance.model.User;
import attendance.service.DatabaseService;
import attendance.service.StudentService;
import attendance.service.TeacherService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class TeacherMainController implements Initializable {
    
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Label userNameLabel;
    
    private User currentUser;
    private TeacherService teacherService;
    private DatabaseService databaseService;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && userNameLabel != null) {
            userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
        }
        teacherService = new TeacherService();
        databaseService = new DatabaseService();
        loadHome();
    }
    
    @FXML
    private void loadHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/teacher_home.fxml"));
            Parent view = loader.load();
            
            TeacherDashboardController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            // initialize() will be called automatically by JavaFX
            
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
            System.err.println("Error loading teacher home: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not load dashboard.");
        }
    }
    
    @FXML
    private void showTakeAttendance() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/take_attendance.fxml"));
            Parent view = loader.load();
            
            // Force CSS loading
            String css = getClass().getResource("/attendance/style/teacher_dashboard.css").toExternalForm();
            System.out.println("Loading CSS from: " + css);
            if (!view.getStylesheets().contains(css)) {
                view.getStylesheets().add(css);
            }
            
            TakeAttendanceController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
            System.err.println("Error loading take attendance: " + e.getMessage());
            e.printStackTrace();
            showPlaceholder("Take Attendance", "Attendance marking system will be available soon.");
        }
    }

    @FXML
    private void showViewAttendance() {
        try {
            // Note: Make sure the filename is correct - should be view_attendance.fxml not view_attendace.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/view_attendance.fxml"));
            Parent view = loader.load();
            
            ViewAttendanceController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
            System.err.println("Error loading view attendance: " + e.getMessage());
            e.printStackTrace();
            showPlaceholder("View Attendance", "Attendance viewing system will be available soon.");
        }
    }
    @FXML
    private void showAssignedClasses() {
        loadAssignedClassesView();
    }
    
    private void loadAssignedClassesView() {
        try {
            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<html><head><style>")
                     .append("body { font-family: Arial, sans-serif; padding: 20px; }")
                     .append(".class-card { background: white; border-radius: 10px; padding: 20px; margin: 10px 0; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }")
                     .append(".class-title { font-size: 18px; font-weight: bold; color: #333; }")
                     .append(".class-details { color: #666; font-size: 14px; }")
                     .append("</style></head><body>")
                     .append("<h2>📚 Your Assigned Classes</h2>");
            
            // Get teacher's assigned classes from database
            String sql = "SELECT * FROM classes WHERE teacher_id = ?";
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                
                int classCount = 0;
                while (rs.next()) {
                    classCount++;
                    htmlContent.append("<div class='class-card'>")
                              .append("<div class='class-title'>").append(rs.getString("class_name")).append("</div>")
                              .append("<div class='class-details'>")
                              .append("Room: ").append(rs.getString("room_number")).append(" | ")
                              .append("Schedule: ").append(rs.getString("schedule")).append(" | ")
                              .append("Students: ").append(rs.getInt("student_count"))
                              .append("</div>")
                              .append("</div>");
                }
                
                if (classCount == 0) {
                    htmlContent.append("<p>No classes assigned yet.</p>");
                }
                
            } catch (SQLException e) {
                htmlContent.append("<p>Error loading classes: ").append(e.getMessage()).append("</p>");
            }
            
            htmlContent.append("</body></html>");
            
            showHtmlView("Assigned Classes", htmlContent.toString());
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load assigned classes: " + e.getMessage());
        }
    }
    
    @FXML
    private void showStudentRecords() {
        loadStudentRecordsView();
    }
    
    private void loadStudentRecordsView() {
        try {
            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<html><head><style>")
                     .append("body { font-family: Arial, sans-serif; padding: 20px; }")
                     .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
                     .append("th { background: #f5f5f5; padding: 10px; text-align: left; }")
                     .append("td { padding: 10px; border-bottom: 1px solid #eee; }")
                     .append("</style></head><body>")
                     .append("<h2>👨‍🎓 Student Records</h2>");
            
            // Get students from database
            String sql = "SELECT s.id, s.student_id, s.first_name, s.last_name, s.class_name, "
                       + "COUNT(a.id) as attendance_count, "
                       + "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_count "
                       + "FROM students s "
                       + "LEFT JOIN attendance a ON s.id = a.student_id "
                       + "WHERE s.class_name IN (SELECT class_name FROM classes WHERE teacher_id = ?) "
                       + "GROUP BY s.id "
                       + "ORDER BY s.last_name, s.first_name";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                
                htmlContent.append("<table>")
                          .append("<tr><th>ID</th><th>Name</th><th>Class</th><th>Attendance</th><th>Rate</th></tr>");
                
                int studentCount = 0;
                while (rs.next()) {
                    studentCount++;
                    int totalAttendance = rs.getInt("attendance_count");
                    int presentCount = rs.getInt("present_count");
                    double attendanceRate = totalAttendance > 0 ? (presentCount * 100.0 / totalAttendance) : 0;
                    
                    htmlContent.append("<tr>")
                              .append("<td>").append(rs.getString("student_id")).append("</td>")
                              .append("<td>").append(rs.getString("first_name")).append(" ").append(rs.getString("last_name")).append("</td>")
                              .append("<td>").append(rs.getString("class_name")).append("</td>")
                              .append("<td>").append(presentCount).append("/").append(totalAttendance).append("</td>")
                              .append("<td>").append(String.format("%.1f", attendanceRate)).append("%</td>")
                              .append("</tr>");
                }
                
                htmlContent.append("</table>");
                
                if (studentCount == 0) {
                    htmlContent.append("<p>No student records found.</p>");
                }
                
            } catch (SQLException e) {
                htmlContent.append("<p>Error loading student records: ").append(e.getMessage()).append("</p>");
            }
            
            htmlContent.append("</body></html>");
            
            showHtmlView("Student Records", htmlContent.toString());
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load student records: " + e.getMessage());
        }
    }
    
    @FXML
    private void showGenerateReports() {
        showReportGenerationView();
    }
    
    private void showReportGenerationView() {
        try {
            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<html><head><style>")
                     .append("body { font-family: Arial, sans-serif; padding: 20px; }")
                     .append(".report-form { background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }")
                     .append(".form-group { margin-bottom: 15px; }")
                     .append("label { display: block; margin-bottom: 5px; font-weight: bold; }")
                     .append("select, input { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }")
                     .append("button { background: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }")
                     .append("</style></head><body>")
                     .append("<h2>📊 Generate Reports</h2>")
                     .append("<div class='report-form'>")
                     .append("<div class='form-group'>")
                     .append("<label>Report Type:</label>")
                     .append("<select id='reportType'>")
                     .append("<option value='attendance'>Attendance Report</option>")
                     .append("<option value='performance'>Student Performance</option>")
                     .append("<option value='summary'>Class Summary</option>")
                     .append("</select>")
                     .append("</div>")
                     .append("<div class='form-group'>")
                     .append("<label>Date Range:</label>")
                     .append("<input type='date' id='startDate'> to <input type='date' id='endDate'>")
                     .append("</div>")
                     .append("<div class='form-group'>")
                     .append("<label>Class:</label>")
                     .append("<select id='classSelect'>");
            
            // Load teacher's classes
            String sql = "SELECT class_name FROM classes WHERE teacher_id = ?";
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                
                htmlContent.append("<option value='all'>All Classes</option>");
                while (rs.next()) {
                    htmlContent.append("<option value='").append(rs.getString("class_name")).append("'>")
                              .append(rs.getString("class_name")).append("</option>");
                }
                
            } catch (SQLException e) {
                htmlContent.append("<option value='all'>All Classes</option>");
            }
            
            htmlContent.append("</select>")
                      .append("</div>")
                      .append("<div class='form-group'>")
                      .append("<label>Format:</label>")
                      .append("<select id='format'>")
                      .append("<option value='html'>HTML</option>")
                      .append("<option value='pdf'>PDF (Coming Soon)</option>")
                      .append("<option value='excel'>Excel (Coming Soon)</option>")
                      .append("</select>")
                      .append("</div>")
                      .append("<button onclick='generateReport()'>Generate Report</button>")
                      .append("</div>")
                      .append("<script>")
                      .append("function generateReport() {")
                      .append("  alert('Report generation feature will be implemented soon!');")
                      .append("}")
                      .append("</script>")
                      .append("</body></html>");
            
            showHtmlView("Generate Reports", htmlContent.toString());
            
        } catch (Exception e) {
            showAlert("Error", "Failed to load report generator: " + e.getMessage());
        }
    }
    
    private void showHtmlView(String title, String htmlContent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/html_viewer.fxml"));
            Parent view = loader.load();
            
            // If you have an HtmlViewerController, set the content
            contentArea.getChildren().setAll(view);
            
            // For now, show placeholder with HTML content
            showPlaceholder(title, htmlContent);
            
        } catch (Exception e) {
            showPlaceholder(title, htmlContent);
        }
    }
    
    private void showPlaceholder(String title, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/placeholder.fxml"));
            Parent view = loader.load();
            
            // If you have a PlaceholderController, you can set the title and message here
            contentArea.getChildren().setAll(view);
            
        } catch (Exception e) {
            // Fallback to simple label
            javafx.scene.control.Label label = new javafx.scene.control.Label(title + "\n\n" + message);
            label.setStyle("-fx-padding: 20px; -fx-font-size: 14px;");
            contentArea.getChildren().setAll(label);
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
        try {
            attendance.Main.showLoginPage();
        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
            showAlert("Logout Error", "Failed to logout: " + e.getMessage());
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Home will be loaded when setCurrentUser is called
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Helper method to get teacher statistics
    public int[] getTeacherStats() {
        int[] stats = new int[3]; // classes, students, attendance rate
        
        try {
            // Get number of assigned classes
            String classSql = "SELECT COUNT(*) as class_count FROM classes WHERE teacher_id = ?";
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(classSql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats[0] = rs.getInt("class_count");
                }
            }
            
            // Get number of students in assigned classes
            String studentSql = "SELECT COUNT(DISTINCT s.id) as student_count "
                              + "FROM students s "
                              + "JOIN classes c ON s.class_name = c.class_name "
                              + "WHERE c.teacher_id = ?";
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(studentSql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats[1] = rs.getInt("student_count");
                }
            }
            
            // Calculate average attendance rate
            String attendanceSql = "SELECT AVG(CASE WHEN a.status = 'PRESENT' THEN 100 "
                                 + "WHEN a.status = 'LATE' THEN 80 "
                                 + "ELSE 0 END) as avg_rate "
                                 + "FROM attendance a "
                                 + "JOIN students s ON a.student_id = s.id "
                                 + "JOIN classes c ON s.class_name = c.class_name "
                                 + "WHERE c.teacher_id = ? AND date(a.date) = date('now')";
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(attendanceSql)) {
                
                pstmt.setInt(1, currentUser.getId());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats[2] = (int) rs.getDouble("avg_rate");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting teacher stats: " + e.getMessage());
        }
        
        return stats;
    }
}