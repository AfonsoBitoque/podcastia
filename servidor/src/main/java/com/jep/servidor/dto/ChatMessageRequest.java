package com.jep.servidor.dto;

/**
 * Pedido de envio de mensagem.
 */
public record ChatMessageRequest(
    Long recipientId,
    String content,
    ChatMessageAttachmentRequest metadata
) {
}