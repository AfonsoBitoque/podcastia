package com.jep.servidor.dto;

/**
 * Payload opcional com anexo de catálogo.
 */
public record ChatMessageAttachmentRequest(
    String type,
    Long podcastId,
    Long episodeId
) {
}