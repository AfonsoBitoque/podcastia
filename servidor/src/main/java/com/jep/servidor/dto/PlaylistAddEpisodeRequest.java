package com.jep.servidor.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para adicionar episódio a uma playlist.
 */
public class PlaylistAddEpisodeRequest {

  @NotNull(message = "O podcastId é obrigatório")
  private Long podcastId;

  public Long getPodcastId() {
    return podcastId;
  }

  public void setPodcastId(Long podcastId) {
    this.podcastId = podcastId;
  }
}
