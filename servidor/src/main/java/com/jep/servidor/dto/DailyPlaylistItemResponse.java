package com.jep.servidor.dto;

import com.jep.servidor.model.DailyPlaylistItem;

/**
 * DTO para resposta de item da playlist diária.
 */
public class DailyPlaylistItemResponse {

  private Long id;
  private Long podcastId;
  private String podcastTitle;
  private int position;
  private int podcastDuration; // em segundos
  private float relevanceScore;

  /**
   * Construtor padrão.
   */
  public DailyPlaylistItemResponse() {
  }

  /**
   * Construtor com dados.
   */
  public DailyPlaylistItemResponse(Long id, Long podcastId, String podcastTitle, int position,
      int podcastDuration, float relevanceScore) {
    this.id = id;
    this.podcastId = podcastId;
    this.podcastTitle = podcastTitle;
    this.position = position;
    this.podcastDuration = podcastDuration;
    this.relevanceScore = relevanceScore;
  }

  /**
   * Cria um DTO a partir de uma entidade DailyPlaylistItem.
   */
  public static DailyPlaylistItemResponse fromEntity(DailyPlaylistItem item) {
    return new DailyPlaylistItemResponse(
        item.getId(),
        item.getPodcast().getId(),
        item.getPodcast().getTitulo(),
        item.getPosition(),
        item.getPodcast().getDuracao(),
        item.getRelevanceScore()
    );
  }

  // Getters e Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getPodcastId() {
    return podcastId;
  }

  public void setPodcastId(Long podcastId) {
    this.podcastId = podcastId;
  }

  public String getPodcastTitle() {
    return podcastTitle;
  }

  public void setPodcastTitle(String podcastTitle) {
    this.podcastTitle = podcastTitle;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public int getPodcastDuration() {
    return podcastDuration;
  }

  public void setPodcastDuration(int podcastDuration) {
    this.podcastDuration = podcastDuration;
  }

  public float getRelevanceScore() {
    return relevanceScore;
  }

  public void setRelevanceScore(float relevanceScore) {
    this.relevanceScore = relevanceScore;
  }
}
