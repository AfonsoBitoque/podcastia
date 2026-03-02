package com.jep.servidor.repository;

import com.jep.servidor.model.UserRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.jep.servidor.model.User;
import java.util.List;

public interface UserRelationRepository extends JpaRepository<UserRelation, Long> {
    List<UserRelation> findByUser(User user);
    List<UserRelation> findByUserAndType(User user, UserRelation.RelationType type);
}