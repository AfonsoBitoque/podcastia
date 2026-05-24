package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entidade JPA de relação direcional entre dois utilizadores.
 *
 * <p><b>Modelo direcional:</b> {@code user} (campo {@code user_id}) é sempre o
 * remetente/iniciador, e {@code friend} (campo {@code friend_id}) é o destinatário.
 * Uma amizade aceite cria <em>dois registos</em> (A→B e B→A).
 *
 * <p><b>Ciclo de vida de um pedido:</b>
 * <ol>
 *   <li>Utilizador A envia pedido: registo {@code A→B, PEDIDO}.</li>
 *   <li>B aceita: registo {@code A→B} passa a {@code AMIGO} e é criado {@code B→A, AMIGO}.</li>
 *   <li>B rejeita: registo {@code A→B} passa a {@code PEDIDO_REJEITADO}.</li>
 *   <li>A cancela: registo {@code A→B} passa a {@code CANCELADO}.</li>
 * </ol>
 *
 * <p>O cooldown de 7 dias após rejeção/cancelamento é forçado pelo
 * {@link com.jep.servidor.service.impl.UserRelationshipServiceImpl}.
 *
 * <p><b>Tabela:</b> {@code user_relations}
 *
 * @see com.jep.servidor.repository.UserRelationRepository
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "user_relations",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "friend_id"})},
    indexes = {
      @Index(columnList = "user_id"),
      @Index(columnList = "friend_id"),
      @Index(columnList = "type")
    })
public class UserRelation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user; // Remetente no caso de um PEDIDO

  @ManyToOne(optional = false)
  @JoinColumn(name = "friend_id", nullable = false)
  private User friend; // Destinatário no caso de um PEDIDO

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RelationType type;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Estado da relação entre dois utilizadores.
   * <ul>
   *   <li>{@code PEDIDO} — pedido enviado, aguarda aceitação.</li>
   *   <li>{@code AMIGO} — amizade aceite (existe registo inverso).</li>
   *   <li>{@code PEDIDO_REJEITADO} — pedido rejeitado pelo destinatário.</li>
   *   <li>{@code CANCELADO} — pedido cancelado pelo remetente.</li>
   *   <li>{@code BLOQUEADO} — utilizador bloqueado.</li>
   * </ul>
   */
  public enum RelationType {
    AMIGO,
    BLOQUEADO,
    PEDIDO,
    PEDIDO_REJEITADO,
    CANCELADO
  }

  /** Construtor padrão. */
  public UserRelation() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Construtor com parâmetros.
   *
   * @param user Utilizador principal (remetente).
   * @param friend Utilizador relacionado (destinatário).
   * @param type Tipo de relação.
   */
  public UserRelation(User user, User friend, RelationType type) {
    this.user = user;
    this.friend = friend;
    this.type = type;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PrePersist
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getSender() {
    return user;
  }

  public void setSender(User user) {
    this.user = user;
  }

  public User getReceiver() {
    return friend;
  }

  public void setReceiver(User friend) {
    this.friend = friend;
  }

  public RelationType getType() {
    return type;
  }

  public void setType(RelationType type) {
    this.type = type;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
