package com.jep.servidor.service.impl;

import com.jep.servidor.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementação stub do serviço de notificações push.
 *
 * <p>Imprime as notificações no {@code stdout} de forma assíncrona.
 * Em produção, substituir por integração com Firebase Cloud Messaging,
 * APNs ou outro serviço de push.
 *
 * @see com.jep.servidor.service.NotificationService
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /**
     * Envia (simulado) uma notificação de forma assíncrona.
     *
     * @param to      identificador do destinatário.
     * @param message conteúdo da notificação.
     */
    @Override
    @Async
    public void sendNotification(String to, String message) {
        // For now, we'll just print the notification to the console.
        // A real implementation would use a service like Firebase Cloud Messaging, an email sender, etc.
        System.out.println("Sending notification to " + to + ": " + message);
    }
}
