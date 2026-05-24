package com.jep.servidor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de pedido de alteração de password do utilizador.
 *
 * <p>Usado pelo endpoint {@code PUT /users/{userId}/password} em
 * {@link com.jep.servidor.controller.UserController}. Exige a password atual
 * para confirmação de identidade antes de aceitar a nova.
 *
 * <p><b>Regras de validação da nova password:</b> mínimo 8 caracteres,
 * pelo menos uma letra maiúscula e um dígito.
 */
public class ChangePasswordRequest {
    
    /** Password atual do utilizador (para confirmação de identidade). */
    @NotBlank(message = "A password atual é obrigatória")
    private String currentPassword;

    /** Nova password: mín. 8 caracteres, pelo menos uma maiúscula e um dígito. */
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
