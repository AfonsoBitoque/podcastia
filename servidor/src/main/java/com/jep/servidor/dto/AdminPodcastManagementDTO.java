package com.jep.servidor.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Admin Podcast Management
 */
public class AdminPodcastManagementDTO {
    
    private Long id;
    private String titulo;
    private String author;
    private List<String> tags;
    private int duracao;
    private boolean explicitContent;
    private boolean hidden;
    private boolean featured;
    private boolean publico;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private long totalPlays;
    private long totalListeningTime;
    private double averageRating;
    private String coverImagePath;
    private String conteudoPath;
    
    // Admin-specific fields
    private String lastModifiedBy;
    private String modificationReason;
    private List<String> violationReasons;
    
    public AdminPodcastManagementDTO() {}
    
    public AdminPodcastManagementDTO(Long id, String titulo, String author, List<String> tags,
                                     int duracao, boolean explicitContent, boolean hidden,
                                     boolean featured, boolean publico, boolean available,
                                     LocalDateTime createdAt, LocalDateTime lastModified,
                                     long totalPlays, long totalListeningTime,
                                     double averageRating, String coverImagePath,
                                     String conteudoPath) {
        this.id = id;
        this.titulo = titulo;
        this.author = author;
        this.tags = tags;
        this.duracao = duracao;
        this.explicitContent = explicitContent;
        this.hidden = hidden;
        this.featured = featured;
        this.publico = publico;
        this.available = available;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.totalPlays = totalPlays;
        this.totalListeningTime = totalListeningTime;
        this.averageRating = averageRating;
        this.coverImagePath = coverImagePath;
        this.conteudoPath = conteudoPath;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public int getDuracao() { return duracao; }
    public void setDuracao(int duracao) { this.duracao = duracao; }
    
    public boolean isExplicitContent() { return explicitContent; }
    public void setExplicitContent(boolean explicitContent) { this.explicitContent = explicitContent; }
    
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    
    public boolean isPublico() { return publico; }
    public void setPublico(boolean publico) { this.publico = publico; }
    
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public long getTotalPlays() { return totalPlays; }
    public void setTotalPlays(long totalPlays) { this.totalPlays = totalPlays; }
    
    public long getTotalListeningTime() { return totalListeningTime; }
    public void setTotalListeningTime(long totalListeningTime) { this.totalListeningTime = totalListeningTime; }
    
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    
    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }
    
    public String getConteudoPath() { return conteudoPath; }
    public void setConteudoPath(String conteudoPath) { this.conteudoPath = conteudoPath; }
    
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
    
    public String getModificationReason() { return modificationReason; }
    public void setModificationReason(String modificationReason) { this.modificationReason = modificationReason; }
    
    public List<String> getViolationReasons() { return violationReasons; }
    public void setViolationReasons(List<String> violationReasons) { this.violationReasons = violationReasons; }
}
