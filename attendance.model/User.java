package attendance.model;

public class User {
    private int id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private String email;
    
    // Constructors
    public User() {
    }
    
    public User(int id, String username, String firstName, String lastName, String role) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }
    
    // Alternative constructor with email
    public User(int id, String username, String firstName, String lastName, String role, String email) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.email = email;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
    
    public boolean isTeacher() {
        return "TEACHER".equals(role);
    }
    
    public boolean isStudent() {
        return "STUDENT".equals(role);
    }
    
    @Override
    public String toString() {
        return getFullName() + " (" + role + ")";
    }
}