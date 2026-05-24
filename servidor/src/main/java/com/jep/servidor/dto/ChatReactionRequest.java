package com.jep.servidor.dto;

import java.time.Instant;

/**
 * Payload STOMP para adicionar, atualizar ou remover uma reação a uma mensagem.
 *
 * <p>Enviado via {@code /app/chat.reaction}. Se o emoji já estiver registado
 * pelo utilizador, a reação é removida (toggle); caso contrário, é adicionada.
 * O {@code clientEventAt} permite ordenar eventos em caso de concorrência.
 *
 * @param emoji         emoji da reação (ex: {@code "😂"}, {@code "❤️"}).
 * @param clientEventAt timestamp do evento no lado do cliente, para desambiguação.
 */
public record ChatReactionRequest(
    String emoji,
    Instant clientEventAt
) {
}