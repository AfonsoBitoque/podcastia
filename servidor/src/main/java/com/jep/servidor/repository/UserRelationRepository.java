package com.jep.servidor.repository;

import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para operações de base de dados relacionadas com relações entre utilizadores.
 */
public interface UserRelationRepository extends JpaRepository<UserRelation, Long> {
    List<UserRelation> findByUser(User user);

    List<UserRelation> findByUserAndType(User user, UserRelation.RelationType type);
}
