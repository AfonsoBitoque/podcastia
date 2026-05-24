package com.jep.servidor.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Entidade JPA central que representa um utilizador da plataforma Podcastia.
 *
 * <p>A identidade única de um utilizador é composta por ({@code username}, {@code tag}),
 * sendo a {@code tag} um sufixo de 4 dígitos gerado automaticamente pelo
 * {@link com.jep.servidor.controller.RegistrationApiController}.
 *
 * <p>Os pontos de afinidade por categoria ({@code pontosDesporto}, etc.) são
 * incrementados pelo {@link com.jep.servidor.service.RecommendationService}
 * cada vez que o utilizador ouve um podcast de uma dada categoria, e
 * usam-se para personalizar o feed e a playlist diária.
 *
 * <p>A password é armazenada com BCrypt e validada pelo filtro JWT
 * ({@link com.jep.servidor.config.JwtAuthenticationFilter}).
 *
 * <p><b>Tabela:</b> {@code users}
 *
 * @see com.jep.servidor.repository.UserRepository
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = {"username", "tag"})
})
public class User {

  /**
   * Tipo de conta do utilizador.
   * <ul>
   *   <li>{@code USERNORMAL} — utilizador regular sem privilégios administrativos.</li>
   *   <li>{@code USERADMIN} — administrador com acesso ao painel {@code /api/admin}.
   *       Verificado via {@code hasRole('USER_ADMIN')} no Spring Security.</li>
   * </ul>
   */
  public enum UserType {
    USERNORMAL, USERADMIN
  }

  /**
   * Estado da conta do utilizador.
   * <ul>
   *   <li>{@code ACTIVE} — conta ativa, acesso normal.</li>
   *   <li>{@code SUSPENDED} — conta suspensa temporariamente por administrador.</li>
   *   <li>{@code BANNED} — conta banida permanentemente.</li>
   * </ul>
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
  @Size(min = 8, message = "A password deve ter pelo menos 8 caracteres")
  @Pattern(
      regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
      message = "A password deve ter pelo menos 8 caracteres, uma letra maiuscula e um numero"
  )
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

  @Column(nullable = false)
  private int pontosDesporto = 0;

  @Column(nullable = false)
  private int pontosPolitica = 0;

  @Column(nullable = false)
  private int pontosFinancas = 0;

  @Column(nullable = false)
  private int pontosGeral = 0;

  @Column(nullable = false)
  private float playbackSpeed = 1.0f;

  @Column(nullable = false)
  private boolean hasCompletedOnboarding = false;

  private String profilePicturePath;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_topics", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "topic")
  @Enumerated(EnumType.STRING)
  private List<PodcastTag> topics = new ArrayList<>();

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

  public List<PodcastTag> getTopics() {
    return topics;
  }

  public void setTopics(List<PodcastTag> topics) {
    this.topics = topics;
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

  public int getPontosDesporto() {
    return pontosDesporto;
  }

  public void setPontosDesporto(int pontosDesporto) {
    this.pontosDesporto = pontosDesporto;
  }

  public int getPontosPolitica() {
    return pontosPolitica;
  }

  public void setPontosPolitica(int pontosPolitica) {
    this.pontosPolitica = pontosPolitica;
  }

  public int getPontosFinancas() {
    return pontosFinancas;
  }

  public void setPontosFinancas(int pontosFinancas) {
    this.pontosFinancas = pontosFinancas;
  }

  public int getPontosGeral() {
    return pontosGeral;
  }

  public void setPontosGeral(int pontosGeral) {
    this.pontosGeral = pontosGeral;
  }

  public float getPlaybackSpeed() {
    return playbackSpeed;
  }

  public void setPlaybackSpeed(float playbackSpeed) {
    this.playbackSpeed = playbackSpeed;
  }

  public boolean isHasCompletedOnboarding() {
    return hasCompletedOnboarding;
  }

  public void setHasCompletedOnboarding(boolean hasCompletedOnboarding) {
    this.hasCompletedOnboarding = hasCompletedOnboarding;
  }
}
