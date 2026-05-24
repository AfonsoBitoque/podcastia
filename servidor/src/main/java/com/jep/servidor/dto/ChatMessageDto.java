package com.jep.servidor.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO de transferência de uma mensagem de chat privado.
 *
 * <p>Representa a visão serializável de uma mensagem, incluindo o seu estado
 * ({@code SENT}, {@code DELIVERED}, {@code READ}), timestamps de entrega e
 * leitura, anexo opcional de catálogo e lista de reações agrupadas por emoji.
 *
 * @param id          ID único da mensagem.
 * @param senderId    ID do utilizador que enviou a mensagem.
 * @param recipientId ID do utilizador destinatário.
 * @param content     Conteúdo de texto da mensagem.
 * @param status      Estado atual: {@code SENT}, {@code DELIVERED} ou {@code READ}.
 * @param createdAt   Instant em que a mensagem foi criada.
 * @param deliveredAt Instant em que a mensagem foi entregue (ou {@code null}).
 * @param readAt      Instant em que a mensagem foi lida (ou {@code null}).
 * @param metadata    Anexo opcional de podcast/episódio ({@link ChatMessageAttachmentRequest}).
 * @param reactions   Lista de reações agrupadas por emoji ({@link ChatReactionSummaryDto}).
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