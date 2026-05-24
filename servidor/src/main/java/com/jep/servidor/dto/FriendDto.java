package com.jep.servidor.dto;

/**
 * DTO de representação resumida de um amigo na lista de amizades.
 *
 * <p>Devolvido pelo endpoint {@code GET /api/relations/friends} em
 * {@link com.jep.servidor.controller.UserRelationController}.
 * Contém apenas os dados necessários para exibir o amigo na UI
 * (ID, nome de utilizador e caminho da foto de perfil).
 */
public class FriendDto {
    private Long id;
    private String username;
    private String profilePicturePath;

    public FriendDto(Long id, String username, String profilePicturePath) {
        this.id = id;
        this.username = username;
        this.profilePicturePath = profilePicturePath;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfilePicturePath() {
        return profilePicturePath;
    }

    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }
}
