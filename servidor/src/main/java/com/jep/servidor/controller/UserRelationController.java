package com.jep.servidor.controller;

import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.UserRelationRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gerir relações entre utilizadores.
 */
@RestController
@RequestMapping("/relations")
public class UserRelationController {
    private final UserRelationRepository relationRepository;

    /**
     * Construtor para injeção de dependências.
     *
     * @param relationRepository Repositório de relações.
     */
    public UserRelationController(UserRelationRepository relationRepository) {
        this.relationRepository = relationRepository;
    }

    /**
     * Retorna todas as relações de um utilizador.
     *
     * @param userId ID do utilizador.
     * @return Lista de relações.
     */
    @GetMapping("/user/{userId}")
    public List<UserRelation> getAllRelations(@PathVariable("userId") Long userId) {
        com.jep.servidor.model.User user = new com.jep.servidor.model.User();
        user.setId(userId);
        return relationRepository.findByUser(user);
    }

    /**
     * Retorna relações de um utilizador por tipo.
     *
     * @param userId ID do utilizador.
     * @param type Tipo de relação.
     * @return Lista de relações filtradas.
     */
    @GetMapping("/user/{userId}/{type}")
    public List<UserRelation> getRelationsByType(@PathVariable("userId") Long userId,
            @PathVariable("type") UserRelation.RelationType type) {
        com.jep.servidor.model.User user = new com.jep.servidor.model.User();
        user.setId(userId);
        return relationRepository.findByUserAndType(user, type);
    }

    /**
     * Cria uma nova relação.
     *
     * @param relation Dados da relação a criar.
     * @return A relação criada.
     */
    @PostMapping
    public ResponseEntity<UserRelation> create(@RequestBody UserRelation relation) {
        UserRelation saved = relationRepository.save(relation);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Remove uma relação pelo ID.
     *
     * @param id ID da relação a remover.
     * @return Resposta sem conteúdo ou 404.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (!relationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        relationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
