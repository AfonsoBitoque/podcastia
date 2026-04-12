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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma relação entre utilizadores.
 * No contexto de um PEDIDO, 'user' é o remetente e 'friend' é o destinatário.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_relations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "friend_id"})
    },
    indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "friend_id"),
        @Index(columnList = "type")
    }
)
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
   * Tipos de relação possíveis.
   */
  public enum RelationType {
    AMIGO, BLOQUEADO, PEDIDO, PEDIDO_REJEITADO, CANCELADO
  }

  /**
   * Construtor padrão.
   */
  public UserRelation() {
  }

  /**
   * Construtor com parâmetros.
   *
   * @param user   Utilizador principal (remetente).
   * @param friend Utilizador relacionado (destinatário).
   * @param type   Tipo de relação.
   */
  public UserRelation(User user, User friend, RelationType type) {
    this.user = user;
    this.friend = friend;
    this.type = type;
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
