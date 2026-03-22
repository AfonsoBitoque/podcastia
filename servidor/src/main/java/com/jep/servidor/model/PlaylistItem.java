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
 * Entidade que representa um episódio (podcast) dentro de uma playlist.
 */
@Entity
@Table(name = "playlist_items",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"playlist_id", "podcast_id"}),
        @UniqueConstraint(columnNames = {"playlist_id", "position"})
    },
    indexes = {
        @Index(columnList = "playlist_id")
    }
)
public class PlaylistItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @ManyToOne(optional = false)
  @JoinColumn(name = "podcast_id", nullable = false)
  private Podcast podcast;

  @Column(nullable = false)
  private int position;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Playlist getPlaylist() {
    return playlist;
  }

  public void setPlaylist(Playlist playlist) {
    this.playlist = playlist;
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
}
