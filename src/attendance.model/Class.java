package attendance.model;

public class Class {
    private int id;
    private String className;
    private String roomNumber;
    private String schedule;
    private int teacherId;
    private String teacherName;
    private int studentCount;
    private String classType; // "ADMINISTRATIVE" or "SUBJECT"
    private String subjectCode; // For subjects only
    
    // Constructors
    public Class() {
        this.classType = "SUBJECT"; // Default to subject
    }
    
    // Constructor for subject
    public Class(int id, String className, String subjectCode, String roomNumber, 
                 String schedule, int teacherId, String teacherName, int studentCount) {
        this.id = id;
        this.className = className;
        this.subjectCode = subjectCode;
        this.roomNumber = roomNumber;
        this.schedule = schedule;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.studentCount = studentCount;
        this.classType = "SUBJECT";
    }
    
    // Constructor for administrative class
    public Class(int id, String className, String roomNumber, String schedule, 
                 int teacherId, String teacherName, int studentCount) {
        this.id = id;
        this.className = className;
        this.roomNumber = roomNumber;
        this.schedule = schedule;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.studentCount = studentCount;
        this.classType = "ADMINISTRATIVE";
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { 
        this.className = className; 
        // Auto-detect type based on class name pattern
        detectClassType();
    }
    
    public String getSubjectCode() { 
        return subjectCode != null ? subjectCode : ""; 
    }
    public void setSubjectCode(String subjectCode) { 
        this.subjectCode = subjectCode; 
        // If subject code is set, it's likely a subject
        if (subjectCode != null && !subjectCode.trim().isEmpty()) {
            this.classType = "SUBJECT";
        }
    }
    
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    
    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    
    // Class type methods
    public String getClassType() { return classType; }
    public void setClassType(String classType) { this.classType = classType; }
    
    public boolean isAdministrative() { 
        return "ADMINISTRATIVE".equals(classType); 
    }
    
    public boolean isSubject() { 
        return "SUBJECT".equals(classType); 
    }
    
    // Auto-detect class type based on naming patterns
    private void detectClassType() {
        if (className == null) {
            this.classType = "SUBJECT";
            return;
        }
        
        String name = className.trim().toLowerCase();
        
        // Patterns that indicate administrative class
        if (name.matches("^class\\s*\\d+[a-z]?$")           // "Class 10A"
            name.matches("^\\d+[a-z]$")                    // "10A"
            name.matches("^grade\\s*\\d+$")                // "Grade 10"
            name.matches("^section\\s*[a-z]$")             // "Section A"
            name.matches("^[a-z]\\d+$")                    // "A10"
            name.contains("homeroom") 
            name.contains("room") ||
className.length() <= 5) {                       // Short names
            this.classType = "ADMINISTRATIVE";
        } else {
            this.classType = "SUBJECT";
        }
    }
    
    // Display methods
    public String getDisplayName() {
        if (isSubject()) {
            if (subjectCode != null && !subjectCode.trim().isEmpty()) {
                return subjectCode + " - " + className;
            }
            return "Subject: " + className;
        } else {
            return "Class: " + className;
        }
    }
    
    public String getFullDisplay() {
        return getDisplayName() + " (Room: " + roomNumber + ", " + schedule + ")";
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
}