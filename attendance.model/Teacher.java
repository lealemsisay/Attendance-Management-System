package attendance.model;

public class Teacher {
    private int id;
    private String firstName;
    private String middleName; // ADDED
    private String lastName;
    private String email;
    private String department;
    private String phone;
    private int userId;
    private int classesAssigned;
    
    // Constructors
    public Teacher() {
        this.classesAssigned = 0;
    }
    
    public Teacher(int id, String firstName, String lastName, String email, String department) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.classesAssigned = 0;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getMiddleName() { return middleName != null ? middleName : ""; } // ADDED
    public void setMiddleName(String middleName) { this.middleName = middleName; } // ADDED
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getClassesAssigned() { return classesAssigned; }
    public void setClassesAssigned(int classesAssigned) { 
        this.classesAssigned = classesAssigned; 
    }
    
    // Helper methods
    public String getFullName() {
        if (middleName != null && !middleName.trim().isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }
    
    @Override
    public String toString() {
        return getFullName() + " (" + department + ")";
    }
}