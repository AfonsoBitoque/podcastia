package com.jep.servidor.dto;

import java.util.List;

/**
 * Resposta paginada do histórico de conversa entre dois utilizadores.
 *
 * <p>Usa paginação baseada em cursor ({@code nextCursor}) em vez de página/offset,
 * garantindo consistência mesmo quando novas mensagens são inseridas durante a navegação.
 *
 * @param messages   lista de mensagens desta página (do mais recente para o mais antigo).
 * @param nextCursor cursor opaco para obter a próxima página; {@code null} se não houver mais.
 * @param hasMore    {@code true} se existirem mais mensagens anteriores.
 */
public record ChatMessageHistoryResponse(
    List<ChatMessageDto> messages,
    String nextCursor,
    boolean hasMore
) {
}