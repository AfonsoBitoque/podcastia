package com.jep.servidor.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de pedido para adicionar um podcast a uma playlist existente.
 *
 * <p>Usado pelo endpoint {@code POST /api/playlists/{id}/episodes} em
 * {@link com.jep.servidor.controller.PlaylistController}.
 */
public class PlaylistAddEpisodeRequest {

  /** ID do podcast a adicionar à playlist (obrigatório). */
  @NotNull(message = "O podcastId é obrigatório")
  private Long podcastId;

  public Long getPodcastId() {
    return podcastId;
  }

  public void setPodcastId(Long podcastId) {
    this.podcastId = podcastId;
  }
}
