package com.jep.servidor.controller;

import com.jep.servidor.config.JwtUtil;
import com.jep.servidor.dto.FriendDto;
import com.jep.servidor.dto.PendingRequestDto;
import com.jep.servidor.dto.RelationStatusDto;
import com.jep.servidor.service.UserRelationshipService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gestão de relações sociais entre utilizadores — amizades e respetivas operações.
 *
 * <p>A autenticação é feita por extracção direta do claim {@code id} do token JWT no
 * cabeçalho {@code Authorization}, em vez de usar o {@code SecurityContextHolder}.
 * Isto permite obter o ID do utilizador sem uma query adicional ao repositório.
 *
 * <p><b>Modelo de relação:</b> As relações são direcionais — {@code UserRelation.user} é
 * o remetente e {@code UserRelation.friend} é o destinatário. Uma amizade aceite cria
 * dois registos (um em cada direção). Existe um cooldown de 7 dias após remoção
 * antes de poder enviar novo pedido.
 *
 * <p><b>Base path:</b> {@code /api/relations} (requer autenticação JWT)
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code POST /friend-request/{friendId}} — enviar pedido de amizade.</li>
 *   <li>{@code POST /friend-request/{friendId}/accept} — aceitar pedido.</li>
 *   <li>{@code POST /friend-request/{friendId}/reject} — rejeitar pedido.</li>
 *   <li>{@code DELETE /friend-request/{friendId}/cancel} — cancelar pedido enviado.</li>
 *   <li>{@code DELETE /friend-request/{friendId}} — remover amizade existente.</li>
 *   <li>{@code GET /friend-requests/pending} — listar pedidos pendentes recebidos.</li>
 *   <li>{@code GET /status/{targetUserId}} — obter estado da relação com outro utilizador.</li>
 *   <li>{@code GET /friends} — listar amigos.</li>
 * </ul>
 *
 * @see UserRelationshipService
 * @see com.jep.servidor.dto.FriendDto
 * @see com.jep.servidor.dto.PendingRequestDto
 * @see com.jep.servidor.dto.RelationStatusDto
 */
@RestController
@RequestMapping("/api/relations")
public class UserRelationController {

  private final UserRelationshipService userRelationshipService;
  private final JwtUtil jwtUtil;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param userRelationshipService serviço com a lógica de negócio das relações.
   * @param jwtUtil                 utilitário JWT para extrair o ID do utilizador do token.
   */
  public UserRelationController(UserRelationshipService userRelationshipService, JwtUtil jwtUtil) {
    this.userRelationshipService = userRelationshipService;
    this.jwtUtil = jwtUtil;
  }

