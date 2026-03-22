package com.jep.servidor.repository;

import com.jep.servidor.model.Playlist;
import com.jep.servidor.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório para operações de base de dados relacionadas com playlists.
 */
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

  List<Playlist> findByOwnerOrderByUpdatedAtDesc(User owner);

  List<Playlist> findByOwnerIdAndIsPublicTrueOrderByUpdatedAtDesc(Long ownerId);

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
