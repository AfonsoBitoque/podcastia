package com.jep.servidor.repository;

import com.jep.servidor.model.Playlist;
import com.jep.servidor.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório Spring Data JPA para playlists de utilizadores.
 *
 * <p>Fornece consultas para o {@link com.jep.servidor.service.PlaylistService}
 * e para o feed social de playlists públicas de amigos.
 */
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

  /**
   * Elimina todas as playlists de um utilizador.
   * Usado ao eliminar um utilizador da plataforma.
   *
   * @param owner o utilizador dono das playlists.
   */
  void deleteByOwner(User owner);

  /**
   * Devolve todas as playlists de um utilizador, ordenadas pela data de atualização.
   *
   * @param owner utilizador dono das playlists.
   * @return lista ordenada por {@code updatedAt} descendente.
   */
  List<Playlist> findByOwnerOrderByUpdatedAtDesc(User owner);

  /**
   * Devolve as playlists públicas de um utilizador específico.
   *
   * @param ownerId ID do dono das playlists.
   * @return playlists públicas ordenadas por {@code updatedAt} descendente.
   */
  List<Playlist> findByOwnerIdAndIsPublicTrueOrderByUpdatedAtDesc(Long ownerId);

  /**
   * Devolve as playlists públicas de todos os amigos do utilizador.
   *
   * <p>Usa uma sub-query JPQL para obter os IDs dos amigos (registos
   * {@link com.jep.servidor.model.UserRelation.RelationType#AMIGO})
   * e filtra as playlists públicas cujo dono é um desses amigos.
   *
   * @param userId ID do utilizador autenticado.
   * @return playlists públicas dos amigos, ordenadas por {@code updatedAt} descendente.
   */
  @Query("SELECT p FROM Playlist p "
      + "WHERE p.isPublic = true "
      + "AND p.owner.id IN ("
      + "  SELECT CASE WHEN r.user.id = :userId THEN r.friend.id ELSE r.user.id END "
      + "  FROM UserRelation r "
      + "  WHERE (r.user.id = :userId OR r.friend.id = :userId) "
      + "    AND r.type = com.jep.servidor.model.UserRelation.RelationType.AMIGO"
      + ") "
      + "ORDER BY p.updatedAt DESC")
  List<Playlist> findPublicPlaylistsFromFriends(@Param("userId") Long userId);
}
