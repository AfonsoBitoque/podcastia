package com.jep.servidor.repository;

import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações de base de dados relacionadas com relações entre utilizadores.
 */
public interface UserRelationRepository extends JpaRepository<UserRelation, Long> {
  List<UserRelation> findByUser(User user);

  List<UserRelation> findByUserAndType(User user, UserRelation.RelationType type);

  List<UserRelation> findByFriendIdAndType(Long friendId, UserRelation.RelationType type);

  @Query("SELECT r FROM UserRelation r WHERE (r.user.id = :userId1 AND r.friend.id = :userId2) OR (r.user.id = :userId2 AND r.friend.id = :userId1)")
  Optional<UserRelation> findRelationship(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
