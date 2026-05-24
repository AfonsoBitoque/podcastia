package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Objeto embutido ({@code @Embeddable}) com metadados de anexo de uma mensagem de chat.
 *
 * <p>Persiste nas colunas {@code attachment_type}, {@code attachment_podcast_id}
 * e {@code attachment_episode_id} da tabela {@code chat_messages}.
 * Todos os campos são opcionais — são {@code null} quando a mensagem não
 * tem anexo.
 *
 * @see ChatMessage
 * @see com.jep.servidor.dto.ChatMessageAttachmentRequest
 */
@Embeddable
public class ChatMessageMetadata {

  @Column(name = "attachment_type")
  private String type;

  @Column(name = "attachment_podcast_id")
  private Long podcastId;

  @Column(name = "attachment_episode_id")
  private Long episodeId;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Long getPodcastId() {
    return podcastId;
  }

  public void setPodcastId(Long podcastId) {
    this.podcastId = podcastId;
  }

  public Long getEpisodeId() {
    return episodeId;
  }

  public void setEpisodeId(Long episodeId) {
    this.episodeId = episodeId;
  }
}