package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entidade que representa um utilizador no sistema.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = {"username", "tag"})
})
public class User {

  /**
   * Tipos de utilizador disponíveis.
   */
  public enum UserType {
    USERNORMAL, USERADMIN
  }

  /**
   * Estados possíveis para um utilizador.
   */
  public enum UserStatus {
    ACTIVE, SUSPENDED, BANNED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime lastActiveAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status = UserStatus.ACTIVE;

  @NotBlank(message = "O nome de utilizador é obrigatório")
  @Column(nullable = false)
  private String username;

  @NotBlank(message = "A tag é obrigatória")
  @Size(min = 4, max = 4, message = "A tag deve ter exatamente 4 caracteres")
  @Column(nullable = false)
  private String tag;

  @NotBlank(message = "A password é obrigatória")
  @Column(nullable = false)
  private String password;

  @NotBlank(message = "O email é obrigatório")
  @Email(message = "O email deve ser válido")
  @Column(nullable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserType userType = UserType.USERNORMAL;

  @Column(length = 500)
  private String bio;

  private String profilePicturePath;

  /**
   * Construtor padrão.
   */
  public User() {
  }

  /**
   * Método executado antes de persistir a entidade para definir datas de criação.
   */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    lastActiveAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UserType getUserType() {
    return userType;
  }

  public void setUserType(UserType userType) {
    this.userType = userType;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getProfilePicturePath() {
    return profilePicturePath;
  }

  public void setProfilePicturePath(String profilePicturePath) {
    this.profilePicturePath = profilePicturePath;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getLastActiveAt() {
    return lastActiveAt;
  }

  public void setLastActiveAt(LocalDateTime lastActiveAt) {
    this.lastActiveAt = lastActiveAt;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }
}
