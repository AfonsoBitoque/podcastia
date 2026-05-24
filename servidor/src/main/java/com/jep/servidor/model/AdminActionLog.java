package com.jep.servidor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA de auditoria de ações administrativas.
 *
 * <p>Cada registo representa uma ação executada por um administrador
 * (ex: ocultar podcast, eliminar utilizador, exportar relatório),
 * com dados de contexto HTTP ({@code ipAddress}, {@code userAgent})
 * e resultado ({@code successful}, {@code errorMessage}).
 *
 * <p><b>Tabela:</b> {@code admin_action_logs}
 *
 * <p>O construtor padrão preenche automaticamente {@code timestamp}
 * com {@link java.time.LocalDateTime#now()}.
 *
 * @see com.jep.servidor.dto.AdminActionLogDTO
 * @see com.jep.servidor.repository.AdminActionLogRepository
 */
@Entity
@Table(name = "admin_action_logs")
public class AdminActionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Username do administrador que executou a ação. */
    @Column(nullable = false)
    private String adminUsername;
    /** Email do administrador. */
    @Column(nullable = false)
    private String adminEmail;
    /** Código da ação (ex: {@code "DELETE_PODCAST"}, {@code "RESET_PASSWORD"}). */
    @Column(nullable = false)
    private String action;
    /** Tipo de entidade alvo: {@code PODCAST}, {@code USER} ou {@code SYSTEM}. */
    @Column(nullable = false)
    private String targetType;
    /** ID da entidade alvo ({@code null} para ações de sistema). */
    private Long targetId;
    /** Nome/título da entidade alvo para referência humana. */
    private String targetName;
    /** Descrição detalhada da ação. Armazenado como TEXT. */
    @Column(columnDefinition = "TEXT")
    private String description;
    /** Endereço IP do administrador no momento da ação. */
    private String ipAddress;
    /** User-Agent do browser do administrador. Armazenado como TEXT. */
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    /** Data e hora em que a ação foi executada (preenchido automaticamente no construtor). */
    @Column(nullable = false)
    private LocalDateTime timestamp;
    /** {@code true} se a ação foi concluída com sucesso. */
    @Column(nullable = false)
    private boolean successful;
    /** Mensagem de erro em caso de falha ({@code null} se bem-sucedido). Armazenado como TEXT. */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /** Construtor padrão. Inicializa {@code timestamp} com a data/hora atual. */
    public AdminActionLog() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Construtor de conveniência para os campos obrigatórios.
     *
     * @param adminUsername username do administrador.
     * @param adminEmail    email do administrador.
     * @param action        código da ação.
     * @param targetType    tipo do alvo ({@code PODCAST}, {@code USER}, {@code SYSTEM}).
     */
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
