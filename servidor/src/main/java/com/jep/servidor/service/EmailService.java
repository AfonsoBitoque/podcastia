package com.jep.servidor.service;

import org.springframework.stereotype.Service;

/**
 * Service for sending emails
 * This is a simplified implementation - in production you would use a proper email service
 */
@Service
public class EmailService {
    
    /**
     * Send report email to admin
     * In a real implementation, you would use Spring Mail or a service like SendGrid
     */
    public void sendReportEmail(String toEmail, String reportType, String reportContent) {
        // For now, we'll just log the email sending
        System.out.println("=== EMAIL SERVICE ===");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: Podcastia " + reportType + " Report");
        System.out.println("Content:");
        System.out.println(reportContent);
        System.out.println("==================");
        
        // In a real implementation:
        // 1. Configure Spring Mail with SMTP settings
        // 2. Create email template
        // 3. Send email using JavaMailSender
        // 4. Handle email delivery failures
    }
    
    /**
     * Send notification email
     */
    public void sendNotificationEmail(String toEmail, String subject, String message) {
        System.out.println("=== NOTIFICATION EMAIL ===");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: " + subject);
        System.out.println("Message: " + message);
        System.out.println("==========================");
    }
}
