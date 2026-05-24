package com.jep.servidor.repository;

import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório Spring Data JPA para relações entre utilizadores.
 *
 * <p>As queries são direcionais: {@code user} = remetente, {@code friend} = destinatário.
 * Para verificar o estado de uma relação bidirecional, consultar ambas as direções.
 *
 * @see com.jep.servidor.model.UserRelation
 * @see com.jep.servidor.service.impl.UserRelationshipServiceImpl
 */
public interface UserRelationRepository extends JpaRepository<UserRelation, Long> {

  /**
   * Devolve todas as relações em que o utilizador é o remetente.
   *
   * @param user o utilizador remetente.
   * @return lista de relações iniciadas pelo utilizador.
   */
  List<UserRelation> findByUser(User user);

  /**
   * Devolve as relações de um tipo específico em que o utilizador é o remetente.
   *
   * @param user o utilizador remetente.
   * @param type o tipo de relação a filtrar.
   * @return lista de relações do tipo especificado.
   */
  List<UserRelation> findByUserAndType(User user, UserRelation.RelationType type);

  /**
   * Devolve as relações de um tipo específico em que o utilizador é o destinatário.
   * Usado para obter pedidos de amizade pendentes recebidos.
   *
   * @param friendId ID do destinatário.
   * @param type     o tipo de relação a filtrar.
   * @return lista de relações do tipo especificado recebidas pelo utilizador.
   */
  List<UserRelation> findByFriendIdAndType(Long friendId, UserRelation.RelationType type);

  /**
   * Encontra a relação direcional de {@code userId1} para {@code userId2}.
   *
   * <p>Retorna apenas o registo onde {@code user.id = userId1} e {@code friend.id = userId2}.
   * Para verificar amizade bidirecional, chamar duas vezes com os IDs invertidos.
   *
   * @param userId1 ID do remetente/iniciador.
   * @param userId2 ID do destinatário.
   * @return a relação se existir, ou {@link java.util.Optional#empty()}.
   */
  @Query("SELECT r FROM UserRelation r WHERE r.user.id = :userId1 AND r.friend.id = :userId2")
  Optional<UserRelation> findRelationship(
      @Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
