package com.jep.servidor.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO para reordenar episódios de uma playlist.
 */
public class PlaylistReorderRequest {

  @NotEmpty(message = "A lista de podcastIds não pode ser vazia")
  private List<Long> podcastIds;

  public List<Long> getPodcastIds() {
    return podcastIds;
  }

  public void setPodcastIds(List<Long> podcastIds) {
    this.podcastIds = podcastIds;
  }
}
