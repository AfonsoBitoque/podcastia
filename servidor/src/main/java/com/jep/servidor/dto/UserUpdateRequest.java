package com.jep.servidor.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO de pedido de atualização parcial dos metadados do utilizador.
 *
 * <p>Usado pelo endpoint {@code PATCH /users/{id}} em
 * {@link com.jep.servidor.controller.UserController}.
 * Todos os campos são opcionais — apenas os não-nulos são aplicados.
 * A {@code tag} nunca é atualizável por este endpoint.
 */
public class UserUpdateRequest {
  /** Novo username (mín. 3 caracteres); {@code null} se não pretende alterar. */
  @Size(min = 3, message = "O nome de utilizador não pode ser vazio")
  private String username;

  /** Nova biografia (máx. 160 caracteres); {@code null} se não pretende alterar. */
  @Size(max = 160, message = "A biografia não pode exceder as 160 letras")
  private String bio;

  /** Velocidade de reprodução preferida (ex: {@code 1.0f}, {@code 1.5f}); {@code null} se não altera. */
  private Float playbackSpeed;

  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }

  public String getBio() {
    return bio;
  }
  public void setBio(String bio) {
    this.bio = bio;
  }

  public Float getPlaybackSpeed() {
    return playbackSpeed;
  }
  public void setPlaybackSpeed(Float playbackSpeed) {
    this.playbackSpeed = playbackSpeed;
  }
}
