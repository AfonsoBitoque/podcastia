package com.jep.servidor.dto;

import java.util.List;

/**
 * Resposta de uma operação de reação.
 */
public record ChatReactionUpdateResponse(
    Long messageId,
    String action,
    ChatReactionSummaryDto reaction,
    List<ChatReactionSummaryDto> reactions
) {
}