package com.jep.servidor.service;

import org.springframework.stereotype.Service;

/**
 * Serviço de envio de emails da plataforma Podcastia.
 *
 * <p><b>Implementação atual:</b> stub que escreve o conteúdo no {@code stdout}.
 * Em produção deve ser substituído por uma integração real com
 * {@code JavaMailSender} (SMTP) ou um serviço externo como SendGrid.
 *
 * @see com.jep.servidor.service.AdminService#generateBackgroundReport
 */
@Service
public class EmailService {

    /**
     * Envia (ou simula o envio de) um email com um relatório de analytics.
     *
     * <p>Chamado por {@link com.jep.servidor.service.AdminService#generateBackgroundReport}
     * após a geração assíncrona do relatório.
     *
     * @param toEmail       endereço de email do destinatário (admin).
     * @param reportType    tipo de relatório (ex: {@code "WEEKLY"}).
     * @param reportContent conteúdo do relatório em formato texto.
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
     * Envia (ou simula o envio de) um email de notificação genérico.
     *
     * @param toEmail endereço de email do destinatário.
     * @param subject assunto do email.
     * @param message corpo da mensagem.
     */
    public void sendNotificationEmail(String toEmail, String subject, String message) {
        System.out.println("=== NOTIFICATION EMAIL ===");
        System.out.println("To: " + toEmail);
        System.out.println("Subject: " + subject);
        System.out.println("Message: " + message);
        System.out.println("==========================");
    }
}
