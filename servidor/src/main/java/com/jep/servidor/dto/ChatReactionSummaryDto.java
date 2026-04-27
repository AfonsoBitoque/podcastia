package com.jep.servidor.dto;

import java.util.List;

/**
 * Reação agregada por emoji.
 */
public record ChatReactionSummaryDto(
    String emoji,
    long count,
    List<Long> reactorUserIds,
    boolean reactedByMe
) {
}