package com.jep.servidor.repository;

import com.jep.servidor.model.ChatMessage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório Spring Data JPA para mensagens de chat privado.
 *
 * <p>Fornece paginação baseada em cursor para o histórico de conversa
 * e contagem de mensagens não lidas para o badge de notificação.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  /**
   * Devolve uma página do histórico de conversa entre dois utilizadores,
   * usando paginação baseada em cursor para consistência em streams ativos.
   *
   * <p>A combinação ({@code cursorCreatedAt}, {@code cursorId}) identifica
   * o último item já carregado. Mensagens com {@code createdAt} anterior
   * (ou igual com {@code id} inferior) ao cursor são devolvidas.
   * Passar {@code null} em ambos os cursores devolve as mensagens mais recentes.
   *
   * @param userId        ID do utilizador autenticado.
   * @param friendId      ID do outro participante da conversa.
   * @param cursorCreatedAt {@code Instant} do cursor (ou {@code null} para primeira página).
   * @param cursorId      ID do cursor (ou {@code null} para primeira página).
   * @param pageable      configuração de tamanho de página.
   * @return lista de mensagens ordenadas de mais recente para mais antiga.
   */
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

  /**
   * Conta todas as mensagens recebidas pelo utilizador que ainda não foram lidas.
   * Usado para o badge de mensagens não lidas no frontend.
   *
   * @param userId ID do utilizador destinatário.
   * @return número de mensagens com status diferente de {@code READ}.
   */
  @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.recipient.id = :userId AND m.status != 'READ'")
  long countUnreadMessages(@Param("userId") Long userId);
}