package com.jep.servidor.dto;

import java.util.List;

/**
 * Resposta paginada do histórico de conversa.
 */
public record ChatMessageHistoryResponse(
    List<ChatMessageDto> messages,
    String nextCursor,
    boolean hasMore
) {
}