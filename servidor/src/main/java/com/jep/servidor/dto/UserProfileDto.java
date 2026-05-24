package com.jep.servidor.dto;

import com.jep.servidor.model.PodcastTag;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO do perfil público de um utilizador.
 *
 * <p>Devolvido pelo endpoint {@code GET /users/{id}/profile} em
 * {@link com.jep.servidor.controller.UserController}.
 * Omite informação sensível (email, password) e expande dados
 * de afinidade por categoria de podcast.
 *
 * <p>Os campos {@code pontosDesporto}, {@code pontosPolitica},
 * {@code pontosFinancas} e {@code pontosGeral} são usados pelo
 * {@link com.jep.servidor.service.RecommendationService} para
 * personalizar o feed, e podem ser exibidos no perfil como indicadores
 * de interesses do utilizador.
 */
public class UserProfileDto {
    /** ID único do utilizador. */
    private Long id;
    /** Nome de utilizador (não único, combinado com {@code tag} para identificação). */
    private String username;
    /** Tag de 4 dígitos única para o username (ex: {@code "0042"}). */
    private String tag;
    /** Biografia do utilizador (até 160 caracteres). */
    private String bio;
    /** Caminho relativo para a foto de perfil (ou {@code null} se não definida). */
    private String profilePicturePath;
    /** Pontos de afinidade com podcasts de desporto. */
    private int pontosDesporto;
    /** Pontos de afinidade com podcasts de política. */
    private int pontosPolitica;
    /** Pontos de afinidade com podcasts de finanças. */
    private int pontosFinancas;
    /** Pontos de afinidade com podcasts gerais. */
    private int pontosGeral;
    /** Data de registo na plataforma. */
    private LocalDateTime createdAt;
    /** Data da última sessão ativa. */
    private LocalDateTime lastActiveAt;
    /** Tópicos de interesse selecionados pelo utilizador (valores de {@link PodcastTag}). */
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
