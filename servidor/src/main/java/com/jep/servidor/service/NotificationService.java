package com.jep.servidor.service;

/**
 * Interface do serviço de notificações push.
 *
 * <p>Define o contrato para o envio de notificações a utilizadores.
 * A implementação atual ({@link com.jep.servidor.service.impl.NotificationServiceImpl})
 * é um stub que escreve no {@code stdout}. Em produção deve ser substituída
 * por uma integração real (ex: Firebase Cloud Messaging, APNs, WebPush).
 */
public interface NotificationService {

    /**
     * Envia uma notificação a um utilizador.
     *
     * @param to      identificador do destinatário (tipicamente o ID do utilizador como {@code String}).
     * @param message conteúdo da notificação.
     */
    void sendNotification(String to, String message);
}
