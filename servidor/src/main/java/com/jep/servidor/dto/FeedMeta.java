package com.jep.servidor.dto;

/**
 * Metadados de paginacao e contexto do feed.
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
