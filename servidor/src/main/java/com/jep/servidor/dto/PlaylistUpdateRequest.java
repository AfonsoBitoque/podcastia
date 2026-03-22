package com.jep.servidor.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO para atualização de playlists.
 */
public class PlaylistUpdateRequest {

  @Size(min = 1, max = 120, message = "O título deve ter entre 1 e 120 caracteres")
  private String title;

  @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
  private String description;

  private String coverImagePath;

  private Boolean isPublic;

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
}