  /**
   * Extrai o ID do utilizador autenticado a partir do token JWT no cabeçalho
   * {@code Authorization: Bearer <token>}.
   *
   * @param authHeader valor do cabeçalho {@code Authorization}.
   * @return ID do utilizador extraído do claim {@code id} do JWT.
   * @throws RuntimeException se o cabeçalho for nulo, vazio ou não iniciar com {@code "Bearer "}.
   */
  private Long getUserIdFromToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Token não fornecido ou malformado.");
    }
    String jwt = authHeader.substring(7);
    return jwtUtil.extractClaim(jwt, claims -> claims.get("id", Long.class));
  }

  /**
   * Envia um pedido de amizade para outro utilizador.
   *
   * @param authHeader cabeçalho JWT do utilizador autenticado (remetente).
   * @param friendId   ID do utilizador destinatário do pedido.
   * @return {@code 200 OK} se enviado com sucesso.
   */
  @PostMapping("/friend-request/{friendId}")
  public ResponseEntity<Void> sendFriendRequest(
      @RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
    Long userId = getUserIdFromToken(authHeader);
    userRelationshipService.sendFriendRequest(userId, friendId);
    return ResponseEntity.ok().build();
  }

  /**
   * Aceita um pedido de amizade enviado por outro utilizador.
   *
   * <p>Cria dois registos {@link com.jep.servidor.model.UserRelation} (bidirecional)
   * marcados com estado {@code ACCEPTED}.
   *
   * @param authHeader cabeçalho JWT do utilizador autenticado (destinatário do pedido).
   * @param friendId   ID do utilizador que enviou o pedido.
   * @return {@code 200 OK} se aceite com sucesso.
   */
  @PostMapping("/friend-request/{friendId}/accept")
  public ResponseEntity<Void> acceptFriendRequest(
      @RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
    Long userId = getUserIdFromToken(authHeader);
    userRelationshipService.acceptFriendRequest(friendId, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Rejeita um pedido de amizade recebido.
   *
   * @param authHeader cabeçalho JWT do utilizador autenticado (destinatário do pedido).
   * @param friendId   ID do utilizador que enviou o pedido.
   * @return {@code 200 OK} se rejeitado com sucesso.
   */
  @PostMapping("/friend-request/{friendId}/reject")
  public ResponseEntity<Void> rejectFriendRequest(
      @RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
    Long userId = getUserIdFromToken(authHeader);
    userRelationshipService.rejectFriendRequest(friendId, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Lista os pedidos de amizade pendentes recebidos pelo utilizador autenticado.
   *
   * @param authHeader cabeçalho JWT do utilizador autenticado.
   * @return {@code 200 OK} com lista de {@link com.jep.servidor.dto.PendingRequestDto}
   *         (pode ser vazia se não houver pedidos pendentes).
   */
  @GetMapping("/friend-requests/pending")
  public ResponseEntity<List<PendingRequestDto>> getPendingFriendRequests(
      @RequestHeader("Authorization") String authHeader) {
    Long userId = getUserIdFromToken(authHeader);
    List<PendingRequestDto> pendingRequests = userRelationshipService.getPendingFriendRequests(userId);
    return ResponseEntity.ok(pendingRequests);
  }

  /**
   * Retorna o estado atual da relação entre o utilizador autenticado e outro utilizador.
   *
   * <p>Os possíveis estados estão definidos em {@link com.jep.servidor.dto.RelationStatusDto}
   * (ex: {@code NONE}, {@code PENDING_SENT}, {@code PENDING_RECEIVED}, {@code FRIENDS},
   * {@code BLOCKED}).
   *
   * @param authHeader   cabeçalho JWT do utilizador autenticado.
   * @param targetUserId ID do utilizador alvo.
   * @return {@code 200 OK} com {@link com.jep.servidor.dto.RelationStatusDto}.
   */
  @GetMapping("/status/{targetUserId}")
  public ResponseEntity<RelationStatusDto> getRelationStatus(
      @RequestHeader("Authorization") String authHeader, @PathVariable Long targetUserId) {
    Long userId = getUserIdFromToken(authHeader);
    RelationStatusDto status = userRelationshipService.getRelationStatus(userId, targetUserId);
    return ResponseEntity.ok(status);
  }

  /**
   * Cancela um pedido de amizade enviado pelo utilizador autenticado (ainda não aceite).
   *
   * @param authHeader cabeçalho JWT do utilizador autenticado (remetente do pedido).
   * @param friendId   ID do utilizador destinatário.
   * @return {@code 204 No Content} se cancelado com sucesso.
   */
  @DeleteMapping("/friend-request/{friendId}/cancel")
  public ResponseEntity<Void> cancelFriendRequest(
      @RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
    Long userId = getUserIdFromToken(authHeader);
    userRelationshipService.cancelFriendRequest(userId, friendId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Remove uma amizade existente entre o utilizador autenticado e outro utilizador.
   *
   * <p>Elimina os dois registos direcionais de {@link com.jep.servidor.model.UserRelation}.
   * Após a remoção, aplica-se um cooldown de 7 dias antes de poder enviar novo pedido.
   *
   * @param authHeader cabeçalho JWT do utilizador autenticado.
   * @param friendId   ID do amigo a remover.
   * @return {@code 204 No Content} se removido com sucesso.
   */
  @DeleteMapping("/friend-request/{friendId}")
  public ResponseEntity<Void> removeFriendship(
      @RequestHeader("Authorization") String authHeader, @PathVariable Long friendId) {
    Long userId = getUserIdFromToken(authHeader);
    userRelationshipService.removeFriendship(userId, friendId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lista todos os amigos do utilizador autenticado.
   *
   * @param authHeader cabeçalho JWT do utilizador autenticado.
   * @return {@code 200 OK} com lista de {@link com.jep.servidor.dto.FriendDto}
   *         (pode ser vazia se não tiver amigos).
   */
  @GetMapping("/friends")
  public ResponseEntity<List<FriendDto>> getFriends(
      @RequestHeader("Authorization") String authHeader) {
    Long userId = getUserIdFromToken(authHeader);
    List<FriendDto> friends = userRelationshipService.getFriends(userId);
    return ResponseEntity.ok(friends);
  }
}
