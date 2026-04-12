package com.jep.servidor.dto;

public class RelationStatusDto {
    private String status;
    private boolean canRequest;

    public RelationStatusDto(String status, boolean canRequest) {
        this.status = status;
        this.canRequest = canRequest;
    }

    // Getters e Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCanRequest() {
        return canRequest;
    }

    public void setCanRequest(boolean canRequest) {
        this.canRequest = canRequest;
    }
}
