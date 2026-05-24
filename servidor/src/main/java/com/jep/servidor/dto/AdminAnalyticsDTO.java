package com.jep.servidor.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO principal do painel de analytics para administradores.
 *
 * <p>Agrega métricas de utilizadores, podcasts, evolução de uso temporal
 * e saúde do sistema, gerado pelo {@link com.jep.servidor.service.AdminService}.
 *
 * <p>Contém duas inner classes:
 * <ul>
 *   <li>{@link PodcastRankingDTO} — dados de ranking de podcasts mais reproduzidos.</li>
 *   <li>{@link UsageDataPointDTO} — ponto de dados de uso para gráficos de evolução.</li>
 * </ul>
 */
public class AdminAnalyticsDTO {
    
    /** Número de utilizadores ativos nas últimas 24 horas. */
    private long dailyActiveUsers;
    /** Número de utilizadores ativos no último mês. */
    private long monthlyActiveUsers;
    /** Total de utilizadores registados na plataforma. */
    private long totalUsers;
    /** Novos registos hoje. */
    private long newRegistrationsToday;
    /** Novos registos no mês atual. */
    private long newRegistrationsThisMonth;
    /** Total de podcasts na plataforma. */
    private long totalPodcasts;
    /** Tempo total de audição acumulado em minutos. */
    private long totalListeningTime;
    /** Lista dos podcasts mais reproduzidos, ordenados por ranking. */
    private List<PodcastRankingDTO> topPodcasts;
    /** Dados de uso diário/semanal para gráfico de evolução semanal. */
    private List<UsageDataPointDTO> weeklyUsage;
    /** Dados de uso diário/mensal para gráfico de evolução mensal. */
    private List<UsageDataPointDTO> monthlyUsage;
    /** Mapa de métricas de saúde do sistema (CPU, memória, BD, etc.). */
    private Map<String, Object> systemHealth;
    /** Data e hora em que este snapshot foi gerado. */
    private LocalDateTime generatedAt;
    
    /** Construtor padrão para deserialização. */
    public AdminAnalyticsDTO() {}
    
