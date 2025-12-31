package attendance.model;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private int id;
    private String studentId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String department;
    private int userId;
    
    // ONE Class Assignment (Administrative)
    private Integer administrativeClassId;
    private String administrativeClassName;
    
    // MANY Subject Enrollment
    private List<Integer> enrolledSubjectIds;
    private List<String> enrolledSubjectNames;
    private int subjectCount;
    
    // Constructors
    public Student() {
        this.enrolledSubjectIds = new ArrayList<>();
        this.enrolledSubjectNames = new ArrayList<>();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getMiddleName() { return middleName != null ? middleName : ""; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getFullName() {
        if (middleName != null && !middleName.trim().isEmpty()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    // Administrative Class (ONE)
    public Integer getAdministrativeClassId() { return administrativeClassId; }
    public void setAdministrativeClassId(Integer administrativeClassId) { 
        this.administrativeClassId = administrativeClassId; 
    }
    
    public String getAdministrativeClassName() { return administrativeClassName; }
    public void setAdministrativeClassName(String administrativeClassName) { 
        this.administrativeClassName = administrativeClassName; 
    }
    
    // Subject Enrollment (MANY)
    public List<Integer> getEnrolledSubjectIds() { return enrolledSubjectIds; }
    public void setEnrolledSubjectIds(List<Integer> enrolledSubjectIds) { 
        this.enrolledSubjectIds = enrolledSubjectIds; 
    }
    
    public List<String> getEnrolledSubjectNames() { return enrolledSubjectNames; }
    public void setEnrolledSubjectNames(List<String> enrolledSubjectNames) { 
        this.enrolledSubjectNames = enrolledSubjectNames; 
    }
    
    public int getSubjectCount() { return subjectCount; }
    public void setSubjectCount(int subjectCount) { this.subjectCount = subjectCount; }
    
    // For backward compatibility
    public String getSubjectsAsString() {
        if (enrolledSubjectNames != null && !enrolledSubjectNames.isEmpty()) {
            return String.join(", ", enrolledSubjectNames);
        }
        return "No subjects enrolled";
    }
    
    public int getClassCount() {
        return subjectCount; // For backward compatibility
    }
    
    // Helper methods
    public boolean isEnrolledInSubject(int subjectId) {
        return enrolledSubjectIds != null && enrolledSubjectIds.contains(subjectId);
    }
    
    public void addSubject(int subjectId, String subjectName) {
        if (!isEnrolledInSubject(subjectId)) {
            enrolledSubjectIds.add(subjectId);
            enrolledSubjectNames.add(subjectName);
            subjectCount++;
        }
    }
public void removeSubject(int subjectId, String subjectName) {
        if (isEnrolledInSubject(subjectId)) {
            enrolledSubjectIds.remove(Integer.valueOf(subjectId));
            enrolledSubjectNames.remove(subjectName);
            subjectCount = Math.max(0, subjectCount - 1);
        }
    }
    
    @Override
    public String toString() {
        return getFullName() + " (" + studentId + ") - Admin Class: " + 
               administrativeClassName + " - Subjects: " + subjectCount;
    }
}