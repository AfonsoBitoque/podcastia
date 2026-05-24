package com.jep.servidor.dto;

import com.jep.servidor.model.DailyPlaylistItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de resposta completo de uma playlist diária gerada automaticamente.
 *
 * <p>Corresponde à visão serializada de uma {@link com.jep.servidor.model.DailyPlaylist},
 * incluindo metadados (data, título, descrição, duração total) e a lista de
 * {@link DailyPlaylistItemResponse} convertidos automaticamente no construtor
 * via {@link DailyPlaylistItemResponse#fromEntity}.
 *
 * @see com.jep.servidor.controller.DailyPlaylistController
 * @see com.jep.servidor.service.DailyPlaylistService
 */
public class DailyPlaylistResponse {

  private Long id;
  private LocalDate playlistDate;
  private String title;
  private String description;
  private int totalDuration; // em segundos
  private int totalPodcasts;
  private List<DailyPlaylistItemResponse> items;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** Construtor padrão para deserialização. */
  public DailyPlaylistResponse() {
  }

  /**
   * Construtor que aceita a lista de entidades {@link DailyPlaylistItem} e as converte
   * automaticamente para {@link DailyPlaylistItemResponse}.
   *
   * @param id            ID da playlist diária.
   * @param playlistDate  Data a que esta playlist corresponde.
   * @param title         Título gerado para a playlist.
   * @param description   Descrição da playlist.
   * @param totalDuration Duração total em segundos.
   * @param totalPodcasts Número de podcasts na lista.
   * @param items         Lista de entidades {@link DailyPlaylistItem} a converter.
   * @param createdAt     Data de criação.
   * @param updatedAt     Data da última atualização.
   */
  public DailyPlaylistResponse(Long id, LocalDate playlistDate, String title, String description,
      int totalDuration, int totalPodcasts, List<DailyPlaylistItem> items,
      LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.playlistDate = playlistDate;
    this.title = title;
    this.description = description;
    this.totalDuration = totalDuration;
    this.totalPodcasts = totalPodcasts;
    this.items = items.stream()
        .map(DailyPlaylistItemResponse::fromEntity)
        .collect(Collectors.toList());
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  // Getters e Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public List<DailyPlaylistItemResponse> getItems() {
    return items;
  }

  public void setItems(List<DailyPlaylistItemResponse> items) {
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
