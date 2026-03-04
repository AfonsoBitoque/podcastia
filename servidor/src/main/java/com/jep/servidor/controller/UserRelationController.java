package com.jep.servidor.controller;

import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.UserRelationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/relations")
public class UserRelationController {
    private final UserRelationRepository relationRepository;

    public UserRelationController(UserRelationRepository relationRepository) {
        this.relationRepository = relationRepository;
    }

    @GetMapping("/user/{userId}")
    public List<UserRelation> getAllRelations(@PathVariable("userId") Long userId) {
        com.jep.servidor.model.User user = new com.jep.servidor.model.User();
        user.setId(userId);
        return relationRepository.findByUser(user);
    }

    @GetMapping("/user/{userId}/{type}")
    public List<UserRelation> getRelationsByType(@PathVariable("userId") Long userId,
            @PathVariable("type") UserRelation.RelationType type) {
        com.jep.servidor.model.User user = new com.jep.servidor.model.User();
        user.setId(userId);
        return relationRepository.findByUserAndType(user, type);
    }

    @PostMapping
    public ResponseEntity<UserRelation> create(@RequestBody UserRelation relation) {
        UserRelation saved = relationRepository.save(relation);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (!relationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        relationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}