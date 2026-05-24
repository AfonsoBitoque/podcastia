package com.jep.servidor.service;

import com.jep.servidor.dto.FriendDto;
import com.jep.servidor.dto.PendingRequestDto;
import com.jep.servidor.dto.RelationStatusDto;
import java.util.List;

/**
 * Interface do serviço de gestão de relações entre utilizadores.
 *
 * <p>Suporta o ciclo de vida completo das relações:
 * pedido de amizade → aceitar/rejeitar/cancelar → amizade → remover;
 * e de forma independente o bloqueio de utilizadores.
 *
 * <p>A implementação é {@link com.jep.servidor.service.impl.UserRelationshipServiceImpl},
 * que aplica um cooldown de 7 dias após rejeição antes de permitir novo pedido.
 *
 * @see com.jep.servidor.controller.UserRelationController
 */
public interface UserRelationshipService {

  /**
   * Envia um pedido de amizade de {@code senderId} para {@code receiverId}.
   *
   * @param senderId   ID do remetente.
   * @param receiverId ID do destinatário.
   * @throws com.jep.servidor.exceptions.BusinessException se já existir uma relação ativa,
   *         o destinatário bloqueou o remetente, ou o cooldown de 7 dias ainda estiver ativo.
   */
  void sendFriendRequest(Long senderId, Long receiverId);

  /**
   * Aceita um pedido de amizade pendente.
   * Cria dois registos {@code AMIGO} (bidirecional).
   *
   * @param senderId   ID do utilizador que enviou o pedido original.
   * @param receiverId ID do utilizador que aceita.
   * @throws com.jep.servidor.exceptions.BusinessException se não existir pedido pendente.
   */
  void acceptFriendRequest(Long senderId, Long receiverId);

  /**
   * Rejeita um pedido de amizade pendente.
   * Define o estado como {@code PEDIDO_REJEITADO} e inicia o cooldown de 7 dias.
   *
   * @param senderId   ID do utilizador que enviou o pedido.
   * @param receiverId ID do utilizador que rejeita.
   * @throws com.jep.servidor.exceptions.BusinessException se não existir pedido pendente.
   */
  void rejectFriendRequest(Long senderId, Long receiverId);

  /**
   * Bloqueia um utilizador. Cria ou atualiza o registo de relação para {@code BLOQUEADO}.
   *
   * @param blockerId ID do utilizador que bloqueia.
   * @param blockedId ID do utilizador a ser bloqueado.
   */
  void blockUser(Long blockerId, Long blockedId);

  /**
   * Cancela um pedido de amizade pendente enviado pelo próprio.
   *
   * @param senderId   ID do utilizador que enviou o pedido.
   * @param receiverId ID do destinatário.
   * @throws com.jep.servidor.exceptions.BusinessException se não existir pedido pendente.
   */
  void cancelFriendRequest(Long senderId, Long receiverId);

  /**
   * Devolve o estado atual da relação entre dois utilizadores.
   *
   * @param userId       ID do utilizador autenticado.
   * @param targetUserId ID do utilizador alvo.
   * @return DTO com o estado ({@code FRIENDS}, {@code PENDING_SENT}, {@code BLOCKED_BY_YOU}, etc.)
   *         e se é possível enviar um pedido ({@code canRequest}).
   */
  RelationStatusDto getRelationStatus(Long userId, Long targetUserId);

  /**
   * Devolve os pedidos de amizade pendentes recebidos pelo utilizador.
   *
   * @param userId ID do utilizador destinatário dos pedidos.
   * @return lista de DTOs com informação do remetente.
   */
  List<PendingRequestDto> getPendingFriendRequests(Long userId);

  /**
   * Remove uma amizade bidirecional (elimina os dois registos {@code AMIGO}).
   *
   * @param userId   ID do utilizador que remove.
   * @param friendId ID do amigo a remover.
   * @throws com.jep.servidor.exceptions.FriendshipNotFoundException se a amizade não existir.
   */
  void removeFriendship(Long userId, Long friendId);

  /**
   * Devolve a lista de amigos aceites do utilizador.
   *
   * @param userId ID do utilizador.
   * @return lista de DTOs com ID, username e foto de perfil de cada amigo.
   */
  List<FriendDto> getFriends(Long userId);
}
