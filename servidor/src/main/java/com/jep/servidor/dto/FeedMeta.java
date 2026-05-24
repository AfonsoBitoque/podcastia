package com.jep.servidor.dto;

/**
 * Metadados de paginação e contexto da resposta do feed da homepage.
 *
 * <p>Incluido em {@link FeedResponse#meta()} para informar o frontend sobre
 * o estado da paginação e disponibilidade de conteúdo na categoria pedida.
 *
 * @param page               número da página atual (0-indexed).
 * @param size               número de resultados por página.
 * @param total              total de podcasts que correspondem ao filtro.
 * @param hasMore            {@code true} se existirem mais páginas.
 * @param categoryHasContent {@code true} se a categoria pedida tem conteúdo disponível;
 *                           {@code null} se não foi especificada categoria.
 * @param category           nome da categoria filtrada, ou {@code null} se não aplicada.
 */
public record FeedMeta(
    int page,
    int size,
    long total,
    boolean hasMore,
    Boolean categoryHasContent,
    String category
) {
}
