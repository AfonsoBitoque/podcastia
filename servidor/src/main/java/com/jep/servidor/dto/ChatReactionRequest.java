package com.jep.servidor.dto;

import java.time.Instant;

/**
 * Pedido para adicionar, atualizar ou remover uma reação.
 */
public record ChatReactionRequest(
    String emoji,
    Instant clientEventAt
) {
}