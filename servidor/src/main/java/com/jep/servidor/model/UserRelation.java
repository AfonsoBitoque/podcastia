package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidade que representa uma relação entre utilizadores.
 */
@Entity
@Table(name = "user_relations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "friend_id"})
    },
    indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "friend_id"),
        @Index(columnList = "type")
    }
)
public class UserRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationType type;

    /**
     * Tipos de relação possíveis.
     */
    public enum RelationType {
        AMIGO, BLOQUEADO, PEDIDO
    }

    /**
     * Construtor padrão.
     */
    public UserRelation() {
    }

    /**
     * Construtor com parâmetros.
     *
     * @param user Utilizador principal.
     * @param friend Utilizador relacionado.
     * @param type Tipo de relação.
     */
    public UserRelation(User user, User friend, RelationType type) {
        this.user = user;
        this.friend = friend;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getFriend() {
        return friend;
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
    }
}
