package com.jep.servidor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for logging admin actions
 */
@Entity
@Table(name = "admin_action_logs")
public class AdminActionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String adminUsername;
    
    @Column(nullable = false)
    private String adminEmail;
    
    @Column(nullable = false)
    private String action;
    
    @Column(nullable = false)
    private String targetType; // PODCAST, USER, SYSTEM
    
    private Long targetId;
    
    private String targetName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private boolean successful;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    // Constructors
    public AdminActionLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AdminActionLog(String adminUsername, String adminEmail, String action, String targetType) {
        this();
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetType = targetType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
