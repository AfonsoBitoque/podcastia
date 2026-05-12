package com.jep.servidor.dto;

import java.time.LocalDateTime;

/**
 * DTO for Admin Action Logging
 */
public class AdminActionLogDTO {
    
    private Long id;
    private String adminUsername;
    private String adminEmail;
    private String action;
    private String targetType; // PODCAST, USER, SYSTEM
    private Long targetId;
    private String targetName;
    private String description;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private boolean successful;
    private String errorMessage;
    
    public AdminActionLogDTO() {}
    
    public AdminActionLogDTO(String adminUsername, String adminEmail, String action,
                             String targetType, Long targetId, String targetName,
                             String description, String ipAddress, String userAgent,
                             LocalDateTime timestamp, boolean successful, String errorMessage) {
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.description = description;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
        this.successful = successful;
        this.errorMessage = errorMessage;
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
