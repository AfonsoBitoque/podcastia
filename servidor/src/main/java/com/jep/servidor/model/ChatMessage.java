package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade JPA de uma mensagem de chat privado entre dois utilizadores.
 *
 * <p>O ciclo de vida de uma mensagem segue o enum {@link MessageStatus}:
 * {@code SENT} → {@code DELIVERED} → {@code READ}, atualizado
 * via STOMP acks em {@link com.jep.servidor.controller.ChatWebSocketController}.
 *
 * <p>O campo {@code metadata} é um objeto embedido ({@link ChatMessageMetadata})
 * que permite anexar referências a podcasts ou episódios.
 *
 * <p><b>Tabela:</b> {@code chat_messages} com índices em {@code sender_id},
 * {@code recipient_id}, {@code created_at} e {@code status} para
 * eficiência nas queries de paginação e contagem de não lidos.
 *
 * @see ChatMessageMetadata
 * @see ChatMessageReaction
 * @see com.jep.servidor.repository.ChatMessageRepository
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(columnList = "sender_id"),
    @Index(columnList = "recipient_id"),
  @Index(columnList = "created_at"),
    @Index(columnList = "status")
})
public class ChatMessage {

  /**
   * Ciclo de vida de uma mensagem de chat.
   * <ul>
   *   <li>{@code SENT} — mensagem enviada pelo remetente, ainda não entregue.</li>
   *   <li>{@code DELIVERED} — mensagem entregue ao destinatário (WebSocket ativo).</li>
   *   <li>{@code READ} — mensagem lida pelo destinatário.</li>
   * </ul>
   */
  public enum MessageStatus {
    SENT, DELIVERED, READ
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne(optional = false)
  @JoinColumn(name = "recipient_id", nullable = false)
  private User recipient;

  @Column(nullable = false, length = 2000)
  private String content;

  @Embedded
  private ChatMessageMetadata metadata;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ChatMessageReaction> reactions = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MessageStatus status = MessageStatus.SENT;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant deliveredAt;

  @Column
  private Instant readAt;

  /**
   * Callback JPA executado antes de persistir a entidade.
   * Preenche {@code createdAt} e garante o estado inicial {@code SENT}.
   */
  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (status == null) {
      status = MessageStatus.SENT;
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getSender() {
    return sender;
  }

  public void setSender(User sender) {
    this.sender = sender;
  }

  public User getRecipient() {
    return recipient;
  }

  public void setRecipient(User recipient) {
    this.recipient = recipient;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public ChatMessageMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ChatMessageMetadata metadata) {
    this.metadata = metadata;
  }

  public List<ChatMessageReaction> getReactions() {
    return reactions;
  }

  public void setReactions(List<ChatMessageReaction> reactions) {
    this.reactions = reactions;
  }

  public MessageStatus getStatus() {
    return status;
  }

  public void setStatus(MessageStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(Instant deliveredAt) {
    this.deliveredAt = deliveredAt;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public void setReadAt(Instant readAt) {
    this.readAt = readAt;
  }
}