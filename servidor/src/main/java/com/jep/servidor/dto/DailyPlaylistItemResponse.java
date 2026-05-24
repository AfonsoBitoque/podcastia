package com.jep.servidor.dto;

import com.jep.servidor.model.DailyPlaylistItem;

/**
 * DTO de resposta para um item individual de uma playlist diária.
 *
 * <p>Representa um podcast incluído numa {@link com.jep.servidor.model.DailyPlaylist},
 * com dados resumidos do podcast e a sua posição e pontuação de relevância
 * calculada pelo {@link com.jep.servidor.service.DailyPlaylistService}.
 *
 * @see DailyPlaylistResponse
 * @see com.jep.servidor.model.DailyPlaylistItem
 */
public class DailyPlaylistItemResponse {

  private Long id;
  private Long podcastId;
  private String podcastTitle;
  private int position;
  private int podcastDuration; // em segundos
  private float relevanceScore;

  /** Construtor padrão para deserialização. */
  public DailyPlaylistItemResponse() {
  }

  /**
   * Construtor completo.
   *
   * @param id             ID do item da playlist.
   * @param podcastId      ID do podcast.
   * @param podcastTitle   Título do podcast.
   * @param position       Posição na playlist (1-indexed).
   * @param podcastDuration Duração do podcast em segundos.
   * @param relevanceScore Pontuação de relevância calculada (0.0–1.0).
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
   * Método factory que cria um DTO a partir da entidade {@link DailyPlaylistItem}.
   *
   * @param item entidade JPA do item de playlist diária.
   * @return DTO preenchido com dados do item e do podcast associado.
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
