package com.jep.servidor.repository;

import com.jep.servidor.model.ChatMessageReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data JPA para reações a mensagens de chat.
 *
 * <p>Usado pelo {@link com.jep.servidor.service.impl.ChatMessageServiceImpl}
 * para gerir o ciclo de toggle de reações e pela
 * {@link com.jep.servidor.controller.ChatController} para eliminação
 * de mensagens com cascade.
 */
public interface ChatMessageReactionRepository extends JpaRepository<ChatMessageReaction, Long> {

  /**
   * Encontra a reação de um utilizador numa mensagem específica.
   * Usado para determinar se o toggle deve adicionar ou remover.
   *
   * @param messageId ID da mensagem.
   * @param userId    ID do utilizador.
   * @return a reação existente, ou {@link java.util.Optional#empty()} se não houver.
   */
  Optional<ChatMessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

  /**
   * Devolve todas as reações de uma mensagem para construção do sumário agrupado.
   *
   * @param messageId ID da mensagem.
   * @return lista de todas as reações da mensagem.
   */
  List<ChatMessageReaction> findByMessageId(Long messageId);

  /**
   * Elimina todas as reações de uma mensagem (usado antes de eliminar a mensagem).
   *
   * @param messageId ID da mensagem cujas reações serão eliminadas.
   */
  void deleteByMessageId(Long messageId);
}