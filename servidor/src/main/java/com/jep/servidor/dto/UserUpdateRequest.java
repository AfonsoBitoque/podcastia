package com.jep.servidor.dto;

import jakarta.validation.constraints.Size;

public class UserUpdateRequest {
  @Size(min = 1, message = "O nome de utilizador não pode ser vazio")
  private String username;

  @Size(max = 160, message = "A biografia não pode exceder as 160 letras")
  private String bio;

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
}
