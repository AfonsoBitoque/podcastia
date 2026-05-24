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

/**
 * Entidade JPA que representa um podcast incluído numa playlist diária.
 *
 * <p>Cada item liga uma {@link DailyPlaylist} a um {@link Podcast},
 * com a posição na lista (1-indexed) e uma pontuação de relevância
 * calculada pelo {@link com.jep.servidor.service.DailyPlaylistService}
 * com base nos pontos de afinidade do utilizador por categoria.
 *
 * <p>A combinação ({@code daily_playlist_id}, {@code podcast_id})
 * é única para evitar duplicados na mesma playlist.
 *
 * <p><b>Tabela:</b> {@code daily_playlist_items}
 *
 * @see DailyPlaylist
 * @see com.jep.servidor.dto.DailyPlaylistItemResponse
 */
@Entity
@Table(name = "daily_playlist_items",
    indexes = {
        @Index(columnList = "daily_playlist_id"),
        @Index(columnList = "podcast_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"daily_playlist_id", "podcast_id"})
    }
)
public class DailyPlaylistItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "daily_playlist_id", nullable = false)
  private DailyPlaylist dailyPlaylist;

  @ManyToOne(optional = false)
  @JoinColumn(name = "podcast_id", nullable = false)
  private Podcast podcast;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false)
  private float relevanceScore; // Score de relevância baseado nas preferências do utilizador

  // Getters e Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DailyPlaylist getDailyPlaylist() {
    return dailyPlaylist;
  }

  public void setDailyPlaylist(DailyPlaylist dailyPlaylist) {
    this.dailyPlaylist = dailyPlaylist;
  }

  public Podcast getPodcast() {
    return podcast;
  }

  public void setPodcast(Podcast podcast) {
    this.podcast = podcast;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public float getRelevanceScore() {
    return relevanceScore;
  }

  public void setRelevanceScore(float relevanceScore) {
    this.relevanceScore = relevanceScore;
  }
}
