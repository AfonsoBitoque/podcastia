package com.jep.servidor.repository;

import com.jep.servidor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsernameAndTag(String username, String tag);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameAndTag(String username, String tag);
}
