package com.jep.servidor.dto;

import java.util.List;

/**
 * DTO de pedido de seleção de tópicos de interesse.
 *
 * <p>Usado pelo endpoint {@code PUT /api/users/{id}/topics} em
 * {@link com.jep.servidor.controller.TopicController}.
 * Os {@code topicIds} devem ser valores válidos de
 * {@link com.jep.servidor.model.PodcastTag} (case-insensitive).
 */
public class TopicSelectionRequest {
  /** Lista de IDs de tópicos selecionados (mínimo 3 requerido). */
  private List<String> topicIds;

  public List<String> getTopicIds() {
    return topicIds;
  }

  public void setTopicIds(List<String> topicIds) {
    this.topicIds = topicIds;
  }
}
