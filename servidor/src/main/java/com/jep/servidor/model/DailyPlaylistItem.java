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
 * Entidade que representa um item (podcast) numa playlist diária.
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
