package com.jep.servidor.dto;

import com.jep.servidor.model.Podcast;
import java.util.List;

/**
 * Resposta do feed filtrado da homepage.
 */
public record FeedResponse(
    List<Podcast> data,
    FeedMeta meta
) {
}
