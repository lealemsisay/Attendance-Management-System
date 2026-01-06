package attendance.controller;

import attendance.model.User;
import attendance.service.ClassService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageClassesController {
    
    // Table Components
    @FXML private TableView<ClassData> classesTable;
    @FXML private TableColumn<ClassData, String> classNameColumn;
    @FXML private TableColumn<ClassData, String> roomColumn;
    @FXML private TableColumn<ClassData, String> scheduleColumn;
    @FXML private TableColumn<ClassData, String> teacherColumn;
    @FXML private TableColumn<ClassData, Integer> studentCountColumn;
    
    // Form Components
    @FXML private TextField classNameField;
    @FXML private TextField roomField;
    @FXML private TextField scheduleField;
    @FXML private ComboBox<String> teacherComboBox;
    
    // Buttons and Menu Items
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    @FXML private Button quickAddButton;
    @FXML private Button quickUpdateButton;
    @FXML private Button quickClearButton;
    @FXML private Button exportButton;
    @FXML private Button assignTeacherButton;
    
    // Menu Items
    @FXML private MenuButton actionsMenuButton;
    @FXML private MenuItem addClassMenuItem;
    @FXML private MenuItem editClassMenuItem;
    @FXML private MenuItem updateClassMenuItem;
    @FXML private MenuItem deleteClassMenuItem;
    @FXML private MenuItem clearFormMenuItem;
    @FXML private MenuItem exportDataMenuItem;
    @FXML private MenuItem importDataMenuItem;
    
    // Labels
    @FXML private Label formStatusLabel;
    @FXML private Label tableStatusLabel;
    @FXML private Label selectedClassLabel;
    @FXML private Label statusLabel;
    
    private ClassService classService;
    private User currentUser;
    private ObservableList<ClassData> classDataList;
    private ObservableList<String> teacherList;
    private Map<String, Integer> teacherIdMap;
    private Map<Integer, String> idToTeacherMap;
    
    private int selectedClassId = -1;
    private String originalClassName = "";
    private String originalRoom = "";
    private String originalSchedule = "";
    private String originalTeacher = "";
    private Integer originalTeacherId = null;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ ManageClassesController: User set - " + 
                          (user != null ? user.getFirstName() : "null"));
    }
    
    @FXML
    public void initialize() {
        System.out.println("✅ ManageClassesController.initialize() called");
        try {
            classService = new ClassService();
            
            // Initialize maps
            teacherIdMap = new HashMap<>();
            idToTeacherMap = new HashMap<>();
            
            // Initialize table columns
            classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
            roomColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
            scheduleColumn.setCellValueFactory(new PropertyValueFactory<>("schedule"));
            teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
            studentCountColumn.setCellValueFactory(new PropertyValueFactory<>("studentCount"));
            
            // Load initial data
            loadClassData();
            loadTeacherData();
            
            // Set up table selection listener
            classesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    handleTableSelection(newSelection);
                } else {
                    clearSelection();
                }
            });
            
            // Set up button actions
            refreshButton.setOnAction(e -> handleRefresh());
            backButton.setOnAction(e -> handleBack());
            quickAddButton.setOnAction(e -> handleAddClass());
            quickUpdateButton.setOnAction(e -> handleUpdateClass());
            quickClearButton.setOnAction(e -> handleClearForm());
            exportButton.setOnAction(e -> handleExportTable());
            
            // Set up Assign Teacher button action
            if (assignTeacherButton != null) {
                assignTeacherButton.setOnAction(e -> handleAssignTeacher());
                assignTeacherButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
            }
            
            // Set up form field listeners to track changes
            setupFormChangeListeners();
            
            // Disable update buttons initially
            setUpdateButtonsDisabled(true);
            
            updateStatus("Ready");
            System.out.println("✅ ManageClassesController initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Error in ManageClassesController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadClassData() {
        try {
            classDataList = FXCollections.observableArrayList();
            List<Map<String, Object>> classes = classService.getAllClasses();
            
            if (classes != null && !classes.isEmpty()) {
                for (Map<String, Object> cls : classes) {
                    Integer id = (Integer) cls.get("id");
                    String className = (String) cls.get("className");
                    String room = (String) cls.get("room");
                    String schedule = (String) cls.get("schedule");
                    
                    Object studentCountObj = cls.get("studentCount");
                    int studentCount = (studentCountObj instanceof Integer) ? (Integer) studentCountObj : 0;
                    
                    String teacherName = (String) cls.get("teacherName");
                    if (teacherName == null || teacherName.isEmpty()) {
                        teacherName = "No Teacher Assigned";
                    }
                    
                    Integer teacherId = (Integer) cls.get("teacherId");
                    
                    ClassData data = new ClassData(
                        id != null ? id : 0,
                        className != null ? className : "",
                        room != null ? room : "",
                        schedule != null ? schedule : "",
                        studentCount,
                        teacherName,
                        teacherId
                    );
                    classDataList.add(data);
                }
                System.out.println("✅ Loaded " + classDataList.size() + " classes");
            } else {
                System.out.println("ℹ️ No classes found in database");
            }
            
            classesTable.setItems(classDataList);
            tableStatusLabel.setText("Total: " + classDataList.size() + " classes");
        } catch (Exception e) {
            System.err.println("❌ Error loading class data: " + e.getMessage());
            e.printStackTrace();
            tableStatusLabel.setText("Error loading classes");
        }
    }
    
    private void loadTeacherData() {
        try {
            teacherList = FXCollections.observableArrayList();
            teacherIdMap.clear();
            idToTeacherMap.clear();
            
            List<Map<String, Object>> teachers = classService.getAllTeachers();
            if (teachers != null && !teachers.isEmpty()) {
                for (Map<String, Object> teacher : teachers) {
                    Integer id = (Integer) teacher.get("id");
                    String name = (String) teacher.get("name");
                    
                    if (id != null && name != null && !name.isEmpty()) {
                        teacherList.add(name);
                        teacherIdMap.put(name, id);
                        idToTeacherMap.put(id, name);
                        System.out.println("   Teacher: " + name + " (ID: " + id + ")");
                    }
                }
            } else {
                System.out.println("ℹ️ No teachers found in database");
            }
            
            // Add "No Teacher Assigned" option at the beginning
            teacherList.add(0, "No Teacher Assigned");
            teacherIdMap.put("No Teacher Assigned", null);
            
            teacherComboBox.setItems(teacherList);
            
            if (!teacherList.isEmpty()) {
                teacherComboBox.getSelectionModel().select(0); // Select "No Teacher Assigned" by default
            }
            
            System.out.println("✅ Loaded " + teacherList.size() + " teachers for combo box");
        } catch (Exception e) {
            System.err.println("❌ Error loading teacher data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Refreshing data...");
        loadClassData();
        loadTeacherData();
        clearForm();
        updateStatus("Data refreshed successfully");
    }
    
    @FXML
    private void handleAddClass() {
        System.out.println("=== handleAddClass() called ===");
        try {
            String className = classNameField.getText() != null ? classNameField.getText().trim() : "";
            String roomNumber = roomField.getText() != null ? roomField.getText().trim() : "";
            String schedule = scheduleField.getText() != null ? scheduleField.getText().trim() : "";
            String selectedTeacher = teacherComboBox.getValue();
            
            System.out.println("Form values:");
            System.out.println("  Class Name: '" + className + "'");
            System.out.println("  Room: '" + roomNumber + "'");
            System.out.println("  Schedule: '" + schedule + "'");
            System.out.println("  Teacher: '" + selectedTeacher + "'");
            
            // Validation
            if (className.isEmpty()) {
                showAlert("Error", "Class name is required.");
                classNameField.requestFocus();
                return;
            }
            
            if (roomNumber.isEmpty()) {
                showAlert("Error", "Room number is required.");
                roomField.requestFocus();
                return;
            }
            
            if (schedule.isEmpty()) {
                showAlert("Error", "Schedule is required.");
                scheduleField.requestFocus();
                return;
            }
            
            if (selectedTeacher == null || selectedTeacher.isEmpty()) {
                showAlert("Error", "Please select a teacher.");
                teacherComboBox.requestFocus();
                return;
            }
            
            Integer teacherId = teacherIdMap.get(selectedTeacher);
            System.out.println("Selected Teacher ID: " + teacherId);
            
            // Check if class name already exists
            if (classService.classNameExists(className)) {
                showAlert("Error", "Class name '" + className + "' already exists. Please use a different name.");
                return;
            }
            
            System.out.println("➕ Adding class: " + className + " with teacher ID: " + teacherId);
            boolean success = classService.addClass(className, roomNumber, schedule, teacherId);
            if (success) {
                showAlert("Success", "Class '" + className + "' added successfully.");
                clearForm();
                loadClassData();
                loadTeacherData(); // Reload to refresh teacher list
                updateStatus("Class '" + className + "' added successfully");
            } else {
                showAlert("Error", "Failed to add class. Please try again.");
                updateStatus("Failed to add class");
            }
        } catch (Exception e) {
            showAlert("Error", "Error adding class: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleEditClass() {
        System.out.println("=== handleEditClass() called ===");
        
        // Get the selected item from the table
        ClassData selectedClass = classesTable.getSelectionModel().getSelectedItem();
        if (selectedClass == null) {
            showAlert("No Selection", "Please select a class from the table to edit.");
            updateStatus("No class selected for editing");
            return;
        }
        
        // Populate the form with the selected class data
        handleTableSelection(selectedClass);
        updateStatus("Editing class: " + selectedClass.getClassName());
    }
    
    @FXML
    private void handleUpdateClass() {
        System.out.println("\n=== handleUpdateClass() called ===");
        System.out.println("selectedClassId: " + selectedClassId);
        
        if (selectedClassId == -1) {
            showAlert("Error", "No class selected. Please select a class from the table first.");
            System.out.println("❌ No class selected");
            return;
        }
        
        try {
            // Get form values
            String newClassName = classNameField.getText() != null ? classNameField.getText().trim() : "";
            String newRoomNumber = roomField.getText() != null ? roomField.getText().trim() : "";
            String newSchedule = scheduleField.getText() != null ? scheduleField.getText().trim() : "";
            String selectedTeacher = teacherComboBox.getValue();
            
            System.out.println("Update form values:");
            System.out.println("  New Class Name: '" + newClassName + "'");
            System.out.println("  New Room: '" + newRoomNumber + "'");
            System.out.println("  New Schedule: '" + newSchedule + "'");
            System.out.println("  Selected Teacher: '" + selectedTeacher + "'");
            
            // Validation
            if (newClassName.isEmpty()) {
                showAlert("Error", "Class name is required.");
                classNameField.requestFocus();
                return;
            }
            
            if (newRoomNumber.isEmpty()) {
                showAlert("Error", "Room number is required.");
                roomField.requestFocus();
                return;
            }
            
            if (newSchedule.isEmpty()) {
                showAlert("Error", "Schedule is required.");
                scheduleField.requestFocus();
                return;
            }
            
            if (selectedTeacher == null || selectedTeacher.isEmpty()) {
                showAlert("Error", "Please select a teacher.");
                teacherComboBox.requestFocus();
                return;
            }
            
            Integer teacherId = teacherIdMap.get(selectedTeacher);
            System.out.println("Teacher ID for update: " + teacherId);
            
            System.out.println("✏️ Updating class ID " + selectedClassId + 
                             " with teacher ID: " + teacherId);
            
            boolean success = classService.updateClass(selectedClassId, newClassName, newRoomNumber, newSchedule, teacherId);
            if (success) {
                showAlert("Success", "Class updated successfully.");
                System.out.println("✅ Class updated successfully");
                clearForm();
                loadClassData();
                updateStatus("Class updated successfully");
            } else {
                showAlert("Error", "Failed to update class. It may not exist or there were no changes.");
                System.out.println("❌ Update failed");
                updateStatus("Failed to update class");
            }
        } catch (Exception e) {
            showAlert("Error", "Error updating class: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAssignTeacher() {
        System.out.println("\n=== handleAssignTeacher() called ===");
        
        if (selectedClassId == -1) {
            showAlert("Error", "No class selected. Please select a class from the table first.");
            return;
        }
        
        String selectedTeacher = teacherComboBox.getValue();
        if (selectedTeacher == null || selectedTeacher.isEmpty() || selectedTeacher.equals("No Teacher Assigned")) {
            showAlert("Error", "Please select a valid teacher to assign.");
            return;
        }
        
        try {
            // Get the class data
            Map<String, Object> classData = classService.getClassById(selectedClassId);
            if (classData == null) {
                showAlert("Error", "Class not found.");
                return;
            }
            
            String className = (String) classData.get("className");
            String room = (String) classData.get("room");
            String schedule = (String) classData.get("schedule");
            
            // Get teacher ID from selection
            Integer teacherId = teacherIdMap.get(selectedTeacher);
            
            // Check if teacher is already assigned
            Integer currentTeacherId = (Integer) classData.get("teacherId");
            if (teacherId != null && teacherId.equals(currentTeacherId)) {
                showAlert("Info", "This teacher is already assigned to this class.");
                return;
            }
            
            System.out.println("👨‍🏫 Assigning teacher ID " + teacherId + " to class ID " + selectedClassId);
            boolean success = classService.updateClass(selectedClassId, className, room, schedule, teacherId);
            
            if (success) {
                String teacherName = teacherId != null ? 
                    idToTeacherMap.get(teacherId) : "No Teacher Assigned";
                showAlert("Success", "Teacher '" + teacherName + "' assigned to class '" + className + "' successfully!");
                loadClassData();
                loadTeacherData(); // Reload teacher data
                updateStatus("Teacher assigned successfully");
            } else {
                showAlert("Error", "Failed to assign teacher to class.");
            }
        } catch (Exception e) {
            showAlert("Error", "Error assigning teacher: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDeleteClass() {
        System.out.println("=== handleDeleteClass() called ===");
        try {
            if (selectedClassId == -1) {
                showAlert("Error", "No class selected.");
                return;
            }
            
            // Get class name for confirmation
            String className = "";
            for (ClassData data : classDataList) {
                if (data.getId() == selectedClassId) {
                    className = data.getClassName();
                    break;
                }
            }
            
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setHeaderText("Delete Class");
            alert.setContentText("Are you sure you want to delete the class '" + className + "'?\n\n" +
                               "⚠️ This will:\n" +
                               "• Unassign all students from this class\n" +
                               "• Delete the class record\n" +
                               "• This action cannot be undone!");
            
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            
            if (alert.showAndWait().get() == ButtonType.YES) {
                System.out.println("🗑️ Deleting class ID: " + selectedClassId);
                boolean success = classService.deleteClass(selectedClassId);
                if (success) {
                    showAlert("Success", "Class '" + className + "' deleted successfully.");
                    clearForm();
                    loadClassData();
                    updateStatus("Class '" + className + "' deleted");
                } else {
                    showAlert("Error", "Failed to delete class.");
                    updateStatus("Failed to delete class");
                }
            }
        } catch (Exception e) {
            showAlert("Error", "Error deleting class: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearForm() {
        System.out.println("=== handleClearForm() called ===");
        clearForm();
        updateStatus("Form cleared");
    }
    
    @FXML
    private void handleExportData() {
        System.out.println("=== handleExportData() called ===");
        handleExportTable();
    }
    
    @FXML
    private void handleImportData() {
        System.out.println("=== handleImportData() called ===");
        showAlert("Coming Soon", "Import feature will be available in the next update.");
        updateStatus("Import feature coming soon");
    }
    
    @FXML
    private void handleExportTable() {
        System.out.println("=== handleExportTable() called ===");
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Classes Data");
            fileChooser.setInitialFileName("classes_export_" + System.currentTimeMillis() + ".csv");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            Stage stage = (Stage) classesTable.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                try (PrintWriter writer = new PrintWriter(file)) {
                    // Write header
                    writer.println("Class Name,Room Number,Schedule,Teacher,Student Count");
                    
                    // Write data
                    for (ClassData data : classDataList) {
                        writer.println(
                            "\"" + data.getClassName() + "\"," +
                            "\"" + data.getRoomNumber() + "\"," +
                            "\"" + data.getSchedule() + "\"," +
                            "\"" + data.getTeacherName() + "\"," +
                            data.getStudentCount()
                        );
                    }
                    
                    showAlert("Success", "Data exported successfully to:\n" + file.getAbsolutePath());
                    updateStatus("Data exported to: " + file.getName());
                }
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to export data: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Export failed: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleViewStudents() {
        System.out.println("=== handleViewStudents() called ===");
        if (selectedClassId == -1) {
            showAlert("Error", "No class selected.");
            return;
        }
        
        String className = "";
        int studentCount = 0;
        for (ClassData data : classDataList) {
            if (data.getId() == selectedClassId) {
                className = data.getClassName();
                studentCount = data.getStudentCount();
                break;
            }
        }
        
        showAlert("Class Students", 
                 "Class: " + className + "\n" +
                 "Student Count: " + studentCount + "\n\n" +
                 "This feature will show detailed student list in the next update.");
        updateStatus("Viewing students for " + className);
    }
    
    @FXML
    private void handleViewAttendance() {
        System.out.println("=== handleViewAttendance() called ===");
        if (selectedClassId == -1) {
            showAlert("Error", "No class selected.");
            return;
        }
        
        String className = "";
        for (ClassData data : classDataList) {
            if (data.getId() == selectedClassId) {
                className = data.getClassName();
                break;
            }
        }
        
        showAlert("Class Attendance", 
                 "Class: " + className + "\n\n" +
                 "This feature will show attendance statistics in the next update.");
        updateStatus("Viewing attendance for " + className);
    }
    
    @FXML
    private void handleBack() {
        System.out.println("=== handleBack() called ===");
        clearForm();
        showAlert("Navigation", "Use the sidebar menu to navigate between pages.");
        updateStatus("Returning to dashboard...");
    }
    
    private void handleTableSelection(ClassData selectedClass) {
        System.out.println("\n=== handleTableSelection() called ===");
        try {
            selectedClassId = selectedClass.getId();
            originalClassName = selectedClass.getClassName();
            originalRoom = selectedClass.getRoomNumber();
            originalSchedule = selectedClass.getSchedule();
            originalTeacher = selectedClass.getTeacherName();
            originalTeacherId = selectedClass.getTeacherId();
            
            System.out.println("Selected Class ID: " + selectedClassId);
            System.out.println("Class Name: " + originalClassName);
            System.out.println("Current Teacher ID: " + originalTeacherId);
            System.out.println("Current Teacher Name (from class): " + originalTeacher);
            
            // Set form values
            classNameField.setText(originalClassName);
            roomField.setText(originalRoom);
            scheduleField.setText(originalSchedule);
            
            // Set teacher in combo box - FIXED THIS PART
            if (originalTeacherId != null && originalTeacherId > 0) {
                // First, try to find the teacher name by ID
                String teacherNameFromId = idToTeacherMap.get(originalTeacherId);
                if (teacherNameFromId != null && !teacherNameFromId.isEmpty()) {
                    // Check if this teacher name exists in the combo box
                    if (teacherList.contains(teacherNameFromId)) {
                        teacherComboBox.setValue(teacherNameFromId);
                        System.out.println("Set teacher by ID: " + teacherNameFromId);
                    } else {
                        teacherComboBox.setValue("No Teacher Assigned");
                        System.out.println("Teacher not in list, setting 'No Teacher Assigned'");
                    }
                } else {
                    // If not found by ID, use the teacher name from the class
                    if (originalTeacher != null && !originalTeacher.isEmpty() && 
                        !originalTeacher.equals("No Teacher Assigned") && 
                        teacherList.contains(originalTeacher)) {
                        teacherComboBox.setValue(originalTeacher);
                        System.out.println("Set teacher by name: " + originalTeacher);
                    } else {
                        teacherComboBox.setValue("No Teacher Assigned");
                        System.out.println("Setting 'No Teacher Assigned'");
                    }
                }
            } else {
                teacherComboBox.setValue("No Teacher Assigned");
                System.out.println("No teacher ID, setting 'No Teacher Assigned'");
            }
            
            // Update UI
            selectedClassLabel.setText("Selected: " + originalClassName);
            formStatusLabel.setText("Editing: " + originalClassName);
            setUpdateButtonsDisabled(false);
            
            // Enable/Disable assign teacher button
            if (assignTeacherButton != null) {
                assignTeacherButton.setDisable(false);
            }
            
            updateStatus("Class '" + originalClassName + "' selected for editing");
            
        } catch (Exception e) {
            System.err.println("❌ Error handling table selection: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Error: " + e.getMessage());
        }
    }
    
    private void clearSelection() {
        selectedClassId = -1;
        originalClassName = "";
        originalRoom = "";
        originalSchedule = "";
        originalTeacher = "";
        originalTeacherId = null;
        
        selectedClassLabel.setText("No class selected");
        formStatusLabel.setText("Ready");
        setUpdateButtonsDisabled(true);
        
        if (assignTeacherButton != null) {
            assignTeacherButton.setDisable(true);
        }
        
        classesTable.getSelectionModel().clearSelection();
    }
    
    private void clearForm() {
        classNameField.clear();
        roomField.clear();
        scheduleField.clear();
        teacherComboBox.getSelectionModel().select(0); // Select first item ("No Teacher Assigned")
        clearSelection();
        classNameField.requestFocus();
    }
    
    private void setUpdateButtonsDisabled(boolean disabled) {
        if (updateClassMenuItem != null) updateClassMenuItem.setDisable(disabled);
        if (deleteClassMenuItem != null) deleteClassMenuItem.setDisable(disabled);
        if (editClassMenuItem != null) editClassMenuItem.setDisable(disabled);
        if (quickUpdateButton != null) quickUpdateButton.setDisable(disabled);
        
        if (quickUpdateButton != null) {
            if (disabled) {
                quickUpdateButton.setStyle("-fx-opacity: 0.5;");
            } else {
                quickUpdateButton.setStyle("-fx-opacity: 1.0;");
            }
        }
    }
    
    private void setupFormChangeListeners() {
        classNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            checkFormChanges();
        });
        
        roomField.textProperty().addListener((observable, oldValue, newValue) -> {
            checkFormChanges();
        });
        
        scheduleField.textProperty().addListener((observable, oldValue, newValue) -> {
            checkFormChanges();
        });
        
        teacherComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            checkFormChanges();
        });
    }
    
    private void checkFormChanges() {
        if (selectedClassId == -1) return;
        
        String currentClassName = classNameField.getText() != null ? classNameField.getText().trim() : "";
        String currentRoom = roomField.getText() != null ? roomField.getText().trim() : "";
        String currentSchedule = scheduleField.getText() != null ? scheduleField.getText().trim() : "";
        String currentTeacher = teacherComboBox.getValue() != null ? teacherComboBox.getValue() : "";
        
        boolean hasChanges = !currentClassName.equals(originalClassName) ||
                            !currentRoom.equals(originalRoom) ||
                            !currentSchedule.equals(originalSchedule) ||
                            !currentTeacher.equals(originalTeacher);
        
        if (hasChanges) {
            formStatusLabel.setText("Changed - ready to update");
            formStatusLabel.setStyle("-fx-text-fill: #ff9800;");
        } else {
            formStatusLabel.setText("No changes");
            formStatusLabel.setStyle("-fx-text-fill: #666;");
        }
    }
    
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText("Status: " + message);
        }
        System.out.println("📢 Status: " + message);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void refreshData() {
        loadClassData();
        loadTeacherData();
    }
    
    // Inner class for table data - UPDATED with teacherId
    public static class ClassData {
        private int id;
        private String className;
        private String roomNumber;
        private String schedule;
        private int studentCount;
        private String teacherName;
        private Integer teacherId;
        
        public ClassData(int id, String className, String roomNumber, String schedule, 
                        int studentCount, String teacherName, Integer teacherId) {
            this.id = id;
            this.className = className;
            this.roomNumber = roomNumber;
            this.schedule = schedule;
            this.studentCount = studentCount;
            this.teacherName = teacherName;
            this.teacherId = teacherId;
        }
        
        public int getId() { return id; }
        public String getClassName() { return className; }
        public String getRoomNumber() { return roomNumber; }
        public String getSchedule() { return schedule; }
        public int getStudentCount() { return studentCount; }
        public String getTeacherName() { return teacherName; }
        public Integer getTeacherId() { return teacherId; }
    }
}