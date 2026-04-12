package com.jep.servidor.service.impl;

import com.jep.servidor.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    @Async
    public void sendNotification(String to, String message) {
        // For now, we'll just print the notification to the console.
        // A real implementation would use a service like Firebase Cloud Messaging, an email sender, etc.
        System.out.println("Sending notification to " + to + ": " + message);
    }
}
