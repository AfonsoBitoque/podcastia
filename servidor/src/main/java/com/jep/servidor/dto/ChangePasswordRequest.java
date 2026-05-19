package com.jep.servidor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    
    @NotBlank(message = "A password atual é obrigatória")
    private String currentPassword;

    @NotBlank(message = "A nova password é obrigatória")
    @Size(min = 8, message = "A nova password deve ter pelo menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "A nova password deve ter pelo menos 8 caracteres, uma letra maiuscula e um numero"
    )
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
