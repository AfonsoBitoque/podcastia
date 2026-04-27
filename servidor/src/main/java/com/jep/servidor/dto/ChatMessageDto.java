package com.jep.servidor.dto;

import java.time.Instant;
import java.util.List;

/**
 * Representação transportável de uma mensagem privada.
 */
public record ChatMessageDto(
    Long id,
    Long senderId,
    Long recipientId,
    String content,
    String status,
    Instant createdAt,
    Instant deliveredAt,
    Instant readAt,
    ChatMessageAttachmentRequest metadata,
    List<ChatReactionSummaryDto> reactions
) {
}