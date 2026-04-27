package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Reação de um utilizador a uma mensagem.
 */
@Entity
@Table(name = "chat_message_reactions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id", "user_id"})
    },
    indexes = {
        @Index(columnList = "message_id"),
        @Index(columnList = "user_id"),
        @Index(columnList = "emoji")
    }
)
public class ChatMessageReaction {

  public enum ReactionEmoji {
    THUMBS_UP("👍"), HEART("❤️"), LAUGH("😂"), SURPRISED("😮"), SAD("😢"), FIRE("🔥");

    private final String value;

    ReactionEmoji(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static boolean isAllowed(String emoji) {
      for (ReactionEmoji reactionEmoji : values()) {
        if (reactionEmoji.value.equals(emoji)) {
          return true;
        }
      }
      return false;
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "message_id", nullable = false)
  private ChatMessage message;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 16)
  private String emoji;

  @Column(nullable = false)
  private Instant clientEventAt;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
    if (clientEventAt == null) {
      clientEventAt = now;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ChatMessage getMessage() {
    return message;
  }

  public void setMessage(ChatMessage message) {
    this.message = message;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }

  public Instant getClientEventAt() {
    return clientEventAt;
  }

  public void setClientEventAt(Instant clientEventAt) {
    this.clientEventAt = clientEventAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}