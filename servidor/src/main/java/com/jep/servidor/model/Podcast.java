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

import java.util.List;

/**
 * Entidade que representa um podcast no sistema.
 */
@Entity
@Table(name = "podcasts",
    indexes = {
        @Index(columnList = "user_id")
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

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ElementCollection
  @CollectionTable(name = "podcast_tags", joinColumns = @JoinColumn(name = "podcast_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "tag")
  private List<PodcastTag> tags;

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
}
