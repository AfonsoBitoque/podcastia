package com.jep.servidor.dto;

/**
 * Payload STOMP/REST para envio de uma mensagem de chat privada.
 *
 * <p>Enviado pelo frontend via WebSocket ({@code /app/chat.send}) ou REST.
 * O campo {@code metadata} é opcional e permite anexar uma referência
 * a um podcast ou episódio.
 *
 * @param recipientId ID do utilizador destinatário da mensagem.
 * @param content     Texto da mensagem (não pode ser nulo ou vazio).
 * @param metadata    Anexo opcional ({@link ChatMessageAttachmentRequest}).
 */
public record ChatMessageRequest(
    Long recipientId,
    String content,
    ChatMessageAttachmentRequest metadata
) {
}