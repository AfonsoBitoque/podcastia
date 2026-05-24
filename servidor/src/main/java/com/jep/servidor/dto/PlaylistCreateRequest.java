package com.jep.servidor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de pedido de criação de uma nova playlist.
 *
 * <p>Usado pelo endpoint {@code POST /api/playlists} em
 * {@link com.jep.servidor.controller.PlaylistController}.
 * O campo {@code initialPodcastId} permite criar a playlist e adicionar
 * imediatamente o primeiro podcast numa única operação.
 */
public class PlaylistCreateRequest {

  @NotBlank(message = "O título da playlist é obrigatório")
  @Size(max = 120, message = "O título deve ter no máximo 120 caracteres")
  private String title;

  @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
  private String description;

  private String coverImagePath;

  private Boolean isPublic;

  private Long initialPodcastId;

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

  public String getCoverImagePath() {
    return coverImagePath;
  }

  public void setCoverImagePath(String coverImagePath) {
    this.coverImagePath = coverImagePath;
  }

  public Boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(Boolean isPublic) {
    this.isPublic = isPublic;
  }

  public Long getInitialPodcastId() {
    return initialPodcastId;
  }

  public void setInitialPodcastId(Long initialPodcastId) {
    this.initialPodcastId = initialPodcastId;
  }
}
