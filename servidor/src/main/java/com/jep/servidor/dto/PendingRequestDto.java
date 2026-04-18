package com.jep.servidor.dto;

public class PendingRequestDto {
    private Long id;
    private Long senderId;
    private String senderUsername;

    public PendingRequestDto(Long id, Long senderId, String senderUsername) {
        this.id = id;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
}