package com.jep.servidor.repository;

import com.jep.servidor.model.ChatMessage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório para persistência de mensagens privadas.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  @Query("SELECT m FROM ChatMessage m "
      + "WHERE ((m.sender.id = :userId AND m.recipient.id = :friendId) "
      + "OR (m.sender.id = :friendId AND m.recipient.id = :userId)) "
      + "AND (:cursorCreatedAt IS NULL OR m.createdAt < :cursorCreatedAt "
      + "OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId)) "
      + "ORDER BY m.createdAt DESC, m.id DESC")
  List<ChatMessage> findConversationPage(
      @Param("userId") Long userId,
      @Param("friendId") Long friendId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable);

  @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipient.id = :userId AND m.status != 'READ'")
  long countUnreadMessages(@Param("userId") Long userId);
}