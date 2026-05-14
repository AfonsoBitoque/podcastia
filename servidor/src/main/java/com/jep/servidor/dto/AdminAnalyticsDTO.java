package com.jep.servidor.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Admin Analytics Dashboard
 */
public class AdminAnalyticsDTO {
    
    // User Metrics
    private long dailyActiveUsers;
    private long monthlyActiveUsers;
    private long totalUsers;
    private long newRegistrationsToday;
    private long newRegistrationsThisMonth;
    
    // Podcast Metrics
    private long totalPodcasts;
    private long totalListeningTime; // in minutes
    private List<PodcastRankingDTO> topPodcasts;
    
    // Usage Evolution Data
    private List<UsageDataPointDTO> weeklyUsage;
    private List<UsageDataPointDTO> monthlyUsage;
    
    // System Health
    private Map<String, Object> systemHealth;
    
    // Timestamp
    private LocalDateTime generatedAt;
    
    // Constructors
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
     * DTO for Podcast Ranking
     */
    public static class PodcastRankingDTO {
        private Long podcastId;
        private String title;
        private String author;
        private long totalPlays;
        private long totalListeningTime; // in minutes
        private double averageRating;
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
     * DTO for Usage Data Points (for charts)
     */
    public static class UsageDataPointDTO {
        private String date;
        private long activeUsers;
        private long newUsers;
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
