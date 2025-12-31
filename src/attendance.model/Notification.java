
package attendance.model;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int senderId;
    private int receiverId;
    private String senderName;
    private String title;
    private String message;
    private LocalDateTime timestamp;
    private boolean read;
    private String type; // "ANNOUNCEMENT", "ATTENDANCE_ALERT", "REMINDER", "SYSTEM"
    
    public Notification() {}
    
    public Notification(int senderId, int receiverId, String title, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.title = title;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.read = false;
        this.type = "ANNOUNCEMENT";
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    
    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}