package attendance.controller;

import attendance.model.Student;
import attendance.model.User;
import attendance.service.StudentService;
import attendance.service.UserService;
import attendance.service.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.beans.binding.Bindings;

import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;

public class AdminManageStudentsController implements Initializable {

    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, Integer> idColumn;
    @FXML private TableColumn<Student, String> studentIdColumn;
    @FXML private TableColumn<Student, String> firstNameColumn;
    @FXML private TableColumn<Student, String> middleNameColumn;
    @FXML private TableColumn<Student, String> lastNameColumn;
    @FXML private TableColumn<Student, String> emailColumn;
    @FXML private TableColumn<Student, String> classNameColumn;
    @FXML private TableColumn<Student, String> departmentColumn;
    @FXML private TableColumn<Student, Integer> userIdColumn;
    
    @FXML private TextField searchField;
    @FXML private Button refreshButton;
    @FXML private Button backToDashboardButton;
    @FXML private MenuButton manageMenuButton;
    @FXML private Label statusLabel;
    
    // Menu items
    @FXML private MenuItem addMenuItem;
    @FXML private MenuItem editMenuItem;
    @FXML private MenuItem deleteMenuItem;
    @FXML private MenuItem resetMenuItem;
    @FXML private MenuItem forceDeleteMenuItem;
    @FXML private MenuItem fixDatabaseMenuItem;
    @FXML private MenuItem resetPasswordMenuItem;
    @FXML private MenuItem resetUsernamePasswordMenuItem;
    @FXML private MenuItem generateReportMenuItem;
    @FXML private MenuItem fixUserAccountsMenuItem;
    @FXML private MenuItem deleteSelectedMenuItem;
    
    private StudentService studentService;
    private UserService userService;
    private DatabaseService databaseService;
    private User currentUser;
    private MainController mainController;
    
    // Flag to track if shortcuts have been set up
    private boolean shortcutsSet = false;
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        studentService = new StudentService();
        userService = new UserService();
        databaseService = new DatabaseService();
        
        setupTableColumns();
        setupTableForMultiSelection();
        loadStudents();
        setupTableSelection();
        setupSearchListener();
        setupContextMenu();
        
        System.out.println("✅ AdminManageStudentsController initialized with multi-selection support");
        
        // Set up keyboard shortcuts when the scene is available
        setupKeyboardShortcutsWhenReady();
        
