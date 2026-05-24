package com.jep.servidor.service;

import com.jep.servidor.dto.ChatMessageDto;
import com.jep.servidor.dto.ChatMessageHistoryResponse;
import com.jep.servidor.dto.ChatMessageRequest;
import com.jep.servidor.dto.ChatReactionRequest;
import com.jep.servidor.dto.ChatReactionUpdateResponse;
import java.security.Principal;

/**
 * Interface do serviço de mensagens privadas de chat.
 *
 * <p>Define o contrato para envio, reconhecimento, histórico, reações
 * e eliminação de mensagens, bem como a gestão de presença WebSocket.
 *
 * <p>A implementação é {@link com.jep.servidor.service.impl.ChatMessageServiceImpl},
 * que adiciona rate-limiting, blacklist de links, push queue e integração STOMP.
 */
public interface ChatMessageService {

  /**
   * Envia uma mensagem de chat de um remetente para um destinatário.
   *
   * @param senderId ID do utilizador remetente.
   * @param request  payload com {@code recipientId}, {@code content} e {@code metadata} opcional.
   * @return DTO da mensagem persistida.
   * @throws com.jep.servidor.exceptions.ChatMessageException se falhar validações de permissão,
   *         rate-limit, conteúdo ou links bloqueados.
   */
  ChatMessageDto sendMessage(Long senderId, ChatMessageRequest request);

  /**
   * Confirma a entrega ou leitura de uma mensagem ({@code DELIVERED} ou {@code READ}).
   *
   * <p>Atualiza o status e timestamps da mensagem e notifica o remetente
   * via STOMP ({@code /queue/messages}).
   *
   * @param userId              ID do utilizador destinatário.
   * @param messageId           ID da mensagem a confirmar.
   * @param acknowledgementType {@code "DELIVERED"} ou {@code "READ"}.
   * @return DTO atualizado da mensagem.
   */
  ChatMessageDto acknowledgeMessage(Long userId, Long messageId, String acknowledgementType);

  /**
   * Devolve uma página do histórico de conversa com paginação baseada em cursor.
   *
   * @param userId   ID do utilizador autenticado.
   * @param friendId ID do outro participante.
   * @param cursor   cursor opaco da última página (ou {@code null} para a primeira).
   * @param limit    número máximo de mensagens por página.
   * @return resposta com mensagens em ordem cronológica, cursor seguinte e flag {@code hasMore}.
   */
  ChatMessageHistoryResponse getConversation(Long userId, Long friendId, String cursor,
      int limit);

  /**
   * Adiciona, altera ou remove uma reação emoji de um utilizador a uma mensagem.
   *
   * @param userId    ID do utilizador que reage.
   * @param messageId ID da mensagem.
   * @param request   payload com o emoji e {@code clientEventAt} para deduplication.
   * @return resposta com a ação ({@code ADDED}, {@code REMOVED}, {@code UPDATED}, {@code IGNORED_STALE})
   *         e o sumário de reações atualizado.
   */
  ChatReactionUpdateResponse reactToMessage(Long userId, Long messageId, ChatReactionRequest request);

  /**
   * Elimina uma mensagem. Apenas o remetente pode eliminar a própria mensagem.
   *
   * @param userId    ID do utilizador remetente.
   * @param messageId ID da mensagem a eliminar.
   * @throws com.jep.servidor.exceptions.ChatMessageException {@code 403} se não for o remetente.
   */
  void deleteMessage(Long userId, Long messageId);

  /**
   * Conta as mensagens não lidas do utilizador.
   *
   * @param userId ID do utilizador destinatário.
   * @return número de mensagens com status diferente de {@code READ}.
   */
  long getUnreadCount(Long userId);

  /**
   * Verifica se um utilizador tem sessões WebSocket ativas.
   *
   * @param userId ID do utilizador.
   * @return {@code true} se tiver pelo menos uma sessão STOMP conectada.
   */
  boolean isOnline(Long userId);

  /**
   * Regista uma nova ligação WebSocket para um utilizador.
   *
   * @param principal principal do utilizador autenticado.
   * @param sessionId ID da sessão STOMP.
   */
  void handleConnect(Principal principal, String sessionId);

  /**
   * Regista a desconexão de uma sessão WebSocket.
   *
   * @param sessionId ID da sessão STOMP encerrada.
   */
  void handleDisconnect(String sessionId);
}