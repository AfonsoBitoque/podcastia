package com.jep.servidor.repository;

import com.jep.servidor.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Repositório para operações de base de dados relacionadas com utilizadores.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByUsernameAndTag(String username, String tag);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameAndTag(String username, String tag);

    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email);

    @Query("SELECT COUNT(r) FROM UserRelation r "
            + "WHERE (r.user.id = :userId OR r.friend.id = :userId) AND r.type = 'AMIGO'")
    long countFriendships(@RequestParam("userId") Long userId);
}
