package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Entidade JPA que regista a relação de favorito entre um utilizador e um podcast.
 *
 * <p>A combinação ({@code user_id}, {@code podcast_id}) é única, garantindo
 * que cada utilizador só pode favoritar um podcast uma vez (toggle).
 *
 * <p>O toggle é gerido pelo
 * {@link com.jep.servidor.controller.PodcastFavoriteController}:
 * se o favorito já existir, é removido; caso contrário, é criado.
 *
 * <p><b>Tabela:</b> {@code podcast_favorites}
 *
 * @see com.jep.servidor.repository.PodcastFavoriteRepository
 */
@Entity
@Table(
    name = "podcast_favorites",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "podcast_id"})
    },
    indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "podcast_id"),
        @Index(columnList = "user_id,podcast_id")
    }
)
public class PodcastFavorite {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "podcast_id", nullable = false)
  private Podcast podcast;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public PodcastFavorite() {
  }

  public PodcastFavorite(User user, Podcast podcast) {
    this.user = user;
    this.podcast = podcast;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Podcast getPodcast() {
    return podcast;
  }

  public void setPodcast(Podcast podcast) {
    this.podcast = podcast;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
