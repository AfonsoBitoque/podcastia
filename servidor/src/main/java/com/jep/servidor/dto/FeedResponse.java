package com.jep.servidor.dto;

import com.jep.servidor.model.Podcast;
import java.util.List;

/**
 * Resposta completa do feed filtrado da homepage.
 *
 * <p>Devolvida pelo {@link com.jep.servidor.controller.FeedController} em resposta
 * a {@code GET /api/home}. Combina a lista de podcasts da página atual com os
 * metadados de paginação e contexto de categoria.
 *
 * @param data lista de {@link Podcast} da página atual (pode ser vazia).
 * @param meta metadados de paginação e contexto ({@link FeedMeta}).
 */
public record FeedResponse(
    List<Podcast> data,
    FeedMeta meta
) {
}
