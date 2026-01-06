package attendance.controller;

import attendance.model.Teacher;
import attendance.service.TeacherService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.List;

public class ManageTeachersController {
    
    @FXML private TableView<Teacher> teachersTable;
    @FXML private TableColumn<Teacher, Integer> idColumn;
    @FXML private TableColumn<Teacher, String> firstNameColumn;
    @FXML private TableColumn<Teacher, String> lastNameColumn;
    @FXML private TableColumn<Teacher, String> emailColumn;
    @FXML private TableColumn<Teacher, String> departmentColumn;
    @FXML private TableColumn<Teacher, Integer> classesCountColumn;
    
    // NOTE: Removed statusColumn reference since Teacher model doesn't have status field
    
    @FXML private TextField searchField;
    @FXML private Button addTeacherBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Label statsLabel;
    
    private TeacherService teacherService;
    private ObservableList<Teacher> teachersList;
    private FilteredList<Teacher> filteredTeachers;
    
    @FXML
    public void initialize() {
        System.out.println("✅ ManageTeachersController initialized");
        
        teacherService = new TeacherService();
        teachersList = FXCollections.observableArrayList();
        filteredTeachers = new FilteredList<>(teachersList, p -> true);
        
        // Configure table columns
        setupTableColumns();
        
        // Load initial data
        loadTeachers();
        
        // Setup search filter
        setupSearchFilter();
        
        // Update statistics
        updateStatistics();
        
        System.out.println("✅ Teacher management system ready");
    }
    
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        classesCountColumn.setCellValueFactory(new PropertyValueFactory<>("classesAssigned"));
    }
    
    private void loadTeachers() {
        teachersList.clear();
        List<Teacher> teachers = teacherService.getAllTeachers();
        if (teachers != null) {
            teachersList.addAll(teachers);
            System.out.println("✅ Loaded " + teachers.size() + " teachers");
        } else {
            System.out.println("⚠️ No teachers found or error loading teachers");
        }
        
        // Set up sorted list for table
        SortedList<Teacher> sortedTeachers = new SortedList<>(filteredTeachers);
        sortedTeachers.comparatorProperty().bind(teachersTable.comparatorProperty());
        teachersTable.setItems(sortedTeachers);
    }
    
    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredTeachers.setPredicate(teacher -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                String firstName = teacher.getFirstName() != null ? teacher.getFirstName().toLowerCase() : "";
                String lastName = teacher.getLastName() != null ? teacher.getLastName().toLowerCase() : "";
                String email = teacher.getEmail() != null ? teacher.getEmail().toLowerCase() : "";
                String department = teacher.getDepartment() != null ? teacher.getDepartment().toLowerCase() : "";
                
                return firstName.contains(lowerCaseFilter) ||
                       lastName.contains(lowerCaseFilter) ||
                       email.contains(lowerCaseFilter) ||
                       department.contains(lowerCaseFilter);
            });
        });
    }
    
    private void updateStatistics() {
        int totalTeachers = teachersList.size();
        statsLabel.setText("Total Teachers: " + totalTeachers);
    }
    
    @FXML
    private void handleEditTeacher() {
        Teacher selectedTeacher = teachersTable.getSelectionModel().getSelectedItem();
        
        if (selectedTeacher == null) {
            showAlert("No Selection", "Please select a teacher to edit.");
            return;
        }
        
        System.out.println("✏️ Editing teacher: " + selectedTeacher.getFirstName() + " " + selectedTeacher.getLastName());
        
        // Create edit dialog
        Dialog<Teacher> dialog = new Dialog<>();
        dialog.setTitle("Edit Teacher");
        dialog.setHeaderText("Edit teacher details");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        
        TextField firstNameField = new TextField(selectedTeacher.getFirstName());
        TextField lastNameField = new TextField(selectedTeacher.getLastName());
        TextField emailField = new TextField(selectedTeacher.getEmail());
        TextField departmentField = new TextField(selectedTeacher.getDepartment());
        TextField phoneField = new TextField(selectedTeacher.getPhone() != null ? selectedTeacher.getPhone() : "");
        phoneField.setPromptText("Phone (optional)");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Leave blank to keep current password");
        
        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Department:"), 0, 3);
        grid.add(departmentField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("New Password:"), 0, 5);
        grid.add(passwordField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String email = emailField.getText().trim();
                String department = departmentField.getText().trim();
                String phone = phoneField.getText().trim();
                String newPassword = passwordField.getText().trim();
                
                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || department.isEmpty()) {
                    showAlert("Validation Error", "All fields except password are required.");
                    return null;
                }
                
                Teacher updatedTeacher = new Teacher();
                updatedTeacher.setId(selectedTeacher.getId());
                updatedTeacher.setFirstName(firstName);
                updatedTeacher.setLastName(lastName);
                updatedTeacher.setEmail(email);
                updatedTeacher.setDepartment(department);
                updatedTeacher.setPhone(phone);
                
                boolean success = teacherService.updateTeacher(updatedTeacher, 
                    newPassword.isEmpty() ? null : newPassword);
                
                if (success) {
                    showAlert("Success", "Teacher updated successfully!");
                    return updatedTeacher;
                } else {
                    showAlert("Error", "Failed to update teacher.");
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            loadTeachers();
            updateStatistics();
        });
    }
    
    @FXML
    private void handleDeleteTeacher() {
        Teacher selectedTeacher = teachersTable.getSelectionModel().getSelectedItem();
        
        if (selectedTeacher == null) {
            showAlert("No Selection", "Please select a teacher to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Teacher");
        confirm.setContentText(String.format("Are you sure you want to delete %s %s?\n\nThis action cannot be undone.",
                selectedTeacher.getFirstName(), selectedTeacher.getLastName()));
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = teacherService.deleteTeacher(selectedTeacher.getId());
                if (success) {
                    showAlert("Success", "Teacher deleted successfully.");
                    loadTeachers();
                    updateStatistics();
                } else {
                    showAlert("Error", "Failed to delete teacher. They may be assigned to classes.");
                }
            }
        });
    }
    
    @FXML
    private void handleSearch() {
        // Search is already handled by the filter listener
        System.out.println("🔍 Searching for: " + searchField.getText());
    }
    
    @FXML
    private void handleAddTeacher() {
        System.out.println("➕ Add Teacher button clicked");
        
        // Create dialog
        Dialog<Teacher> dialog = new Dialog<>();
        dialog.setTitle("Add New Teacher");
        dialog.setHeaderText("Enter teacher details");
        
        // Set up form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        
        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        
        TextField departmentField = new TextField();
        departmentField.setPromptText("Department");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        
        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Department:"), 0, 3);
        grid.add(departmentField, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passwordField, 1, 4);
        grid.add(new Label("Confirm Password:"), 0, 5);
        grid.add(confirmPasswordField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Add buttons
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Validate and save
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String email = emailField.getText().trim();
                String department = departmentField.getText().trim();
                String password = passwordField.getText();
                String confirmPassword = confirmPasswordField.getText();
                
                // Validation
                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || 
                    department.isEmpty() || password.isEmpty()) {
                    showAlert("Validation Error", "All fields are required.");
                    return null;
                }
                
                if (!password.equals(confirmPassword)) {
                    showAlert("Validation Error", "Passwords do not match.");
                    return null;
                }
                
                // Email validation
                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    showAlert("Validation Error", "Please enter a valid email address.");
                    return null;
                }
                
                Teacher teacher = new Teacher();
                teacher.setFirstName(firstName);
                teacher.setLastName(lastName);
                teacher.setEmail(email);
                teacher.setDepartment(department);
                
                boolean success = teacherService.addTeacher(teacher, password);
                if (success) {
                    showAlert("Success", "Teacher added successfully!");
                    return teacher;
                } else {
                    showAlert("Error", "Failed to add teacher. Email might already exist.");
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            loadTeachers();
            updateStatistics();
        });
    }
    
    @FXML
    private void handleRefresh() {
        System.out.println("🔄 Refreshing teacher list...");
        loadTeachers();
        updateStatistics();
        searchField.clear();
        showAlert("Refreshed", "Teacher list has been refreshed.");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}