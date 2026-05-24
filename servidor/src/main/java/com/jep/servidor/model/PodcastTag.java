package com.jep.servidor.model;

/**
 * Enum de categorias disponíveis para classificação de podcasts.
 *
 * <p>Usado em:
 * <ul>
 *   <li>{@link Podcast#getTags()} — cada podcast pode ter várias categorias.</li>
 *   <li>{@link User} — pontos de afinidade por categoria ({@code pontosDesporto}, etc.).</li>
 *   <li>{@link com.jep.servidor.service.RecommendationService} — personalização do feed.</li>
 *   <li>{@link com.jep.servidor.service.DailyPlaylistService} — seleção de podcasts para playlist diária.</li>
 * </ul>
 *
 * <p>Armazenado como {@code STRING} nas tabelas {@code podcast_tags} e {@code user_topics}.
 */
public enum PodcastTag {
  /** Podcasts sobre desporto e atividade física. */
  DESPORTO,
  /** Podcasts sobre política e atualidade. */
  POLITICA,
  /** Podcasts sobre finanças, economia e investimento. */
  FINANCAS,
  /** Podcasts de conteúdo geral sem categoria específica. */
  GERAL
}
