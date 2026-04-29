package com.jep.servidor.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa uma playlist diária gerada automaticamente para um utilizador.
 * Esta playlist é atualizada diariamente com base nas preferências do utilizador.
 */
@Entity
@Table(name = "daily_playlists",
    indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "playlist_date"),
        @Index(columnList = "user_id, playlist_date", unique = true)
    }
)
public class DailyPlaylist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, name = "playlist_date")
  private LocalDate playlistDate;

  @Column(nullable = false)
  private String title;

  @Column(length = 500)
  private String description;

  @Column(nullable = false)
  private int totalDuration = 0; // em segundos

  @Column(nullable = false)
  private int totalPodcasts = 0;

  @OneToMany(mappedBy = "dailyPlaylist", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<DailyPlaylistItem> items = new ArrayList<>();

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Método executado antes de persistir para preencher timestamps.
   */
  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
    if (title == null) {
      title = "Playlist Diária - " + playlistDate;
    }
  }

  /**
   * Método executado antes de atualizar para refrescar timestamp.
   */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Getters e Setters
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

  public LocalDate getPlaylistDate() {
    return playlistDate;
  }

  public void setPlaylistDate(LocalDate playlistDate) {
    this.playlistDate = playlistDate;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getTotalDuration() {
    return totalDuration;
  }

  public void setTotalDuration(int totalDuration) {
    this.totalDuration = totalDuration;
  }

  public int getTotalPodcasts() {
    return totalPodcasts;
  }

  public void setTotalPodcasts(int totalPodcasts) {
    this.totalPodcasts = totalPodcasts;
  }

  public List<DailyPlaylistItem> getItems() {
    return items;
  }

  public void setItems(List<DailyPlaylistItem> items) {
    this.items = items;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
