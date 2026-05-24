package com.jep.servidor.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade JPA central que representa um podcast gerado ou importado na plataforma.
 *
 * <p>Os podcasts são gerados pelo {@link com.jep.servidor.service.PodcastGenerationService}
 * (Gemini API + edge-tts) e associados a um {@link User}. O ficheiro áudio MP3 é
 * referenciado por {@code conteudoPath} e servido via streaming.
 *
 * <p>Campos de moderação ({@code explicitContent}, {@code hidden}, {@code featured})
 * são geridos exclusivamente pelo {@link com.jep.servidor.controller.AdminController}.
 *
 * <p>As {@code tags} são uma coleção de {@link PodcastTag} armazenada na
 * tabela {@code podcast_tags} com índice composto para pesquisa eficiente.
 *
 * <p><b>Tabela:</b> {@code podcasts}
 *
 * @see PodcastTag
 * @see com.jep.servidor.repository.PodcastRepository
 */
@Entity
@Table(name = "podcasts",
    indexes = {
    @Index(columnList = "user_id"),
    @Index(columnList = "duracao")
    }
)
public class Podcast {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String titulo;

  @Column(nullable = false)
  private int duracao;

  // Caminho do ficheiro mp3 guardado no servidor
  @Column(nullable = false)
  private String conteudoPath;

  @Column(name = "cover_image_path")
  private String coverImagePath;

  @Column(nullable = false)
  private boolean publico = false;

  @Column(nullable = false)
  private boolean available = true;

  // Admin fields
  @Column(nullable = false)
  private boolean explicitContent = false;

  @Column(nullable = false)
  private boolean hidden = false;

  @Column(nullable = false)
  private boolean featured = false;

  @Column(name = "last_modified")
  private LocalDateTime lastModified;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

    @ElementCollection
    @CollectionTable(
      name = "podcast_tags",
      joinColumns = @JoinColumn(name = "podcast_id"),
      indexes = {
        @Index(columnList = "tag"),
        @Index(columnList = "podcast_id,tag")
      }
    )
  @Enumerated(EnumType.STRING)
  @Column(name = "tag")
  private List<PodcastTag> tags;

  @jakarta.persistence.PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }

  /**
   * Construtor padrão.
   */
  public Podcast() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public int getDuracao() {
    return duracao;
  }

  public void setDuracao(int duracao) {
    this.duracao = duracao;
  }

  public String getConteudoPath() {
    return conteudoPath;
  }

  public void setConteudoPath(String conteudoPath) {
    this.conteudoPath = conteudoPath;
  }

  public String getCoverImagePath() {
    return coverImagePath;
  }

  public void setCoverImagePath(String coverImagePath) {
    this.coverImagePath = coverImagePath;
  }

  public List<PodcastTag> getTags() {
    return tags;
  }

  public void setTags(List<PodcastTag> tags) {
    this.tags = tags;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public boolean isPublico() {
    return publico;
  }

  public void setPublico(boolean publico) {
    this.publico = publico;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public boolean isExplicitContent() {
    return explicitContent;
  }

  public void setExplicitContent(boolean explicitContent) {
    this.explicitContent = explicitContent;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public boolean isFeatured() {
    return featured;
  }

  public void setFeatured(boolean featured) {
    this.featured = featured;
  }

  public LocalDateTime getLastModified() {
    return lastModified;
  }

  public void setLastModified(LocalDateTime lastModified) {
    this.lastModified = lastModified;
  }

  /**
   * Retorna a URL de streaming do áudio para o frontend.
   * Este campo não é persistido na base de dados, é calculado dinamicamente.
   *
   * @return URL do endpoint de streaming de áudio.
   */
  @com.fasterxml.jackson.annotation.JsonProperty("audioUrl")
  public String getAudioUrl() {
    return "/api/podcasts/" + id + "/audio";
  }
}
