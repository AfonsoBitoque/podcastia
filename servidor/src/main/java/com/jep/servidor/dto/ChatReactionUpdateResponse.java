package com.jep.servidor.dto;

import java.util.List;

/**
 * Resposta de uma operação de adição ou remoção de reação a uma mensagem de chat.
 *
 * <p>Enviada via WebSocket para ambos os participantes da conversa após
 * processar um {@link ChatReactionRequest}.
 *
 * @param messageId ID da mensagem afetada.
 * @param action    ação executada: {@code "ADDED"} ou {@code "REMOVED"}.
 * @param reaction  DTO da reação específica que foi modificada.
 * @param reactions lista completa e atualizada de todas as reações da mensagem.
 */
public record ChatReactionUpdateResponse(
    Long messageId,
    String action,
    ChatReactionSummaryDto reaction,
    List<ChatReactionSummaryDto> reactions
) {
}