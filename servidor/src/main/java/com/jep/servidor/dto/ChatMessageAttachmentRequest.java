package com.jep.servidor.dto;

/**
 * Payload opcional de anexo de catálogo numa mensagem de chat.
 *
 * <p>Permite partilhar referências a podcasts ou episódios numa mensagem privada.
 * Enviado como sub-objeto {@code metadata} de {@link ChatMessageRequest}.
 *
 * @param type      tipo de anexo (ex: {@code "PODCAST"}, {@code "EPISODE"}).
 * @param podcastId ID do podcast referenciado (opcional).
 * @param episodeId ID do episódio referenciado (opcional).
 */
public record ChatMessageAttachmentRequest(
    String type,
    Long podcastId,
    Long episodeId
) {
}