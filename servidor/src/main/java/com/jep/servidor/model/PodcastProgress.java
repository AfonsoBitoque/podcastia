package com.jep.servidor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA que regista o progresso de audição de um utilizador num podcast.
 *
 * <p>Armazena o útimo ponto de audição em segundos ({@code progressSeconds})
 * e a data/hora da última audição ({@code lastListenedAt}), usados por:
 * <ul>
 *   <li>{@link com.jep.servidor.controller.PodcastController#continueListening}
 *       — para devolver os 10 podcasts mais recentemente ouvidos.</li>
 *   <li>{@link com.jep.servidor.service.AdminService}
 *       — para calcular métricas de tempo de audição no painel admin.</li>
 * </ul>
 *
 * <p><b>Tabela:</b> {@code podcast_progress}
 *
 * @see com.jep.servidor.repository.PodcastProgressRepository
 */
@Entity
@Table(name = "podcast_progress")
public class PodcastProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "podcast_id", nullable = false)
    private Podcast podcast;

    @Column(nullable = false)
    private int progressSeconds;

    @Column(nullable = false)
    private int totalListenedSeconds;

    @Column(nullable = false)
    private int playCount;

    @Column(nullable = false)
    private boolean hasCompleted;

    @Column(nullable = false)
    private LocalDateTime lastListenedAt;

    public PodcastProgress() {
    }

    public PodcastProgress(User user, Podcast podcast, int progressSeconds) {
        this.user = user;
        this.podcast = podcast;
        this.progressSeconds = progressSeconds;
        this.totalListenedSeconds = 0;
        this.playCount = 1;
        this.hasCompleted = false;
        this.lastListenedAt = LocalDateTime.now();
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

    public Podcast getPodcast() {
        return podcast;
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
    }

    public int getProgressSeconds() {
        return progressSeconds;
    }

    public void setProgressSeconds(int progressSeconds) {
        this.progressSeconds = progressSeconds;
    }

    public LocalDateTime getLastListenedAt() {
        return lastListenedAt;
    }

    public void setLastListenedAt(LocalDateTime lastListenedAt) {
        this.lastListenedAt = lastListenedAt;
    }

    public int getTotalListenedSeconds() {
        return totalListenedSeconds;
    }

    public void setTotalListenedSeconds(int totalListenedSeconds) {
        this.totalListenedSeconds = totalListenedSeconds;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }

    public boolean isHasCompleted() {
        return hasCompleted;
    }

    public void setHasCompleted(boolean hasCompleted) {
        this.hasCompleted = hasCompleted;
    }
}
