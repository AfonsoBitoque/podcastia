package com.jep.servidor.dto;

import com.jep.servidor.model.PodcastTag;
import java.time.LocalDateTime;
import java.util.List;

public class UserProfileDto {
    private Long id;
    private String username;
    private String tag;
    private String bio;
    private String profilePicturePath;
    private int pontosDesporto;
    private int pontosPolitica;
    private int pontosFinancas;
    private int pontosGeral;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private List<PodcastTag> topics;

    public UserProfileDto() {}

    public UserProfileDto(Long id, String username, String tag, String bio, String profilePicturePath, 
                          int pontosDesporto, int pontosPolitica, int pontosFinancas, int pontosGeral, 
                          LocalDateTime createdAt, LocalDateTime lastActiveAt, List<PodcastTag> topics) {
        this.id = id;
        this.username = username;
        this.tag = tag;
        this.bio = bio;
        this.profilePicturePath = profilePicturePath;
        this.pontosDesporto = pontosDesporto;
        this.pontosPolitica = pontosPolitica;
        this.pontosFinancas = pontosFinancas;
        this.pontosGeral = pontosGeral;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.topics = topics;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfilePicturePath() { return profilePicturePath; }
    public void setProfilePicturePath(String profilePicturePath) { this.profilePicturePath = profilePicturePath; }

    public int getPontosDesporto() { return pontosDesporto; }
    public void setPontosDesporto(int pontosDesporto) { this.pontosDesporto = pontosDesporto; }

    public int getPontosPolitica() { return pontosPolitica; }
    public void setPontosPolitica(int pontosPolitica) { this.pontosPolitica = pontosPolitica; }

    public int getPontosFinancas() { return pontosFinancas; }
    public void setPontosFinancas(int pontosFinancas) { this.pontosFinancas = pontosFinancas; }

    public int getPontosGeral() { return pontosGeral; }
    public void setPontosGeral(int pontosGeral) { this.pontosGeral = pontosGeral; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public List<PodcastTag> getTopics() { return topics; }
    public void setTopics(List<PodcastTag> topics) { this.topics = topics; }
}
