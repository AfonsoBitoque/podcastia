package com.jep.servidor.repository;

import com.jep.servidor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByUsernameAndTag(String username, String tag);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameAndTag(String username, String tag);

    java.util.List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) FROM UserRelation r WHERE (r.user.id = :userId OR r.friend.id = :userId) AND r.type = 'AMIGO'")
    long countFriendships(@org.springframework.web.bind.annotation.RequestParam("userId") Long userId);
}
