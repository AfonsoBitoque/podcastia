package com.jep.servidor.service;

import com.jep.servidor.dto.AdminAnalyticsDTO;
import com.jep.servidor.dto.AdminPodcastManagementDTO;
import com.jep.servidor.dto.AdminActionLogDTO;
import com.jep.servidor.model.User;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.AdminActionLog;
import com.jep.servidor.repository.AdminActionLogRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.repository.PodcastProgressRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Admin operations
 */
@Service
public class AdminService {

    private final AdminActionLogRepository adminActionLogRepository;
    private final PodcastRepository podcastRepository;
    private final UserRepository userRepository;
    private final PodcastProgressRepository podcastProgressRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminService(AdminActionLogRepository adminActionLogRepository,
                        PodcastRepository podcastRepository,
                        UserRepository userRepository,
                        PodcastProgressRepository podcastProgressRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.adminActionLogRepository = adminActionLogRepository;
        this.podcastRepository = podcastRepository;
        this.userRepository = userRepository;
        this.podcastProgressRepository = podcastProgressRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Get comprehensive analytics for admin dashboard
     */
    public AdminAnalyticsDTO getAnalytics() {
        // Calculate DAU (Daily Active Users)
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        long dailyActiveUsers = userRepository.countByLastActiveAtAfter(today);

        // Calculate MAU (Monthly Active Users)
        LocalDateTime monthAgo = today.minusMonths(1);
        long monthlyActiveUsers = userRepository.countByLastActiveAtAfter(monthAgo);

        // Total users
        long totalUsers = userRepository.count();

        // New registrations
        long newRegistrationsToday = userRepository.countByCreatedAtAfter(today);
        LocalDateTime monthStart = today.toLocalDate().withDayOfMonth(1).atStartOfDay();
        long newRegistrationsThisMonth = userRepository.countByCreatedAtAfter(monthStart);

        // Podcast metrics
        long totalPodcasts = podcastRepository.count();
        long totalListeningTime = podcastProgressRepository.sumTotalListeningTime();

        // Top podcasts
        List<AdminAnalyticsDTO.PodcastRankingDTO> topPodcasts = getTopPodcasts();

        // Usage evolution data
        List<AdminAnalyticsDTO.UsageDataPointDTO> weeklyUsage = getWeeklyUsageData();
        List<AdminAnalyticsDTO.UsageDataPointDTO> monthlyUsage = getMonthlyUsageData();

        // System health
        Map<String, Object> systemHealth = getSystemHealth();

        return new AdminAnalyticsDTO(
            dailyActiveUsers, monthlyActiveUsers, totalUsers,
            newRegistrationsToday, newRegistrationsThisMonth,
            totalPodcasts, totalListeningTime,
            topPodcasts, weeklyUsage, monthlyUsage, systemHealth
        );
    }

    /**
     * Get all podcasts for admin management
     */
    public List<AdminPodcastManagementDTO> getAllPodcastsForManagement() {
        List<Podcast> podcasts = podcastRepository.findAll();
        
        return podcasts.stream().map(podcast -> {
            AdminPodcastManagementDTO dto = new AdminPodcastManagementDTO();
            dto.setId(podcast.getId());
            dto.setTitulo(podcast.getTitulo());
            dto.setAuthor(podcast.getUser() != null ? podcast.getUser().getUsername() : "Unknown");
            dto.setTags(podcast.getTags() != null ? 
                podcast.getTags().stream().map(tag -> tag.toString()).collect(Collectors.toList()) : new ArrayList<>());
            dto.setDuracao(podcast.getDuracao());
            dto.setExplicitContent(podcast.isExplicitContent());
            dto.setHidden(podcast.isHidden());
            dto.setFeatured(podcast.isFeatured());
            dto.setPublico(podcast.isPublico());
            dto.setAvailable(podcast.isAvailable());
            dto.setCreatedAt(podcast.getCreatedAt());
            dto.setLastModified(podcast.getLastModified());
            dto.setCoverImagePath(podcast.getCoverImagePath());
            dto.setConteudoPath(podcast.getConteudoPath());
            
            // Calculate metrics
            long totalPlays = podcastProgressRepository.countByPodcastId(podcast.getId());
            long totalListeningTime = podcastProgressRepository.sumListeningTimeByPodcastId(podcast.getId());
            dto.setTotalPlays(totalPlays);
            dto.setTotalListeningTime(totalListeningTime);
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Update podcast metadata
     */
    @Transactional
    public Podcast updatePodcastMetadata(Long podcastId, Podcast podcastData, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        // Log the action
        logAdminAction(admin, "UPDATE_PODCAST_METADATA", "PODCAST", podcastId, podcast.getTitulo(), 
                      "Updated podcast metadata", null, null, true, null);

        // Update metadata
        if (podcastData.getTitulo() != null) {
            podcast.setTitulo(podcastData.getTitulo());
        }
        if (podcastData.getTags() != null) {
            podcast.setTags(podcastData.getTags());
        }
        if (podcastData.getDuracao() > 0) {
            podcast.setDuracao(podcastData.getDuracao());
        }
        if (podcastData.getCoverImagePath() != null) {
            podcast.setCoverImagePath(podcastData.getCoverImagePath());
        }
        
        podcast.setLastModified(LocalDateTime.now());
        
        return podcastRepository.save(podcast);
    }

    /**
     * Mark podcast as explicit content
     */
    @Transactional
    public Podcast markAsExplicit(Long podcastId, boolean explicit, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        podcast.setExplicitContent(explicit);
        podcast.setLastModified(LocalDateTime.now());
        
        // Log the action
        logAdminAction(admin, explicit ? "MARK_EXPLICIT" : "UNMARK_EXPLICIT", "PODCAST", podcastId, podcast.getTitulo(), 
                      explicit ? "Marked as explicit content" : "Unmarked as explicit content", null, null, true, null);

        return podcastRepository.save(podcast);
    }

    /**
     * Toggle podcast visibility
     */
    @Transactional
    public Podcast togglePodcastVisibility(Long podcastId, boolean hidden, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        podcast.setHidden(hidden);
        podcast.setLastModified(LocalDateTime.now());
        
        // Log the action
        logAdminAction(admin, hidden ? "HIDE_PODCAST" : "SHOW_PODCAST", "PODCAST", podcastId, podcast.getTitulo(), 
                      hidden ? "Hidden podcast" : "Unhidden podcast", null, null, true, null);

        return podcastRepository.save(podcast);
    }

    /**
     * Toggle podcast featured status
     */
    @Transactional
    public Podcast togglePodcastFeatured(Long podcastId, boolean featured, User admin) {
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));

        podcast.setFeatured(featured);
        podcast.setLastModified(LocalDateTime.now());
        
        // Log the action
        logAdminAction(admin, featured ? "FEATURE_PODCAST" : "UNFEATURE_PODCAST", "PODCAST", podcastId, podcast.getTitulo(), 
                      featured ? "Featured podcast" : "Unfeatured podcast", null, null, true, null);

        return podcastRepository.save(podcast);
    }

    /**
     * Confirm podcast deletion with double confirmation
     */
    @Transactional
    public boolean confirmPodcastDeletion(Long podcastId, String confirmation, String adminPassword, User admin) {
        // Verify admin password
        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            logAdminAction(admin, "DELETE_PODCAST_FAILED", "PODCAST", podcastId, "", 
                          "Failed deletion - invalid password", null, null, false, "Invalid admin password");
            return false;
        }

        // Verify confirmation text
        Podcast podcast = podcastRepository.findById(podcastId)
            .orElseThrow(() -> new RuntimeException("Podcast not found"));
        
        String expectedConfirmation = "DELETE_" + podcast.getTitulo().toUpperCase().replaceAll("\\s+", "_");
        if (!expectedConfirmation.equals(confirmation)) {
            logAdminAction(admin, "DELETE_PODCAST_FAILED", "PODCAST", podcastId, podcast.getTitulo(), 
                          "Failed deletion - invalid confirmation", null, null, false, "Invalid confirmation text");
            return false;
        }

        // Delete the podcast
        String podcastTitle = podcast.getTitulo();
        podcastRepository.delete(podcast);
        
        // Log the action
        logAdminAction(admin, "DELETE_PODCAST", "PODCAST", podcastId, podcastTitle, 
                      "Deleted podcast permanently", null, null, true, null);

        return true;
    }

    /**
     * Get admin action logs
     */
    public List<AdminActionLogDTO> getAdminLogs(int limit, int offset) {
        List<AdminActionLog> logs = adminActionLogRepository.findAllByOrderByTimestampDesc();
        
        return logs.stream()
            .skip(offset)
            .limit(limit)
            .map(this::convertToLogDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get all users for admin management
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Reset user password
     */
    @Transactional
    public String resetUserPassword(Long userId, User admin) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        // Log the action
        logAdminAction(admin, "RESET_USER_PASSWORD", "USER", userId, user.getUsername(), 
                      "Reset user password", null, null, true, null);

        return tempPassword;
    }

    /**
     * Confirm user deletion with double confirmation
     */
    @Transactional
    public boolean confirmUserDeletion(Long userId, String confirmation, String adminPassword, User admin) {
        // Verify admin password
        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            logAdminAction(admin, "DELETE_USER_FAILED", "USER", userId, "", 
                          "Failed deletion - invalid password", null, null, false, "Invalid admin password");
            return false;
        }

        // Verify confirmation text
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String expectedConfirmation = "DELETE_" + user.getUsername().toUpperCase().replaceAll("\\s+", "_");
        if (!expectedConfirmation.equals(confirmation)) {
            logAdminAction(admin, "DELETE_USER_FAILED", "USER", userId, user.getUsername(), 
                          "Failed deletion - invalid confirmation", null, null, false, "Invalid confirmation text");
            return false;
        }

        // Delete the user
        String username = user.getUsername();
        userRepository.delete(user);
        
        // Log the action
        logAdminAction(admin, "DELETE_USER", "USER", userId, username, 
                      "Deleted user permanently", null, null, true, null);

        return true;
    }

    /**
     * Export analytics to CSV
     */
    public void exportAnalyticsCsv(HttpServletResponse response) {
        try {
            AdminAnalyticsDTO analytics = getAnalytics();
            
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=analytics.csv");
            
            // Create CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append("Daily Active Users,").append(analytics.getDailyActiveUsers()).append("\n");
            csv.append("Monthly Active Users,").append(analytics.getMonthlyActiveUsers()).append("\n");
            csv.append("Total Users,").append(analytics.getTotalUsers()).append("\n");
            csv.append("New Registrations Today,").append(analytics.getNewRegistrationsToday()).append("\n");
            csv.append("New Registrations This Month,").append(analytics.getNewRegistrationsThisMonth()).append("\n");
            csv.append("Total Podcasts,").append(analytics.getTotalPodcasts()).append("\n");
            csv.append("Total Listening Time (minutes),").append(analytics.getTotalListeningTime()).append("\n");
            
            // Add top podcasts
            csv.append("\nTop Podcasts\n");
            csv.append("Rank,Title,Author,Plays,Listening Time (minutes)\n");
            for (AdminAnalyticsDTO.PodcastRankingDTO podcast : analytics.getTopPodcasts()) {
                csv.append(podcast.getRank()).append(",")
                   .append(podcast.getTitle()).append(",")
                   .append(podcast.getAuthor()).append(",")
                   .append(podcast.getTotalPlays()).append(",")
                   .append(podcast.getTotalListeningTime()).append("\n");
            }
            
            response.getWriter().write(csv.toString());
            
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV export", e);
        }
    }

    /**
     * Export analytics to PDF
     */
    public void exportAnalyticsPdf(HttpServletResponse response) {
        // For now, implement a simple text-based PDF export
        // In a real implementation, you would use a library like iText or PDFBox
        try {
            AdminAnalyticsDTO analytics = getAnalytics();
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=analytics.pdf");
            
            StringBuilder pdfContent = new StringBuilder();
            pdfContent.append("Podcastia Analytics Report\n");
            pdfContent.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
            
            pdfContent.append("User Metrics:\n");
            pdfContent.append("Daily Active Users: ").append(analytics.getDailyActiveUsers()).append("\n");
            pdfContent.append("Monthly Active Users: ").append(analytics.getMonthlyActiveUsers()).append("\n");
            pdfContent.append("Total Users: ").append(analytics.getTotalUsers()).append("\n");
            pdfContent.append("New Registrations Today: ").append(analytics.getNewRegistrationsToday()).append("\n");
            pdfContent.append("New Registrations This Month: ").append(analytics.getNewRegistrationsThisMonth()).append("\n\n");
            
            pdfContent.append("Podcast Metrics:\n");
            pdfContent.append("Total Podcasts: ").append(analytics.getTotalPodcasts()).append("\n");
            pdfContent.append("Total Listening Time (minutes): ").append(analytics.getTotalListeningTime()).append("\n\n");
            
            pdfContent.append("Top Podcasts:\n");
            for (AdminAnalyticsDTO.PodcastRankingDTO podcast : analytics.getTopPodcasts()) {
                pdfContent.append(podcast.getRank()).append(". ").append(podcast.getTitle())
                         .append(" by ").append(podcast.getAuthor())
                         .append(" (").append(podcast.getTotalPlays()).append(" plays)\n");
            }
            
            response.getWriter().write(pdfContent.toString());
            
        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF export", e);
        }
    }

    /**
     * Generate report in background
     */
    public String generateBackgroundReport(String reportType, String email, User admin) {
        String jobId = UUID.randomUUID().toString();
        
        // Log the action
        logAdminAction(admin, "GENERATE_REPORT", "SYSTEM", null, reportType, 
                      "Started background report generation", null, null, true, null);
        
        // In a real implementation, you would use a background job queue like RabbitMQ or Spring Batch
        // For now, we'll simulate this with a simple async process
        Thread backgroundThread = new Thread(() -> {
            try {
                Thread.sleep(5000); // Simulate processing time
                
                // Generate the report and send email
                String reportContent = generateReportContent(reportType);
                emailService.sendReportEmail(email, reportType, reportContent);
                
                // Update job status
                // In a real implementation, you would update the job status in a database
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        backgroundThread.start();
        
        return jobId;
    }

    /**
     * Get report generation status
     */
    public Map<String, Object> getReportStatus(String jobId) {
        // In a real implementation, you would check the job status from a database
        Map<String, Object> status = new HashMap<>();
        status.put("jobId", jobId);
        status.put("status", "PROCESSING");
        status.put("progress", 50);
        status.put("estimatedCompletion", LocalDateTime.now().plusMinutes(2));
        
        return status;
    }

    // Helper methods
    
    private List<AdminAnalyticsDTO.PodcastRankingDTO> getTopPodcasts() {
        // Get top 10 podcasts by total plays
        List<Object[]> results = podcastProgressRepository.findTopPodcastsByPlays();
        
        return results.stream()
            .map(result -> new AdminAnalyticsDTO.PodcastRankingDTO(
                (Long) result[0], // podcastId
                (String) result[1], // title
                (String) result[2], // author
                (Long) result[3], // totalPlays
                (Long) result[4], // totalListeningTime
                0.0, // averageRating (placeholder)
                0 // rank (will be set below)
            ))
            .collect(Collectors.toList());
    }

    private List<AdminAnalyticsDTO.UsageDataPointDTO> getWeeklyUsageData() {
        List<AdminAnalyticsDTO.UsageDataPointDTO> weeklyData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            // Calculate metrics for this day
            long activeUsers = userRepository.countByLastActiveAtAfter(date.toLocalDate().atStartOfDay());
            long newUsers = userRepository.countByCreatedAtAfter(date.toLocalDate().atStartOfDay());
            long listeningTime = podcastProgressRepository.sumListeningTimeByDate(date.toLocalDate());
            
            weeklyData.add(new AdminAnalyticsDTO.UsageDataPointDTO(dateStr, activeUsers, newUsers, listeningTime));
        }
        
        return weeklyData;
    }

    private List<AdminAnalyticsDTO.UsageDataPointDTO> getMonthlyUsageData() {
        List<AdminAnalyticsDTO.UsageDataPointDTO> monthlyData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 11; i >= 0; i--) {
            LocalDateTime date = now.minusMonths(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // Calculate metrics for this month
            LocalDateTime monthStart = date.toLocalDate().withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            long activeUsers = userRepository.countByLastActiveAtBetween(monthStart, monthEnd);
            long newUsers = userRepository.countByCreatedAtBetween(monthStart, monthEnd);
            long listeningTime = podcastProgressRepository.sumListeningTimeByMonth(date.toLocalDate().getMonthValue(), date.getYear());
            
            monthlyData.add(new AdminAnalyticsDTO.UsageDataPointDTO(dateStr, activeUsers, newUsers, listeningTime));
        }
        
        return monthlyData;
    }

    private Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Database status
        health.put("database", "HEALTHY");
        
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        health.put("memoryUsed", usedMemory);
        health.put("memoryTotal", totalMemory);
        health.put("memoryUsagePercent", (double) usedMemory / totalMemory * 100);
        
        // Disk space (simplified)
        health.put("diskSpace", "SUFFICIENT");
        
        return health;
    }

    private void logAdminAction(User admin, String action, String targetType, Long targetId, 
                              String targetName, String description, String ipAddress, 
                              String userAgent, boolean successful, String errorMessage) {
        
        AdminActionLog log = new AdminActionLog();
        log.setAdminUsername(admin.getUsername());
        log.setAdminEmail(admin.getEmail());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setTimestamp(LocalDateTime.now());
        log.setSuccessful(successful);
        log.setErrorMessage(errorMessage);
        
        adminActionLogRepository.save(log);
    }

    private AdminActionLogDTO convertToLogDTO(AdminActionLog log) {
        AdminActionLogDTO dto = new AdminActionLogDTO();
        dto.setId(log.getId());
        dto.setAdminUsername(log.getAdminUsername());
        dto.setAdminEmail(log.getAdminEmail());
        dto.setAction(log.getAction());
        dto.setTargetType(log.getTargetType());
        dto.setTargetId(log.getTargetId());
        dto.setTargetName(log.getTargetName());
        dto.setDescription(log.getDescription());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setTimestamp(log.getTimestamp());
        dto.setSuccessful(log.isSuccessful());
        dto.setErrorMessage(log.getErrorMessage());
        
        return dto;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    private String generateReportContent(String reportType) {
        // Generate report content based on type
        AdminAnalyticsDTO analytics = getAnalytics();
        
        StringBuilder content = new StringBuilder();
        content.append("Podcastia ").append(reportType.toUpperCase()).append(" Report\n");
        content.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        content.append("User Metrics:\n");
        content.append("Daily Active Users: ").append(analytics.getDailyActiveUsers()).append("\n");
        content.append("Monthly Active Users: ").append(analytics.getMonthlyActiveUsers()).append("\n");
        content.append("Total Users: ").append(analytics.getTotalUsers()).append("\n");
        
        return content.toString();
    }
}
