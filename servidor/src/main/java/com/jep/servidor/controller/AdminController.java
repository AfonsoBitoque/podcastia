package com.jep.servidor.controller;

import com.jep.servidor.model.User;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.dto.AdminAnalyticsDTO;
import com.jep.servidor.dto.AdminPodcastManagementDTO;
import com.jep.servidor.dto.AdminActionLogDTO;
import com.jep.servidor.service.AdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Admin Controller for managing podcasts, users, and analytics
 * Only accessible by users with USER_ADMIN role
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('USER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Get admin dashboard analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<AdminAnalyticsDTO> getAnalytics() {
        AdminAnalyticsDTO analytics = adminService.getAnalytics();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get all podcasts for admin management
     */
    @GetMapping("/podcasts")
    public ResponseEntity<List<AdminPodcastManagementDTO>> getAllPodcastsForManagement() {
        List<AdminPodcastManagementDTO> podcasts = adminService.getAllPodcastsForManagement();
        return ResponseEntity.ok(podcasts);
    }

    /**
     * Update podcast metadata
     */
    @PutMapping("/podcasts/{podcastId}")
    public ResponseEntity<Podcast> updatePodcastMetadata(
            @PathVariable Long podcastId,
            @RequestBody Podcast podcastData,
            @AuthenticationPrincipal User admin) {
        
        Podcast updatedPodcast = adminService.updatePodcastMetadata(podcastId, podcastData, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Mark podcast as explicit content
     */
    @PutMapping("/podcasts/{podcastId}/explicit")
    public ResponseEntity<Podcast> markAsExplicit(
            @PathVariable Long podcastId,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal User admin) {
        
        boolean isExplicit = request.getOrDefault("explicit", false);
        Podcast updatedPodcast = adminService.markAsExplicit(podcastId, isExplicit, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Hide/Unhide podcast
     */
    @PutMapping("/podcasts/{podcastId}/hidden")
    public ResponseEntity<Podcast> togglePodcastVisibility(
            @PathVariable Long podcastId,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal User admin) {
        
        boolean hidden = request.getOrDefault("hidden", false);
        Podcast updatedPodcast = adminService.togglePodcastVisibility(podcastId, hidden, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Feature/Unfeature podcast
     */
    @PutMapping("/podcasts/{podcastId}/featured")
    public ResponseEntity<Podcast> togglePodcastFeatured(
            @PathVariable Long podcastId,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal User admin) {
        
        boolean featured = request.getOrDefault("featured", false);
        Podcast updatedPodcast = adminService.togglePodcastFeatured(podcastId, featured, admin);
        return ResponseEntity.ok(updatedPodcast);
    }

    /**
     * Delete podcast with double confirmation
     */
    @DeleteMapping("/podcasts/{podcastId}/confirm")
    public ResponseEntity<?> confirmPodcastDeletion(
            @PathVariable Long podcastId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {
        
        String confirmation = request.get("confirmation");
        String adminPassword = request.get("adminPassword");
        
        boolean success = adminService.confirmPodcastDeletion(podcastId, confirmation, adminPassword, admin);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Podcast deleted successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid confirmation or password"));
        }
    }

    /**
     * Get admin action logs
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AdminActionLogDTO>> getAdminLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        List<AdminActionLogDTO> logs = adminService.getAdminLogs(limit, offset);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get all users for admin management
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        // Remove password hashes from response
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    /**
     * Reset user password (admin action)
     */
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long userId,
            @AuthenticationPrincipal User admin) {
        
        String tempPassword = adminService.resetUserPassword(userId, admin);
        return ResponseEntity.ok(Map.of(
            "message", "Password reset successfully",
            "tempPassword", tempPassword
        ));
    }

    /**
     * Delete user with double confirmation
     */
    @DeleteMapping("/users/{userId}/confirm")
    public ResponseEntity<?> confirmUserDeletion(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {
        
        String confirmation = request.get("confirmation");
        String adminPassword = request.get("adminPassword");
        
        boolean success = adminService.confirmUserDeletion(userId, confirmation, adminPassword, admin);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid confirmation or password"));
        }
    }

    /**
     * Export analytics to CSV
     */
    @GetMapping("/export/csv")
    public void exportAnalyticsCsv(HttpServletResponse response, @AuthenticationPrincipal User admin) {
        adminService.exportAnalyticsCsv(response);
    }

    /**
     * Export analytics to PDF
     */
    @GetMapping("/export/pdf")
    public void exportAnalyticsPdf(HttpServletResponse response, @AuthenticationPrincipal User admin) {
        adminService.exportAnalyticsPdf(response);
    }

    /**
     * Generate report in background
     */
    @PostMapping("/reports/generate")
    public ResponseEntity<?> generateBackgroundReport(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {
        
        String reportType = request.getOrDefault("type", "analytics");
        String email = request.get("email");
        
        String jobId = adminService.generateBackgroundReport(reportType, email, admin);
        
        return ResponseEntity.ok(Map.of(
            "message", "Report generation started",
            "jobId", jobId
        ));
    }

    /**
     * Get report generation status
     */
    @GetMapping("/reports/{jobId}/status")
    public ResponseEntity<?> getReportStatus(@PathVariable String jobId) {
        Map<String, Object> status = adminService.getReportStatus(jobId);
        return ResponseEntity.ok(status);
    }
}
