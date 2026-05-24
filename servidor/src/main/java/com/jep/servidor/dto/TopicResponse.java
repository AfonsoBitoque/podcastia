package com.jep.servidor.dto;

/**
 * DTO de resposta de um tópico/tag disponível na plataforma.
 *
 * <p>Devolvido pelo endpoint {@code GET /api/topics} em
 * {@link com.jep.servidor.controller.TopicController}.
 * O {@code id} corresponde ao nome do enum {@link com.jep.servidor.model.PodcastTag}
 * (ex: {@code "DESPORTO"}) e o {@code label} é o texto human-readable
 * em português (ex: {@code "Desporto"}).
 */
public class TopicResponse {
  /** Identificador do tópico (nome do enum {@link com.jep.servidor.model.PodcastTag}). */
  private String id;
  /** Label human-readable em português (ex: {@code "Desporto"}). */
  private String label;

  public TopicResponse() {
  }

  public TopicResponse(String id, String label) {
    this.id = id;
    this.label = label;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
}
