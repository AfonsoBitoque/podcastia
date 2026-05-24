package com.jep.servidor.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO de pedido de reordenação dos episodios de uma playlist.
 *
 * <p>Usado pelo endpoint {@code PUT /api/playlists/{id}/reorder} em
 * {@link com.jep.servidor.controller.PlaylistController}. A nova ordem
 * é definida pela sequência dos {@code podcastIds} na lista.
 */
public class PlaylistReorderRequest {

  /** Lista ordenada dos IDs de podcast que define a nova ordem dos episódios. */
  @NotEmpty(message = "A lista de podcastIds não pode ser vazia")
  private List<Long> podcastIds;

  public List<Long> getPodcastIds() {
    return podcastIds;
  }

  public void setPodcastIds(List<Long> podcastIds) {
    this.podcastIds = podcastIds;
  }
}
