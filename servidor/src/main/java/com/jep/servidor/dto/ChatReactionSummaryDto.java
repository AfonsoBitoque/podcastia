package com.jep.servidor.dto;

import java.util.List;

/**
 * DTO de reação agregada por emoji numa mensagem de chat.
 *
 * <p>Agrupa todos os utilizadores que reagiram com o mesmo emoji,
 * incluindo uma flag de conveniência indicando se o utilizador atual
 * (“me”) já reagiu com este emoji.
 *
 * @param emoji          emoji da reação.
 * @param count          número total de utilizadores que reagiram com este emoji.
 * @param reactorUserIds lista dos IDs de utilizadores que reagiram.
 * @param reactedByMe    {@code true} se o utilizador autenticado está na lista.
 */
public record ChatReactionSummaryDto(
    String emoji,
    long count,
    List<Long> reactorUserIds,
    boolean reactedByMe
) {
}