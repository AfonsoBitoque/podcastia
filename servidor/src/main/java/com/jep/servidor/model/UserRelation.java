package com.jep.servidor.model;

import jakarta.persistence.*;

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

    public enum RelationType {
        AMIGO, BLOQUEADO, PEDIDO
    }

    public UserRelation() {}

    public UserRelation(User user, User friend, RelationType type) {
        this.user = user;
        this.friend = friend;
        this.type = type;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getFriend() { return friend; }
    public void setFriend(User friend) { this.friend = friend; }
    public RelationType getType() { return type; }
    public void setType(RelationType type) { this.type = type; }
}