package com.jep.servidor.repository;

import com.jep.servidor.model.ChatMessageReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para reações às mensagens.
 */
public interface ChatMessageReactionRepository extends JpaRepository<ChatMessageReaction, Long> {

  Optional<ChatMessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

  List<ChatMessageReaction> findByMessageId(Long messageId);

  void deleteByMessageId(Long messageId);
}