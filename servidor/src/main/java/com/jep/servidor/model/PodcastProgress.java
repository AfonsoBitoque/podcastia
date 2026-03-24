package com.jep.servidor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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
    private LocalDateTime lastListenedAt;

    public PodcastProgress() {
    }

    public PodcastProgress(User user, Podcast podcast, int progressSeconds) {
        this.user = user;
        this.podcast = podcast;
        this.progressSeconds = progressSeconds;
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
}
