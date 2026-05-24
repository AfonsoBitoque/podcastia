package com.jep.servidor.controller;

import com.jep.servidor.dto.ChatMessageHistoryResponse;
import com.jep.servidor.dto.ChatReactionRequest;
import com.jep.servidor.dto.ChatReactionUpdateResponse;
import com.jep.servidor.exceptions.ChatMessageException;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.ChatMessageService;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para operações de chat via HTTP — histórico de mensagens, contagem
 * de não lidas, reações e eliminação de mensagens.
 *
 * <p>Este controller complementa o {@link ChatWebSocketController}, que lida com
 * operações em tempo real via STOMP. As operações aqui expostas são adequadas para
 * pedidos REST clássicos (paginação de histórico, contadores, etc.).
 *
 * <p><b>Base path:</b> {@code /api/chats}
 *
 * <p><b>Autenticação:</b> Todos os endpoints requerem JWT válido. O utilizador autenticado
 * é resolvido internamente via {@link #getAuthenticatedUser()}, que lê o email do
 * {@link SecurityContextHolder} e localiza o {@link User} correspondente.
 *
 * <p><b>Tratamento de erros:</b> Exceções do tipo {@link ChatMessageException} são
 * capturadas e retornadas com o código HTTP definido em {@link ChatMessageException#getStatus()},
 * evitando que o Spring retorne um 500 genérico.
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /{friendId}/messages} — histórico paginado com cursor.</li>
 *   <li>{@code GET /unread-count} — total de mensagens não lidas do utilizador.</li>
 *   <li>{@code POST /messages/{messageId}/reactions} — adicionar/alterar reação a mensagem.</li>
 *   <li>{@code DELETE /messages/{messageId}} — eliminar mensagem.</li>
 * </ul>
 *
 * @see ChatWebSocketController
 * @see ChatMessageService
 * @see com.jep.servidor.service.impl.ChatMessageServiceImpl
 */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

  private final ChatMessageService chatMessageService;
  private final UserRepository userRepository;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param chatMessageService serviço de mensagens de chat com a lógica de negócio.
   * @param userRepository     repositório para resolver o utilizador autenticado pelo email.
   */
  public ChatController(ChatMessageService chatMessageService, UserRepository userRepository) {
    this.chatMessageService = chatMessageService;
    this.userRepository = userRepository;
  }

  /**
   * Retorna o histórico paginado de mensagens de uma conversa entre dois utilizadores.
   *
   * <p>Utiliza paginação baseada em cursor para navegar eficientemente em conversas longas.
   * O {@code cursor} é tipicamente o ID ou timestamp da última mensagem recebida.
   *
   * @param friendId ID do utilizador com quem a conversa é partilhada.
   * @param cursor   cursor opcional para paginação (omitir para obter as mensagens mais recentes).
   * @param limit    número máximo de mensagens a retornar (por omissão: 30).
   * @return {@code 200 OK} com {@link ChatMessageHistoryResponse};
   *         {@code 401 Unauthorized} se não autenticado;
   *         código do {@link ChatMessageException} se ocorrer erro de negócio.
   */
  @GetMapping("/{friendId}/messages")
  public ResponseEntity<?> getMessages(@PathVariable Long friendId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "30") int limit) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      ChatMessageHistoryResponse response = chatMessageService.getConversation(
          authUser.get().getId(),
          friendId,
          cursor,
          limit);
      return ResponseEntity.ok(response);
    } catch (ChatMessageException exception) {
      return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
    }
  }

  /**
   * Retorna a contagem total de mensagens não lidas para o utilizador autenticado,
   * considerando todas as suas conversas ativas.
   *
   * @return {@code 200 OK} com {@code {"count": N}};
   *         {@code 401 Unauthorized} se não autenticado.
   */
  @GetMapping("/unread-count")
  public ResponseEntity<?> getUnreadCount() {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      long count = chatMessageService.getUnreadCount(authUser.get().getId());
      return ResponseEntity.ok(Map.of("count", count));
    } catch (ChatMessageException exception) {
      return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
    }
  }

  /**
   * Adiciona ou atualiza a reação emoji de um utilizador a uma mensagem específica.
   *
   * <p>Se o utilizador já tiver reagido com o mesmo emoji, a reação é removida (toggle).
   * A resposta inclui o estado atualizado das reações da mensagem.
   *
   * @param messageId ID da mensagem a reagir.
   * @param request   corpo JSON com o emoji e timestamp do evento do cliente
   *                  ({@link ChatReactionRequest}).
   * @return {@code 200 OK} com {@link ChatReactionUpdateResponse} atualizado;
   *         {@code 401 Unauthorized} se não autenticado;
   *         código do {@link ChatMessageException} se ocorrer erro de negócio.
   */
  @PostMapping("/messages/{messageId}/reactions")
  public ResponseEntity<?> reactToMessage(@PathVariable Long messageId,
      @RequestBody ChatReactionRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      ChatReactionUpdateResponse response = chatMessageService.reactToMessage(
          authUser.get().getId(), messageId, request);
      return ResponseEntity.ok(response);
    } catch (ChatMessageException exception) {
      return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
    }
  }

  /**
   * Elimina uma mensagem de chat, desde que o utilizador autenticado seja o remetente.
   *
   * @param messageId ID da mensagem a eliminar.
   * @return {@code 204 No Content} se eliminada com sucesso;
   *         {@code 401 Unauthorized} se não autenticado;
   *         código do {@link ChatMessageException} se o utilizador não for o remetente
   *         ou a mensagem não existir.
   */
  @DeleteMapping("/messages/{messageId}")
  public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      chatMessageService.deleteMessage(authUser.get().getId(), messageId);
      return ResponseEntity.noContent().build();
    } catch (ChatMessageException exception) {
      return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
    }
  }

  /**
   * Método utilitário que resolve o {@link User} autenticado a partir do contexto de segurança.
   *
   * <p>Lê o email do nome do principal no {@link SecurityContextHolder} e pesquisa
   * o utilizador correspondente na base de dados.
   *
   * @return {@link Optional} com o utilizador autenticado, ou vazio se não autenticado
   *         ou se o utilizador não existir na BD.
   */
  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }
}