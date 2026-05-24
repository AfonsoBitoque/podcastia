package com.jep.servidor.dto;

import java.time.LocalDateTime;

/**
 * DTO de transferência de dados para logs de ações administrativas.
 *
 * <p>Representa um registo de auditoria de uma ação efetuada por um administrador,
 * incluindo dados de identidade, alvo, contexto HTTP e resultado da operação.
 * Usado pelo {@link com.jep.servidor.controller.AdminController} para expor os logs
 * gerados pelo {@link com.jep.servidor.model.AdminActionLog}.
 *
 * <p><b>Campos de alvo ({@code targetType}):</b> {@code PODCAST}, {@code USER}, {@code SYSTEM}.
 */
public class AdminActionLogDTO {
    
    /** ID único do registo de log. */
    private Long id;
    /** Username do administrador que executou a ação. */
    private String adminUsername;
    /** Email do administrador. */
    private String adminEmail;
    /** Código da ação executada (ex: {@code "DELETE_PODCAST"}, {@code "RESET_PASSWORD"}). */
    private String action;
    /** Tipo de entidade alvo: {@code PODCAST}, {@code USER} ou {@code SYSTEM}. */
    private String targetType;
    /** ID da entidade alvo (podcast, utilizador, etc.). */
    private Long targetId;
    /** Nome/título da entidade alvo para referência humana. */
    private String targetName;
    /** Descrição detalhada da ação executada. */
    private String description;
    /** Endereço IP do administrador no momento da ação. */
    private String ipAddress;
    /** User-Agent do browser/cliente do administrador. */
    private String userAgent;
    /** Data e hora em que a ação foi executada. */
    private LocalDateTime timestamp;
    /** {@code true} se a ação foi concluída com sucesso; {@code false} se falhou. */
    private boolean successful;
    /** Mensagem de erro em caso de falha; {@code null} se bem-sucedido. */
    private String errorMessage;
    
    /** Construtor padrão para deserialização. */
    public AdminActionLogDTO() {}

    /**
     * Construtor completo para criação programática do log.
     *
     * @param adminUsername   username do administrador.
     * @param adminEmail      email do administrador.
     * @param action          código da ação.
     * @param targetType      tipo do alvo ({@code PODCAST}, {@code USER}, {@code SYSTEM}).
     * @param targetId        ID do alvo.
     * @param targetName      nome do alvo.
     * @param description     descrição da ação.
     * @param ipAddress       IP do administrador.
     * @param userAgent       user-agent do cliente.
     * @param timestamp       data/hora da ação.
     * @param successful      {@code true} se bem-sucedido.
     * @param errorMessage    mensagem de erro (ou {@code null}).
     */
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