    public AdminAnalyticsDTO(long dailyActiveUsers, long monthlyActiveUsers, long totalUsers,
                             long newRegistrationsToday, long newRegistrationsThisMonth,
                             long totalPodcasts, long totalListeningTime,
                             List<PodcastRankingDTO> topPodcasts,
                             List<UsageDataPointDTO> weeklyUsage,
                             List<UsageDataPointDTO> monthlyUsage,
                             Map<String, Object> systemHealth) {
        this.dailyActiveUsers = dailyActiveUsers;
        this.monthlyActiveUsers = monthlyActiveUsers;
        this.totalUsers = totalUsers;
        this.newRegistrationsToday = newRegistrationsToday;
        this.newRegistrationsThisMonth = newRegistrationsThisMonth;
        this.totalPodcasts = totalPodcasts;
        this.totalListeningTime = totalListeningTime;
        this.topPodcasts = topPodcasts;
        this.weeklyUsage = weeklyUsage;
        this.monthlyUsage = monthlyUsage;
        this.systemHealth = systemHealth;
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public long getDailyActiveUsers() { return dailyActiveUsers; }
    public void setDailyActiveUsers(long dailyActiveUsers) { this.dailyActiveUsers = dailyActiveUsers; }
    
    public long getMonthlyActiveUsers() { return monthlyActiveUsers; }
    public void setMonthlyActiveUsers(long monthlyActiveUsers) { this.monthlyActiveUsers = monthlyActiveUsers; }
    
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    
    public long getNewRegistrationsToday() { return newRegistrationsToday; }
    public void setNewRegistrationsToday(long newRegistrationsToday) { this.newRegistrationsToday = newRegistrationsToday; }
    
    public long getNewRegistrationsThisMonth() { return newRegistrationsThisMonth; }
    public void setNewRegistrationsThisMonth(long newRegistrationsThisMonth) { this.newRegistrationsThisMonth = newRegistrationsThisMonth; }
    
    public long getTotalPodcasts() { return totalPodcasts; }
    public void setTotalPodcasts(long totalPodcasts) { this.totalPodcasts = totalPodcasts; }
    
    public long getTotalListeningTime() { return totalListeningTime; }
    public void setTotalListeningTime(long totalListeningTime) { this.totalListeningTime = totalListeningTime; }
    
    public List<PodcastRankingDTO> getTopPodcasts() { return topPodcasts; }
    public void setTopPodcasts(List<PodcastRankingDTO> topPodcasts) { this.topPodcasts = topPodcasts; }
    
    public List<UsageDataPointDTO> getWeeklyUsage() { return weeklyUsage; }
    public void setWeeklyUsage(List<UsageDataPointDTO> weeklyUsage) { this.weeklyUsage = weeklyUsage; }
    
    public List<UsageDataPointDTO> getMonthlyUsage() { return monthlyUsage; }
    public void setMonthlyUsage(List<UsageDataPointDTO> monthlyUsage) { this.monthlyUsage = monthlyUsage; }
    
    public Map<String, Object> getSystemHealth() { return systemHealth; }
    public void setSystemHealth(Map<String, Object> systemHealth) { this.systemHealth = systemHealth; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    /**
     * DTO de ranking de um podcast no painel de analytics.
     *
     * <p>Exposta na lista {@link AdminAnalyticsDTO#getTopPodcasts()},
     * ordenada pelo campo {@link #rank}.
     */
    public static class PodcastRankingDTO {
        /** ID do podcast. */
        private Long podcastId;
        /** Título do podcast. */
        private String title;
        /** Nome do autor/criador. */
        private String author;
        /** Número total de reproduções. */
        private long totalPlays;
        /** Tempo total de audição em minutos. */
        private long totalListeningTime;
        /** Classificação média (0.0–5.0). */
        private double averageRating;
        /** Posição no ranking (1 = mais reproduzido). */
        private int rank;
        
        public PodcastRankingDTO() {}
        
        public PodcastRankingDTO(Long podcastId, String title, String author, 
                                 long totalPlays, long totalListeningTime, 
                                 double averageRating, int rank) {
            this.podcastId = podcastId;
            this.title = title;
            this.author = author;
            this.totalPlays = totalPlays;
            this.totalListeningTime = totalListeningTime;
            this.averageRating = averageRating;
            this.rank = rank;
        }
        
        // Getters and Setters
        public Long getPodcastId() { return podcastId; }
        public void setPodcastId(Long podcastId) { this.podcastId = podcastId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public long getTotalPlays() { return totalPlays; }
        public void setTotalPlays(long totalPlays) { this.totalPlays = totalPlays; }
        
        public long getTotalListeningTime() { return totalListeningTime; }
        public void setTotalListeningTime(long totalListeningTime) { this.totalListeningTime = totalListeningTime; }
        
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
    }
    
    /**
     * Ponto de dados de uso para construção de gráficos de evolução.
     *
     * <p>Cada instância representa um dia ou período agrupado, usada nas
     * listas {@link AdminAnalyticsDTO#getWeeklyUsage()} e
     * {@link AdminAnalyticsDTO#getMonthlyUsage()}.
     */
    public static class UsageDataPointDTO {
        /** Data do ponto (formato {@code yyyy-MM-dd} ou descrição do período). */
        private String date;
        /** Utilizadores ativos neste período. */
        private long activeUsers;
        /** Novos utilizadores registados neste período. */
        private long newUsers;
        /** Tempo de audição total neste período (em minutos). */
        private long listeningTime;
        
        public UsageDataPointDTO() {}
        
        public UsageDataPointDTO(String date, long activeUsers, long newUsers, long listeningTime) {
            this.date = date;
            this.activeUsers = activeUsers;
            this.newUsers = newUsers;
            this.listeningTime = listeningTime;
        }
        
        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
        
        public long getNewUsers() { return newUsers; }
        public void setNewUsers(long newUsers) { this.newUsers = newUsers; }
        
        public long getListeningTime() { return listeningTime; }
        public void setListeningTime(long listeningTime) { this.listeningTime = listeningTime; }
    }
}