        // NEW: Add tooltips to buttons for better UX
        addTooltips();
    }
    
    private void setupKeyboardShortcutsWhenReady() {
        // Wait for the table to be added to a scene before setting up shortcuts
        studentsTable.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null && !shortcutsSet) {
                setupKeyboardShortcuts();
                shortcutsSet = true;
                System.out.println("✅ Keyboard shortcuts set up");
            }
        });
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        middleNameColumn.setCellValueFactory(new PropertyValueFactory<>("middleName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        
        // NEW: Add styling to columns for better visual feedback
        studentIdColumn.setStyle("-fx-alignment: CENTER;");
        userIdColumn.setStyle("-fx-alignment: CENTER;");
    }
    
    private void setupTableForMultiSelection() {
        studentsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        studentsTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.A && event.isControlDown()) {
                studentsTable.getSelectionModel().selectAll();
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE) {
                handleDeleteSelectedStudents();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                studentsTable.getSelectionModel().clearSelection();
                event.consume();
            } else if (event.getCode() == KeyCode.F5) {
                handleRefresh();
                event.consume();
            }
        });
    }
    
    private void setupKeyboardShortcuts() {
        if (studentsTable.getScene() == null) {
            System.out.println("⚠️ Table scene is null, cannot set up keyboard shortcuts yet");
            return;
        }
        
        try {
            // Delete key shortcut
            studentsTable.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DELETE),
                this::handleDeleteSelectedStudents
            );
            
            // Select all shortcut
            studentsTable.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN),
                () -> studentsTable.getSelectionModel().selectAll()
            );
            
            // Refresh shortcut
            studentsTable.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.F5),
                this::handleRefresh
            );
            
            // Escape to clear selection
            studentsTable.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.ESCAPE),
                () -> studentsTable.getSelectionModel().clearSelection()
            );
            
            System.out.println("✅ Keyboard shortcuts registered");
        } catch (Exception e) {
            System.err.println("❌ Error setting up keyboard shortcuts: " + e.getMessage());
        }
    }
    
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("✏️ Edit Student");
        editItem.setOnAction(e -> handleEditStudent());
        editItem.disableProperty().bind(
            studentsTable.getSelectionModel().selectedItemProperty().isNull()
            .or(Bindings.size(studentsTable.getSelectionModel().getSelectedItems()).isNotEqualTo(1))
        );
        
        MenuItem deleteItem = new MenuItem("🗑️ Delete Selected");
        deleteItem.setOnAction(e -> handleDeleteSelectedStudents());
        deleteItem.disableProperty().bind(
            studentsTable.getSelectionModel().selectedItemProperty().isNull()
        );
        
        MenuItem resetPasswordItem = new MenuItem("🔑 Reset Password");
        resetPasswordItem.setOnAction(e -> handleResetPassword());
        resetPasswordItem.disableProperty().bind(
            studentsTable.getSelectionModel().selectedItemProperty().isNull()
            .or(Bindings.size(studentsTable.getSelectionModel().getSelectedItems()).isNotEqualTo(1))
        );
        
        MenuItem resetUsernamePasswordItem = new MenuItem("👤 Reset Username & Password");
        resetUsernamePasswordItem.setOnAction(e -> handleResetUsernamePassword());
        resetUsernamePasswordItem.disableProperty().bind(
            studentsTable.getSelectionModel().selectedItemProperty().isNull()
            .or(Bindings.size(studentsTable.getSelectionModel().getSelectedItems()).isNotEqualTo(1))
        );
        
        MenuItem selectAllItem = new MenuItem("📋 Select All");
        selectAllItem.setOnAction(e -> handleSelectAll());
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
        
        MenuItem deselectAllItem = new MenuItem("❌ Clear Selection");
        deselectAllItem.setOnAction(e -> studentsTable.getSelectionModel().clearSelection());
        
        // NEW: Add quick navigation menu items
        MenuItem viewAttendanceItem = new MenuItem("📊 View Attendance");
        viewAttendanceItem.setOnAction(e -> handleViewAttendance());
        viewAttendanceItem.disableProperty().bind(
            studentsTable.getSelectionModel().selectedItemProperty().isNull()
            .or(Bindings.size(studentsTable.getSelectionModel().getSelectedItems()).isNotEqualTo(1))
        );
        
        contextMenu.getItems().addAll(
            editItem,
            resetPasswordItem,
            resetUsernamePasswordItem,
            viewAttendanceItem, // NEW: Added view attendance option
            new SeparatorMenuItem(),
            deleteItem,
            new SeparatorMenuItem(),
            selectAllItem,
            deselectAllItem
        );
        
        studentsTable.setContextMenu(contextMenu);
    }
    
    private void loadStudents() {
        System.out.println("🔄 Loading students from database...");
        List<Student> students = studentService.getAllStudents();
        studentsTable.getItems().clear();
        studentsTable.getItems().addAll(students);
        
        // NEW: Add row factory for better visual feedback
        studentsTable.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<Student>() {
                @Override
                protected void updateItem(Student student, boolean empty) {
                    super.updateItem(student, empty);
                    if (student == null || empty) {
                        setStyle("");
                    } else {
                        // Alternate row colors for better readability
                        if (getIndex() % 2 == 0) {
                            setStyle("-fx-background-color: #f9fafb;");
                        } else {
                            setStyle("-fx-background-color: white;");
                        }
                        
                        // Highlight rows with incomplete data
                        if (student.getUserId() == 0 || student.getUserId() == -1) {
                            setStyle("-fx-background-color: #fff7ed; -fx-border-color: #fed7aa; -fx-border-width: 0 0 1 0;");
                        }
                    }
                }
            };
            return row;
        });
        
        updateStatusLabel("Total students: " + students.size(), "#10b981");
        System.out.println("✅ Loaded " + students.size() + " students");
    }
    
    private void updateStatusLabel(String text, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        });
    }
    
    private void setupTableSelection() {
        studentsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
                int selectedCount = selectedStudents.size();
                
                boolean hasSelection = selectedCount > 0;
                boolean singleSelection = selectedCount == 1;
                
                editMenuItem.setDisable(!singleSelection);
                resetPasswordMenuItem.setDisable(!singleSelection);
                resetUsernamePasswordMenuItem.setDisable(!singleSelection);
                deleteMenuItem.setDisable(!hasSelection);
                deleteSelectedMenuItem.setDisable(!hasSelection);
                forceDeleteMenuItem.setDisable(!hasSelection);
                
                if (hasSelection) {
                    System.out.println("👆 Selected " + selectedCount + " student(s)");
                    
                    if (selectedCount == 1) {
                        Student student = selectedStudents.get(0);
                        updateStatusLabel("Selected: " + student.getFullName() + " (" + student.getStudentId() + ")", "#3b82f6");
                    } else {
                        updateStatusLabel("Selected " + selectedCount + " students", "#3b82f6");
                    }
                } else {
                    updateStatusLabel("Total students: " + studentsTable.getItems().size(), "#10b981");
                }
            }
        );
    }
    
    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            handleSearch();
        });
        
        // NEW: Add enter key support for search
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSearch();
            }
        });
    }
    
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return true; // Allow empty for middle name
        }
        return name.matches("^[A-Za-z]+(?:\\s+[A-Za-z]+)*$");
    }
    
    private boolean isValidInstitutionalEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9._%+-]+@du\\.edu\\.et$");
    }
    
    private boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return username.matches("^[A-Za-z0-9_]{3,20}$");
    }
    
    private boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return password.length() >= 6;
    }
    
    private boolean isValidClassName(String className) {
        if (className == null || className.trim().isEmpty()) {
            return false;
        }
        return className.matches("^[A-Za-z0-9\\s]+$");
    }
    
    private boolean isValidDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            return false;
        }
        return department.matches("^[A-Za-z\\s]+$");
    }
    
    private boolean isValidStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }
        return studentId.matches("^[A-Za-z0-9]{3,10}$");
    }
    
    private void showValidationError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private HBox createFormRow(String labelText, Node field) {
        HBox row = new HBox(10);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label label = new Label(labelText);
        label.setPrefWidth(120); // Increased from 100 for better alignment
        label.getStyleClass().add("form-label");
        
        if (field instanceof Control) {
            ((Control) field).getStyleClass().add("form-textfield");
        }
        
        row.getChildren().addAll(label, field);
        return row;
    }
    
    @FXML
    private void handleAddStudent() {
        try {
            Dialog<StudentWithCredentials> dialog = new Dialog<>();
            dialog.setTitle("Add New Student");
            dialog.setHeaderText("Enter student details and login credentials");
            dialog.getDialogPane().getStyleClass().add("form-dialog");
            
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            // NEW: Add form validation in real-time
            TextField firstNameField = new TextField();
            firstNameField.setPromptText("First Name");
            
            TextField middleNameField = new TextField();
            middleNameField.setPromptText("Middle Name (Optional)");
            
            TextField lastNameField = new TextField();
            lastNameField.setPromptText("Last Name");
            
            TextField emailField = new TextField();
            emailField.setPromptText("Email (@du.edu.et)");
            
            TextField usernameField = new TextField();
            usernameField.setPromptText("Username (3-20 characters)");
            
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password (min 6 characters)");
            
            TextField visiblePasswordField = new TextField();
            visiblePasswordField.setPromptText("Password (min 6 characters)");
            visiblePasswordField.setVisible(false);
            
            CheckBox showPasswordCheck = new CheckBox("Show Password");
            showPasswordCheck.setOnAction(e -> {
                if (showPasswordCheck.isSelected()) {
                    visiblePasswordField.setText(passwordField.getText());
                    visiblePasswordField.setVisible(true);
                    passwordField.setVisible(false);
                } else {
                    passwordField.setText(visiblePasswordField.getText());
                    passwordField.setVisible(true);
                    visiblePasswordField.setVisible(false);
                }
            });
            
            HBox passwordBox = new HBox(10);
            passwordBox.getChildren().addAll(passwordField, visiblePasswordField, showPasswordCheck);
            
            // NEW: Add class selection from existing classes
            TextField departmentField = new TextField();
            departmentField.setPromptText("Department");
            
            // NEW: Get existing classes from database for auto-completion
            List<String> existingClasses = getExistingClasses();
            ComboBox<String> classNameCombo = new ComboBox<>();
            classNameCombo.setPromptText("Select or type class name");
            classNameCombo.setEditable(true);
            classNameCombo.getItems().addAll(existingClasses);
            
            VBox form = new VBox(10);
            form.setPadding(new Insets(20));
            form.getStyleClass().add("form-container");
            
            form.getChildren().addAll(
                createFormRow("First Name:", firstNameField),
                createFormRow("Middle Name:", middleNameField),
                createFormRow("Last Name:", lastNameField),
                createFormRow("Email:", emailField),
                createFormRow("Username:", usernameField),
                createFormRow("Password:", passwordBox),
                createFormRow("Department:", departmentField),
                createFormRow("Class:", classNameCombo)
            );
            
            dialog.getDialogPane().setContent(form);
            Platform.runLater(() -> firstNameField.requestFocus());
            
            // NEW: Enable/disable save button based on form validation
            Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.setDisable(true);
            
            // Add validation listeners
            Runnable validateForm = () -> {
                boolean isValid = !firstNameField.getText().trim().isEmpty() &&
                                 !lastNameField.getText().trim().isEmpty() &&
                                 !emailField.getText().trim().isEmpty() &&
                                 !usernameField.getText().trim().isEmpty() &&
                                 !passwordField.getText().trim().isEmpty() &&
                                 !departmentField.getText().trim().isEmpty() &&
                                 classNameCombo.getValue() != null &&
                                 !classNameCombo.getValue().trim().isEmpty();
                saveButton.setDisable(!isValid);
            };
            
            firstNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            lastNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            emailField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            usernameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            departmentField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            classNameCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    String password = showPasswordCheck.isSelected() ? 
                        visiblePasswordField.getText().trim() : 
                        passwordField.getText().trim();
                    
                    return new StudentWithCredentials(
                        firstNameField.getText().trim(),
                        middleNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        emailField.getText().trim(),
                        usernameField.getText().trim(),
                        password,
                        departmentField.getText().trim(),
                        classNameCombo.getValue().trim()
                    );
                }
                return null;
            });
            
            Optional<StudentWithCredentials> result = dialog.showAndWait();
            
            result.ifPresent(data -> {
                if (!isValidName(data.firstName)) {
                    showValidationError("Invalid First Name", 
                        "First name must contain only letters.\nExample: John");
                    return;
                }
                
                if (!isValidName(data.middleName) && !data.middleName.isEmpty()) {
                    showValidationError("Invalid Middle Name", 
                        "Middle name must contain only letters or be empty.\nExample: Michael");
                    return;
                }
                
                if (!isValidName(data.lastName)) {
                    showValidationError("Invalid Last Name", 
                        "Last name must contain only letters.\nExample: Smith");
                    return;
                }
                
                if (!isValidInstitutionalEmail(data.email)) {
                    showValidationError("Invalid Email", 
                        "Email must be institutional email (@du.edu.et).\nExample: student123@du.edu.et");
                    return;
                }
                
                if (!isValidUsername(data.username)) {
                    showValidationError("Invalid Username", 
                        "Username must be 3-20 characters (letters, numbers, underscore).");
                    return;
                }
                
                if (!isValidPassword(data.password)) {
                    showValidationError("Invalid Password", 
                        "Password must be at least 6 characters long.");
                    return;
                }
                
                if (!isValidDepartment(data.department)) {
                    showValidationError("Invalid Department", 
                        "Department must contain only letters and spaces.");
                    return;
                }
                
                if (!isValidClassName(data.className)) {
                    showValidationError("Invalid Class Name", 
                        "Class name can contain letters, numbers, and spaces.");
                    return;
                }
                
                if (userService.usernameExists(data.username)) {
                    showValidationError("Username Exists", 
                        "Username '" + data.username + "' already exists. Please choose another.");
                    return;
                }
                
                if (studentService.emailExists(data.email)) {
                    showValidationError("Email Exists", 
                        "Email '" + data.email + "' already exists.");
                    return;
                }
                
                String studentId = generateStudentId();
                
                User newUser = new User();
                newUser.setUsername(data.username);
                newUser.setPassword(data.password);
                newUser.setRole("STUDENT");
                newUser.setFirstName(data.firstName);
                newUser.setLastName(data.lastName);
                newUser.setEmail(data.email);
                
                int userId = userService.createUser(newUser);
                
                if (userId > 0) {
                    Student newStudent = new Student();
                    newStudent.setStudentId(studentId);
                    newStudent.setFirstName(data.firstName);
                    newStudent.setMiddleName(data.middleName);
                    newStudent.setLastName(data.lastName);
                    newStudent.setEmail(data.email);
                    newStudent.setClassName(data.className);
                    newStudent.setDepartment(data.department);
                    newStudent.setUserId(userId);
                    
                    boolean success = studentService.addStudent(newStudent);
                    if (success) {
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Success");
                        successAlert.setHeaderText("Student added successfully!");
                        successAlert.setContentText(
                            "Student ID: " + studentId + "\n" +
                            "Full Name: " + data.firstName + " " + 
                            (data.middleName.isEmpty() ? "" : data.middleName + " ") + data.lastName + "\n" +
                            "Username: " + data.username + "\n" +
                            "Password: " + data.password + "\n" +
                            "Department: " + data.department + "\n" +
                            "Class: " + data.className + "\n\n" +
                            "✅ Student can now log in with the credentials above."
                        );
                        successAlert.showAndWait();
                        
                        loadStudents();
                    } else {
                        userService.deleteUser(userId);
                        showAlert("Error", "Failed to add student. Please try again.", 
                                 Alert.AlertType.ERROR);
                    }
                } else {
                    showAlert("Error", "Failed to create user account.", Alert.AlertType.ERROR);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleEditStudent() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.size() != 1) {
            showAlert("Invalid Selection", "Please select exactly one student to edit.", Alert.AlertType.WARNING);
            return;
        }
        
        Student student = selectedStudents.get(0);
        handleEditStudentDialog(student);
    }
    
    private void handleEditStudentDialog(Student student) {
        try {
            Dialog<Student> dialog = new Dialog<>();
            dialog.setTitle("Edit Student");
            dialog.setHeaderText("Edit student details");
            dialog.getDialogPane().getStyleClass().add("form-dialog");
            
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            TextField studentIdField = new TextField(student.getStudentId());
            TextField firstNameField = new TextField(student.getFirstName());
            TextField middleNameField = new TextField(student.getMiddleName());
            TextField lastNameField = new TextField(student.getLastName());
            TextField emailField = new TextField(student.getEmail());
            TextField departmentField = new TextField(student.getDepartment());
            
            // NEW: Use ComboBox for class selection in edit dialog too
            List<String> existingClasses = getExistingClasses();
            ComboBox<String> classNameCombo = new ComboBox<>();
            classNameCombo.setPromptText("Select or type class name");
            classNameCombo.setEditable(true);
            classNameCombo.getItems().addAll(existingClasses);
            classNameCombo.setValue(student.getClassName());
            
            studentIdField.setEditable(true);
            
            VBox form = new VBox(10);
            form.setPadding(new Insets(20));
            form.getStyleClass().add("form-container");
            
            form.getChildren().addAll(
                createFormRow("Student ID:", studentIdField),
                createFormRow("First Name:", firstNameField),
                createFormRow("Middle Name:", middleNameField),
                createFormRow("Last Name:", lastNameField),
                createFormRow("Email:", emailField),
                createFormRow("Department:", departmentField),
                createFormRow("Class:", classNameCombo)
            );
            
            dialog.getDialogPane().setContent(form);
            Platform.runLater(() -> studentIdField.requestFocus());
            
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    Student updatedStudent = new Student();
                    updatedStudent.setId(student.getId());
                    updatedStudent.setStudentId(studentIdField.getText().trim());
                    updatedStudent.setFirstName(firstNameField.getText().trim());
                    updatedStudent.setMiddleName(middleNameField.getText().trim());
                    updatedStudent.setLastName(lastNameField.getText().trim());
                    updatedStudent.setEmail(emailField.getText().trim());
                    updatedStudent.setDepartment(departmentField.getText().trim());
                    updatedStudent.setClassName(classNameCombo.getValue().trim());
                    updatedStudent.setUserId(student.getUserId());
                    return updatedStudent;
                }
                return null;
            });
            
            Optional<Student> result = dialog.showAndWait();
            
            result.ifPresent(updatedStudent -> {
                if (!isValidStudentId(updatedStudent.getStudentId())) {
                    showValidationError("Invalid Student ID", 
                        "Student ID must be 3-10 characters (letters and numbers only).");
                    return;
                }
                
                if (!isValidName(updatedStudent.getFirstName())) {
                    showValidationError("Invalid First Name", 
                        "First name must contain only letters.");
                    return;
                }
                
                if (!isValidName(updatedStudent.getMiddleName()) && !updatedStudent.getMiddleName().isEmpty()) {
                    showValidationError("Invalid Middle Name", 
                        "Middle name must contain only letters or be empty.");
                    return;
                }
                
                if (!isValidName(updatedStudent.getLastName())) {
                    showValidationError("Invalid Last Name", 
                        "Last name must contain only letters.");
                    return;
                }
                
                if (!isValidInstitutionalEmail(updatedStudent.getEmail())) {
                    showValidationError("Invalid Email", 
                        "Email must be institutional email (@du.edu.et).");
                    return;
                }
                
                if (!isValidDepartment(updatedStudent.getDepartment())) {
                    showValidationError("Invalid Department", 
                        "Department must contain only letters and spaces.");
                    return;
                }
                
                if (!isValidClassName(updatedStudent.getClassName())) {
                    showValidationError("Invalid Class Name", 
                        "Class name can contain letters, numbers, and spaces.");
                    return;
                }
                
                if (!updatedStudent.getStudentId().equals(student.getStudentId()) && 
                    studentService.studentIdExists(updatedStudent.getStudentId())) {
                    showValidationError("Student ID Exists", 
                        "Student ID '" + updatedStudent.getStudentId() + "' already exists.");
                    return;
                }
                
                boolean success = studentService.updateStudent(updatedStudent);
                if (success) {
                    if (!updatedStudent.getEmail().equals(student.getEmail())) {
                        User user = userService.getUserById(student.getUserId());
                        if (user != null) {
                            user.setEmail(updatedStudent.getEmail());
                            userService.updateUser(user);
                        }
                    }
                    
                    showAlert("Success", "Student updated successfully.", Alert.AlertType.INFORMATION);
                    loadStudents();
                } else {
                    showAlert("Error", "Failed to update student.", Alert.AlertType.ERROR);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleResetPassword() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.size() != 1) {
            showAlert("Invalid Selection", "Please select exactly one student to reset password.", Alert.AlertType.WARNING);
            return;
        }
        
        Student selected = selectedStudents.get(0);
        
        Alert choiceDialog = new Alert(Alert.AlertType.CONFIRMATION);
        choiceDialog.setTitle("Reset Password");
        choiceDialog.setHeaderText("Reset password for: " + selected.getFullName());
        choiceDialog.setContentText("Choose reset method:");
        
        ButtonType generateButton = new ButtonType("Generate Random Password");
        ButtonType customButton = new ButtonType("Set Custom Password");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        choiceDialog.getButtonTypes().setAll(generateButton, customButton, cancelButton);
        
        Optional<ButtonType> result = choiceDialog.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == generateButton) {
                String newPassword = generateRandomPassword();
                resetPassword(selected, newPassword, true);
                
            } else if (result.get() == customButton) {
                showCustomPasswordDialog(selected);
            }
        }
    }
    
    @FXML
    private void handleResetUsernamePassword() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.size() != 1) {
            showAlert("Invalid Selection", "Please select exactly one student to reset username and password.", Alert.AlertType.WARNING);
            return;
        }
        
        Student selected = selectedStudents.get(0);
        showResetUsernamePasswordDialog(selected);
    }
    
    private void showResetUsernamePasswordDialog(Student student) {
        Dialog<UsernamePasswordReset> dialog = new Dialog<>();
        dialog.setTitle("Reset Username & Password");
        dialog.setHeaderText("Reset username and password for: " + student.getFullName());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Get current user information
        User user = userService.getUserById(student.getUserId());
        if (user == null) {
            showAlert("Error", "User account not found for this student.", Alert.AlertType.ERROR);
            return;
        }
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("New username (3-20 characters)");
        usernameField.setText(user.getUsername());
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("New password (min 6 characters)");
        
        TextField visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("New password (min 6 characters)");
        visiblePasswordField.setVisible(false);
        
        CheckBox showPasswordCheck = new CheckBox("Show Password");
        showPasswordCheck.setOnAction(e -> {
            if (showPasswordCheck.isSelected()) {
                visiblePasswordField.setText(passwordField.getText());
                visiblePasswordField.setVisible(true);
                passwordField.setVisible(false);
            } else {
                passwordField.setText(visiblePasswordField.getText());
                passwordField.setVisible(true);
                visiblePasswordField.setVisible(false);
            }
        });
        
        HBox passwordBox = new HBox(10);
        passwordBox.getChildren().addAll(passwordField, visiblePasswordField, showPasswordCheck);
        
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");
        
        grid.add(new Label("New Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(passwordBox, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmField, 1, 2);
        grid.add(showPasswordCheck, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> usernameField.requestFocus());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String password = showPasswordCheck.isSelected() ? 
                    visiblePasswordField.getText().trim() : 
                    passwordField.getText().trim();
                
                return new UsernamePasswordReset(
                    usernameField.getText().trim(),
                    password
                );
            }
            return null;
        });
        
        Optional<UsernamePasswordReset> result = dialog.showAndWait();
        
        result.ifPresent(resetData -> {
            // Validation
            if (resetData.username.isEmpty()) {
                showAlert("Error", "Username cannot be empty.", Alert.AlertType.ERROR);
                return;
            }
            
            String confirmPassword = confirmField.getText().trim();
            if (resetData.password.isEmpty()) {
                showAlert("Error", "Password cannot be empty.", Alert.AlertType.ERROR);
                return;
            }
            
            if (!resetData.password.equals(confirmPassword)) {
                showAlert("Error", "Passwords do not match.", Alert.AlertType.ERROR);
                return;
            }
            
            if (!isValidUsername(resetData.username)) {
                showAlert("Invalid Username", 
                    "Username must be 3-20 characters (letters, numbers, underscore).", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            if (!isValidPassword(resetData.password)) {
                showAlert("Invalid Password", 
                    "Password must be at least 6 characters long.", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            // Check if username is already taken by another user
            if (!resetData.username.equals(user.getUsername()) && 
                userService.usernameExists(resetData.username)) {
                showAlert("Username Exists", 
                    "Username '" + resetData.username + "' already exists. Please choose another.", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            // Update user
            user.setUsername(resetData.username);
            user.setPassword(resetData.password);
            boolean success = userService.updateUser(user);
            
            if (success) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Username and password reset successfully!");
                alert.setContentText(
                    "Student: " + student.getFullName() + "\n" +
                    "Student ID: " + student.getStudentId() + "\n" +
                    "New Username: " + resetData.username + "\n" +
                    "New Password: " + resetData.password + "\n\n" +
                    "⚠️ IMPORTANT: Please inform the student to change this password immediately!"
                );
                alert.showAndWait();
                
                System.out.println("✅ Username and password reset for student: " + student.getFullName());
            } else {
                showAlert("Error", "Failed to reset username and password.", Alert.AlertType.ERROR);
            }
        });
    }
    
    private void resetPassword(Student student, String newPassword, boolean showPassword) {
        try {
            User user = userService.getUserById(student.getUserId());
            if (user == null) {
                showAlert("Error", "User account not found for this student.", Alert.AlertType.ERROR);
                return;
            }
            
            user.setPassword(newPassword);
            boolean success = userService.updateUser(user);
            
            if (success) {
                String message = "✅ Password reset successfully for " + student.getFullName();
                
                if (showPassword) {
                    message += "\n\n📝 New Password: " + newPassword + 
                              "\n\n⚠️ IMPORTANT: Please inform the student to change this password immediately!";
                }
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Password Reset Successful");
                alert.setHeaderText("Password has been reset");
                alert.setContentText(message);
                alert.showAndWait();
            } else {
                showAlert("Error", "Failed to reset password.", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void showCustomPasswordDialog(Student student) {
        Dialog<String> passwordDialog = new Dialog<>();
        passwordDialog.setTitle("Set Custom Password");
        passwordDialog.setHeaderText("Set password for: " + student.getFullName());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        passwordDialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("New password (min 6 characters)");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");
        
        grid.add(new Label("New Password:"), 0, 0);
        grid.add(passwordField, 1, 0);
        grid.add(new Label("Confirm Password:"), 0, 1);
        grid.add(confirmField, 1, 1);
        
        passwordDialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> passwordField.requestFocus());
        
        passwordDialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return passwordField.getText();
            }
            return null;
        });
        
        Optional<String> passwordResult = passwordDialog.showAndWait();
        
        passwordResult.ifPresent(password -> {
            if (password.isEmpty()) {
                showAlert("Error", "Password cannot be empty.", Alert.AlertType.ERROR);
                return;
            }
            
            if (!password.equals(confirmField.getText())) {
                showAlert("Error", "Passwords do not match.", Alert.AlertType.ERROR);
                return;
            }
            
            if (!isValidPassword(password)) {
                showAlert("Invalid Password", 
                    "Password must be at least 6 characters long.", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            resetPassword(student, password, false);
        });
    }
    
    private String generateRandomPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%";
        String allChars = upper + lower + digits + special;
        
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));
        
        for (int i = 0; i < 6; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
    
    @FXML
    private void handleFixDatabase() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Fix Database");
        confirmation.setHeaderText("Fix Database Schema");
        confirmation.setContentText("This will fix missing columns and database structure issues.\n" +
                                   "Your existing data will be preserved.\n\n" +
                                   "Do you want to continue?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                databaseService.fixDatabaseSchema();
                showAlert("Success", "Database fixed successfully!\nPlease restart the application for changes to take effect.", 
                         Alert.AlertType.INFORMATION);
                
                loadStudents();
                
            } catch (Exception e) {
                showAlert("Error", "Failed to fix database: " + e.getMessage(), 
                         Alert.AlertType.ERROR);
            }
        }
    }
    
    @FXML
    private void handleResetDatabase() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("⚠️ WARNING: Reset Database");
        alert.setHeaderText("THIS WILL DELETE ALL DATA!");
        alert.setContentText("This action will:\n" +
                            "1. Delete ALL students, teachers, and attendance records\n" +
                            "2. Delete ALL users except default admin\n" +
                            "3. Recreate all tables\n" +
                            "4. Insert fresh sample data\n\n" +
                            "This cannot be undone!\n\n" +
                            "Are you absolutely sure?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                databaseService.resetDatabase();
                showAlert("Database Reset", 
                         "Database has been reset successfully!\n" +
                         "Please close and restart the application.", 
                         Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Error", "Failed to reset database: " + e.getMessage(), 
                         Alert.AlertType.ERROR);
            }
        }
    }
    
    @FXML
    private void handleFixUserAccounts() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Fix User Accounts");
        confirmation.setHeaderText("Fix Missing User Accounts");
        confirmation.setContentText("This will create user accounts for students who don't have them.\n" +
                                   "Students without user accounts cannot log in.\n\n" +
                                   "Do you want to continue?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Alert progress = new Alert(Alert.AlertType.INFORMATION);
            progress.setTitle("Fixing User Accounts");
            progress.setHeaderText("Please wait while we fix user accounts...");
            progress.setContentText("Creating user accounts for students...");
            progress.show();
            
            new Thread(() -> {
                try {
                    int fixedCount = databaseService.fixMissingUserAccounts();
                    
                    Platform.runLater(() -> {
                        progress.close();
                        
                        if (fixedCount > 0) {
                            showAlert("Success", 
                                    "✅ Successfully fixed " + fixedCount + " user account(s)!\n\n" +
                                    "• Created missing user accounts\n" +
                                    "• Linked students to user accounts\n" +
                                    "• Students can now log in with their email as username\n" +
                                    "• Default password: Student@123",
                                    Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("No Issues Found", 
                                    "✅ All students already have user accounts!\n\n" +
                                    "No user accounts needed to be created.",
                                    Alert.AlertType.INFORMATION);
                        }
                        
                        loadStudents();
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progress.close();
                        showAlert("Error", "Failed to fix user accounts: " + e.getMessage(), 
                                 Alert.AlertType.ERROR);
                    });
                }
            }).start();
        }
    }
    
    @FXML
    private void handleDeleteSelectedStudents() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.isEmpty()) {
            showAlert("No Selection", "Please select at least one student to delete.", Alert.AlertType.WARNING);
            return;
        }
        
        int selectedCount = selectedStudents.size();
        
        StringBuilder studentList = new StringBuilder();
        if (selectedCount <= 5) {
            for (Student student : selectedStudents) {
                studentList.append("\n• ").append(student.getFullName())
                          .append(" (").append(student.getStudentId()).append(")");
            }
        } else {
            int count = 0;
            for (Student student : selectedStudents) {
                if (count < 3) {
                    studentList.append("\n• ").append(student.getFullName())
                              .append(" (").append(student.getStudentId()).append(")");
                    count++;
                } else {
                    break;
                }
            }
            studentList.append("\n• ... and ").append(selectedCount - 3).append(" more");
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Students");
        confirmation.setHeaderText("Delete " + selectedCount + " student(s)?");
        confirmation.setContentText("Are you sure you want to delete the following students?" + 
                                   studentList.toString() + "\n\n" +
                                   "This will also delete:\n" +
                                   "• All attendance records\n" +
                                   "• User accounts\n" +
                                   "• Any related data");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Alert progress = new Alert(Alert.AlertType.INFORMATION);
            progress.setTitle("Deleting Students");
            progress.setHeaderText("Deleting " + selectedCount + " student(s)...");
            progress.setContentText("Please wait while we delete the selected students and all related data.");
            progress.show();
            
            new Thread(() -> {
                // Use arrays to work around the lambda "final or effectively final" restriction
                final int[] successCount = {0};
                final int[] failCount = {0};
                final List<String> failedStudents = new ArrayList<>();
                
                for (Student student : selectedStudents) {
                    boolean success = studentService.deleteStudent(student.getId());
                    
                    if (success) {
                        successCount[0]++;
                        System.out.println("✅ Successfully deleted: " + student.getFullName());
                    } else {
                        failCount[0]++;
                        failedStudents.add(student.getFullName() + " (" + student.getStudentId() + ")");
                        System.out.println("❌ Failed to delete: " + student.getFullName());
                    }
                }
                
                final int finalSuccessCount = successCount[0];
                final int finalFailCount = failCount[0];
                
                Platform.runLater(() -> {
                    progress.close();
                    
                    if (finalSuccessCount > 0) {
                        String successMessage = "✅ Successfully deleted " + finalSuccessCount + " student(s)!";
                        
                        if (finalFailCount > 0) {
                            successMessage += "\n\n❌ Failed to delete " + finalFailCount + " student(s):";
                            for (String name : failedStudents) {
                                successMessage += "\n• " + name;
                            }
                            successMessage += "\n\nPossible reasons:\n" +
                                            "1. Database constraints are preventing deletion\n" +
                                            "2. Database is locked\n" +
                                            "3. Foreign key violations\n\n" +
                                            "You can try 'Force Delete' or 'Fix Database' options.";
                        }
                        
                        showAlert("Deletion Results", successMessage, 
                                 finalFailCount > 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
                        
                        loadStudents();
                    } else {
                        showAlert("Deletion Failed", 
                                "❌ Failed to delete all " + selectedCount + " student(s).\n\n" +
                                "Please try:\n" +
                                "1. Fix Database (from Manage Students menu)\n" +
                                "2. Force Delete option\n" +
                                "3. Check database permissions",
                                Alert.AlertType.ERROR);
                    }
                });
            }).start();
        }
    }
    
    @FXML
    private void handleForceDeleteSelected() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.isEmpty()) {
            showAlert("No Selection", "Please select at least one student to force delete.", Alert.AlertType.WARNING);
            return;
        }
        
        int selectedCount = selectedStudents.size();
        
        StringBuilder studentList = new StringBuilder();
        if (selectedCount <= 5) {
            for (Student student : selectedStudents) {
                studentList.append("\n• ").append(student.getFullName())
                          .append(" (").append(student.getStudentId()).append(")");
            }
        } else {
            int count = 0;
            for (Student student : selectedStudents) {
                if (count < 3) {
                    studentList.append("\n• ").append(student.getFullName())
                              .append(" (").append(student.getStudentId()).append(")");
                    count++;
                } else {
                    break;
                }
            }
            studentList.append("\n• ... and ").append(selectedCount - 3).append(" more");
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("⚠️ FORCE DELETE");
        confirmation.setHeaderText("FORCE DELETE " + selectedCount + " STUDENT(S)");
        confirmation.setContentText("WARNING: This will forcefully delete the students even if there are errors.\n\n" +
                                   "Students:" + studentList.toString() + "\n\n" +
                                   "This may leave orphaned data in the database.\n\n" +
                                   "Are you absolutely sure?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Alert progress = new Alert(Alert.AlertType.INFORMATION);
            progress.setTitle("Force Deleting Students");
            progress.setHeaderText("Force deleting " + selectedCount + " student(s)...");
            progress.setContentText("Please wait while we forcefully delete the selected students.");
            progress.show();
            
            new Thread(() -> {
                // Use arrays to work around the lambda "final or effectively final" restriction
                final int[] successCount = {0};
                final int[] failCount = {0};
                final List<String> failedStudents = new ArrayList<>();
                
                for (Student student : selectedStudents) {
                    boolean success = forceDeleteStudent(student);
                    
                    if (success) {
                        successCount[0]++;
                        System.out.println("✅ Force deleted: " + student.getFullName());
                    } else {
                        failCount[0]++;
                        failedStudents.add(student.getFullName() + " (" + student.getStudentId() + ")");
                        System.out.println("❌ Failed to force delete: " + student.getFullName());
                    }
                }
                
                final int finalSuccessCount = successCount[0];
                final int finalFailCount = failCount[0];
                
                Platform.runLater(() -> {
                    progress.close();
                    
                    String message = "Force Delete Complete\n\n";
                    
                    if (finalSuccessCount > 0) {
                        message += "✅ Successfully force deleted " + finalSuccessCount + " student(s).\n";
                        message += "Note: Some orphaned data may remain in the database.\n\n";
                    }
                    
                    if (finalFailCount > 0) {
                        message += "❌ Failed to force delete " + finalFailCount + " student(s):";
                        for (String name : failedStudents) {
                            message += "\n• " + name;
                        }
                        message += "\n\nPlease check the database manually.";
                    }
                    
                    showAlert("Force Delete Results", message, 
                             finalFailCount > 0 ? Alert.AlertType.ERROR : Alert.AlertType.WARNING);
                    
                    loadStudents();
                });
            }).start();
        }
    }
    
    private boolean forceDeleteStudent(Student student) {
        try {
            DatabaseService dbService = new DatabaseService();
            try (Connection conn = dbService.getConnection(); 
                 Statement stmt = conn.createStatement()) {
                
                stmt.execute("PRAGMA foreign_keys = OFF");
                
                String deleteSql = "DELETE FROM students WHERE id = " + student.getId();
                stmt.executeUpdate(deleteSql);
                
                if (student.getUserId() > 0) {
                    String deleteUserSql = "DELETE FROM users WHERE id = " + student.getUserId();
                    stmt.executeUpdate(deleteUserSql);
                }
                
                String deleteAttendanceSql = "DELETE FROM attendance WHERE student_id = " + student.getId();
                stmt.executeUpdate(deleteAttendanceSql);
                
                stmt.execute("PRAGMA foreign_keys = ON");
                
                return true;
                
            }
        } catch (Exception e) {
            System.err.println("❌ Force delete failed for student ID " + student.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @FXML
    private void handleDeleteStudent() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.isEmpty()) {
            showAlert("No Selection", "Please select at least one student to delete.", Alert.AlertType.WARNING);
            return;
        }
        
        if (selectedStudents.size() == 1) {
            Student selected = selectedStudents.get(0);
            
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Delete Student");
            confirmation.setHeaderText("Delete Student: " + selected.getFullName());
            confirmation.setContentText("Are you sure you want to delete this student?\n\n" +
                                       "Student ID: " + selected.getStudentId() + "\n" +
                                       "Department: " + selected.getDepartment() + "\n\n" +
                                       "This will also delete:\n" +
                                       "• All attendance records\n" +
                                       "• User account\n" +
                                       "• Any related data");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Alert progress = new Alert(Alert.AlertType.INFORMATION);
                progress.setTitle("Deleting Student");
                progress.setHeaderText("Deleting student record...");
                progress.setContentText("Please wait while we delete the student and all related data.");
                progress.show();
                
                new Thread(() -> {
                    boolean success = studentService.deleteStudent(selected.getId());
                    
                    Platform.runLater(() -> {
                        progress.close();
                        
                        if (success) {
                            showAlert("Success", 
                                    "✅ Student deleted successfully!\n\n" +
                                    "Deleted: " + selected.getFullName() + "\n" +
                                    "Student ID: " + selected.getStudentId(), 
                                    Alert.AlertType.INFORMATION);
                            
                            loadStudents();
                        } else {
                            showAlert("Error", 
                                    "❌ Failed to delete student.\n\n" +
                                    "Please try the 'Delete Selected' option or use 'Force Delete'.",
                                    Alert.AlertType.ERROR);
                        }
                    });
                }).start();
            }
        } else {
            handleDeleteSelectedStudents();
        }
    }
    
    @FXML
    private void handleForceDelete() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.isEmpty()) {
            showAlert("No Selection", "Please select at least one student to force delete.", Alert.AlertType.WARNING);
            return;
        }
        
        if (selectedStudents.size() == 1) {
            Student selected = selectedStudents.get(0);
            
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("⚠️ FORCE DELETE");
            confirmation.setHeaderText("FORCE DELETE STUDENT");
            confirmation.setContentText("WARNING: This will forcefully delete the student even if there are errors.\n\n" +
                                       "Student: " + selected.getFullName() + "\n" +
                                       "ID: " + selected.getStudentId() + "\n\n" +
                                       "This may leave orphaned data in the database.\n\n" +
                                       "Are you absolutely sure?");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Alert progress = new Alert(Alert.AlertType.INFORMATION);
                progress.setTitle("Force Deleting Student");
                progress.setHeaderText("Force deleting student record...");
                progress.setContentText("Please wait while we forcefully delete the student.");
                progress.show();
                
                new Thread(() -> {
                    boolean success = forceDeleteStudent(selected);
                    
                    Platform.runLater(() -> {
                        progress.close();
                        
                        if (success) {
                            showAlert("Force Delete Complete", 
                                    "✅ Student forcefully deleted.\n" +
                                    "Note: Some orphaned data may remain in the database.",
                                    Alert.AlertType.WARNING);
                            loadStudents();
                        } else {
                            showAlert("Force Delete Failed", 
                                    "❌ Could not force delete: " + selected.getFullName(),
                                    Alert.AlertType.ERROR);
                        }
                    });
                }).start();
            }
        } else {
            handleForceDeleteSelected();
        }
    }
    
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        System.out.println("🔍 Searching for: '" + searchText + "'");
        
        if (searchText.isEmpty()) {
            loadStudents();
            return;
        }
        
        List<Student> allStudents = studentService.getAllStudents();
        studentsTable.getItems().clear();
        
        int foundCount = 0;
        for (Student student : allStudents) {
            boolean matches = false;
            
            if (student.getFirstName() != null && student.getFirstName().toLowerCase().contains(searchText)) {
                matches = true;
            } else if (student.getMiddleName() != null && student.getMiddleName().toLowerCase().contains(searchText)) {
                matches = true;
            } else if (student.getLastName() != null && student.getLastName().toLowerCase().contains(searchText)) {
                matches = true;
            } else if (student.getStudentId() != null && student.getStudentId().toLowerCase().contains(searchText)) {
                matches = true;
            } else if (student.getEmail() != null && student.getEmail().toLowerCase().contains(searchText)) {
                matches = true;
            } else if (student.getDepartment() != null && student.getDepartment().toLowerCase().contains(searchText)) {
                matches = true;
            } else if (student.getClassName() != null && student.getClassName().toLowerCase().contains(searchText)) {
                matches = true;
            }
            
            if (matches) {
                foundCount++;
                studentsTable.getItems().add(student);
            }
        }
        
        if (foundCount == 0) {
            updateStatusLabel("No students found matching '" + searchText + "'", "#ef4444");
            System.out.println("❌ No matches found");
        } else {
            updateStatusLabel("Found " + foundCount + " student(s) matching '" + searchText + "'", "#10b981");
            System.out.println("✅ Total matches: " + foundCount);
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadStudents();
        searchField.clear();
        studentsTable.getSelectionModel().clearSelection();
        updateStatusLabel("Refreshed student list", "#3b82f6");
        System.out.println("🔄 Student list refreshed");
    }
    
    @FXML
    private void handleSelectAll() {
        studentsTable.getSelectionModel().selectAll();
        int selectedCount = studentsTable.getSelectionModel().getSelectedItems().size();
        updateStatusLabel("Selected all " + selectedCount + " students", "#3b82f6");
        System.out.println("✅ Selected all " + selectedCount + " students");
    }
    
    @FXML
    private void handleGenerateReport() {
        List<Student> students = studentService.getAllStudents();
        List<String> classes = students.stream()
            .map(Student::getClassName)
            .distinct()
            .filter(className -> className != null && !className.trim().isEmpty())
            .sorted()
            .toList();
        
        if (classes.isEmpty()) {
            showAlert("No Classes", "No classes found in the database.", Alert.AlertType.WARNING);
            return;
        }
        
        ChoiceDialog<String> classDialog = new ChoiceDialog<>(classes.get(0), classes);
        classDialog.setTitle("Generate Report");
        classDialog.setHeaderText("Select a class to generate report:");
        classDialog.setContentText("Class:");
        
        Optional<String> result = classDialog.showAndWait();
        result.ifPresent(selectedClass -> {
            List<Student> classStudents = students.stream()
                .filter(student -> selectedClass.equals(student.getClassName()))
                .toList();
            
            StringBuilder report = new StringBuilder();
            report.append("📊 Attendance Report for Class: ").append(selectedClass).append("\n");
            report.append("============================================\n");
            report.append("Generated on: ").append(java.time.LocalDate.now()).append("\n");
            
            // FIXED: Use getFirstName() and getLastName() instead of getFullName()
            if (currentUser != null) {
                String fullName = currentUser.getFirstName();
                if (currentUser.getLastName() != null && !currentUser.getLastName().isEmpty()) {
                    fullName += " " + currentUser.getLastName();
                }
                report.append("Generated by: ").append(fullName).append("\n");
            } else {
                report.append("Generated by: Admin\n");
            }
            
            report.append("Total Students: ").append(classStudents.size()).append("\n\n");
            
            report.append("Student List:\n");
            report.append("-------------\n");
            for (Student student : classStudents) {
                report.append("• ").append(student.getStudentId())
                      .append(" - ").append(student.getFullName())
                      .append(" (").append(student.getDepartment()).append(")\n");
            }
            
            // Calculate attendance statistics
            try {
                int totalAttendanceRecords = 0;
                int presentCount = 0;
                int absentCount = 0;
                
                // Get attendance data for these students
                String sql = "SELECT COUNT(*) as total, " +
                            "SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) as present, " +
                            "SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absent " +
                            "FROM attendance a " +
                            "JOIN students s ON a.student_id = s.id " +
                            "WHERE s.class_name = ?";
                
                try (java.sql.Connection conn = databaseService.getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    
                    pstmt.setString(1, selectedClass);
                    java.sql.ResultSet rs = pstmt.executeQuery();
                    
                    if (rs.next()) {
                        totalAttendanceRecords = rs.getInt("total");
                        presentCount = rs.getInt("present");
                        absentCount = rs.getInt("absent");
                    }
                }
                
                report.append("\n📈 Attendance Statistics:\n");
                report.append("-------------------------\n");
                report.append("Total Attendance Records: ").append(totalAttendanceRecords).append("\n");
                report.append("Total Present: ").append(presentCount).append("\n");
                report.append("Total Absent: ").append(absentCount).append("\n");
                
                if (totalAttendanceRecords > 0) {
                    double presentPercentage = (presentCount * 100.0) / totalAttendanceRecords;
                    double absentPercentage = (absentCount * 100.0) / totalAttendanceRecords;
                    
                    report.append("Present Rate: ").append(String.format("%.1f", presentPercentage)).append("%\n");
                    report.append("Absent Rate: ").append(String.format("%.1f", absentPercentage)).append("%\n");
                    
                    // Add assessment
                    if (presentPercentage >= 90) {
                        report.append("\n✅ Excellent attendance for this class!\n");
                    } else if (presentPercentage >= 75) {
                        report.append("\n👍 Good attendance rate.\n");
                    } else if (presentPercentage >= 50) {
                        report.append("\n⚠️  Attendance needs improvement.\n");
                    } else {
                        report.append("\n❌ Poor attendance rate. Immediate action required.\n");
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error calculating attendance statistics: " + e.getMessage());
                report.append("\n📈 Attendance Statistics: Data unavailable\n");
            }
            
            TextArea textArea = new TextArea(report.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefSize(600, 500); // Increased height for statistics
            
            Dialog<Void> reportDialog = new Dialog<>();
            reportDialog.setTitle("Attendance Report");
            reportDialog.setHeaderText("Report for " + selectedClass);
            reportDialog.getDialogPane().setContent(textArea);
            reportDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            
            // Add export button
            ButtonType exportButton = new ButtonType("Export to File", ButtonBar.ButtonData.OTHER);
            reportDialog.getDialogPane().getButtonTypes().add(exportButton);
            
            reportDialog.setResultConverter(buttonType -> {
                if (buttonType == exportButton) {
                    exportReportToFile(report.toString(), selectedClass);
                }
                return null;
            });
            
            reportDialog.showAndWait();
        });
    }
    
    @FXML
    private void handleBackToDashboard() {
        // Similar to above - navigate back to dashboard within main.fxml
        try {
            BorderPane mainBorderPane = (BorderPane) searchField.getScene().getRoot();
            StackPane contentArea = (StackPane) mainBorderPane.getCenter();
            contentArea.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/attendance/view/admin_dashboard.fxml"));
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String generateStudentId() {
        List<Student> students = studentService.getAllStudents();
        int maxId = 0;
        
        for (Student student : students) {
            String studentId = student.getStudentId();
            if (studentId != null && studentId.startsWith("STU")) {
                try {
                    int id = Integer.parseInt(studentId.substring(3));
                    maxId = Math.max(maxId, id);
                } catch (NumberFormatException e) {
                    // Ignore invalid format
                }
            }
        }
        
        return String.format("STU%03d", maxId + 1);
    }
    
    @FXML
    private void handleKeyboardShortcutsHelp() {
        String helpText = """
            📋 Selection Shortcuts:
            • Ctrl+Click: Select/Deselect individual students
            • Shift+Click: Select range of students
            • Ctrl+A: Select all students
            • Click: Select single student
            
            🗑️ Deletion Shortcuts:
            • Delete Key: Delete selected students
            • Ctrl+D: Delete selected students (alternative)
            
            🔍 Navigation Shortcuts:
            • Enter in Search: Perform search
            • F5: Refresh student list
            • Escape: Clear search/selection
            
            💡 Tips:
            1. Hold Ctrl to select multiple students
            2. Hold Shift to select a range
            3. Use Ctrl+A to select all students
            4. Press Delete to delete selected students
            5. Right-click for context menu options
            6. Use F5 to refresh the student list
            """;
        
        Alert helpDialog = new Alert(Alert.AlertType.INFORMATION);
        helpDialog.setTitle("Keyboard Shortcuts Help");
        helpDialog.setHeaderText("Multi-Selection and Keyboard Shortcuts");
        helpDialog.setContentText(helpText);
        helpDialog.getDialogPane().setPrefSize(500, 400);
        helpDialog.showAndWait();
    }
    
    // NEW: Helper method to get existing classes from database
    private List<String> getExistingClasses() {
        List<String> classes = new ArrayList<>();
        try {
            String sql = "SELECT DISTINCT class_name FROM students WHERE class_name IS NOT NULL AND class_name != '' ORDER BY class_name";
            try (java.sql.Connection conn = databaseService.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                 java.sql.ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    classes.add(rs.getString("class_name"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting existing classes: " + e.getMessage());
        }
        return classes;
    }
    
    // NEW: Method to add tooltips for better UX
    private void addTooltips() {
        Tooltip searchTooltip = new Tooltip("Type to search students by name, ID, email, department, or class\nPress Enter to search");
        searchField.setTooltip(searchTooltip);
        
        Tooltip refreshTooltip = new Tooltip("Refresh student list (F5)");
        refreshButton.setTooltip(refreshTooltip);
        
        Tooltip backTooltip = new Tooltip("Return to main dashboard");
        backToDashboardButton.setTooltip(backTooltip);
    }
    
    // NEW: Handle view attendance for selected student
    private void handleViewAttendance() {
        ObservableList<Student> selectedStudents = studentsTable.getSelectionModel().getSelectedItems();
        
        if (selectedStudents.size() != 1) {
            showAlert("Invalid Selection", "Please select exactly one student to view attendance.", Alert.AlertType.WARNING);
            return;
        }
        
        Student student = selectedStudents.get(0);
        showAlert("View Attendance", 
                 "Attendance records for: " + student.getFullName() + "\n" +
                 "Student ID: " + student.getStudentId() + "\n" +
                 "Class: " + student.getClassName() + "\n\n" +
                 "This feature will be available in the next update.",
                 Alert.AlertType.INFORMATION);
    }
    
 

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private class StudentWithCredentials {
        String firstName;
        String middleName;
        String lastName;
        String email;
        String username;
        String password;
        String department;
        String className;
        
        StudentWithCredentials(String firstName, String middleName, String lastName, String email, 
                              String username, String password, String department, String className) {
            this.firstName = firstName;
            this.middleName = middleName;
            this.lastName = lastName;
            this.email = email;
            this.username = username;
            this.password = password;
            this.department = department;
            this.className = className;
        }
    }
    private void showReportDialog(String reportContent, String className) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Attendance Report - " + className);
        alert.setHeaderText("Class: " + className + " | Generated: " + java.time.LocalDate.now());
        
        TextArea textArea = new TextArea(reportContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(700, 400);
        textArea.setStyle("-fx-font-family: 'Monospace'; -fx-font-size: 12px;");
        
        // Create custom buttons
        ButtonType exportButtonType = new ButtonType("Export to File", ButtonBar.ButtonData.OTHER);
        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getDialogPane().getButtonTypes().addAll(exportButtonType, closeButtonType);
        alert.getDialogPane().setContent(textArea);
        
        // Set result converter
        alert.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                exportReportToFile(reportContent, className);
                // Return a special value to indicate dialog should stay open
                return ButtonType.CLOSE;
            }
            return dialogButton;
        });
        
        // Show dialog
        alert.showAndWait();
    }


    private void exportReportToFile(String reportContent, String className) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Report As");
            
            // Clean up class name for filename
            String cleanClassName = className.replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = "Attendance_Report_" + cleanClassName + "_" + 
                             java.time.LocalDate.now() + ".txt";
            fileChooser.setInitialFileName(fileName);
            
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            // Get the current window
            Stage stage = (Stage) studentsTable.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                    writer.write(reportContent);
                    
                    // Show success message
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Report saved to:\n" + file.getAbsolutePath());
                    successAlert.showAndWait();
                }
            }
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Failed to save file: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    
    private void showReportWindow(String reportContent, String className) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Attendance Report");
        alert.setHeaderText("Report for " + className);
        
        TextArea textArea = new TextArea(reportContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(700, 400);
        
        alert.getDialogPane().setContent(textArea);
        
        // Create buttons
        ButtonType exportButton = new ButtonType("Export to File");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getDialogPane().getButtonTypes().setAll(exportButton, closeButton);
        
        // Get the actual button nodes
        Button exportBtn = (Button) alert.getDialogPane().lookupButton(exportButton);
        Button closeBtn = (Button) alert.getDialogPane().lookupButton(closeButton);
        
        // Set button actions directly
        exportBtn.setOnAction(e -> {
            exportReportToFile(reportContent, className);
            // DON'T close the alert - let user click Close
        });
        
        closeBtn.setOnAction(e -> {
            alert.close(); // This closes the alert
        });
        
        // Show and wait
        alert.showAndWait();
    }
    
    
    private class UsernamePasswordReset {
        String username;
        String password;
        
        UsernamePasswordReset(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}