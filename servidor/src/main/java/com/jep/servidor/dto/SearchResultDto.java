package com.jep.servidor.dto;

import com.jep.servidor.model.PodcastTag;
import java.util.List;

/**
 * DTO polimórfico de resultado de pesquisa unificada.
 *
 * <p>Devolvido pelo {@link com.jep.servidor.controller.SearchController} e
 * gerado pelo {@link com.jep.servidor.service.SearchService}. Representa tanto
 * um utilizador como um podcast num formato normalizado para o frontend.
 *
 * <p>O campo {@code type} discrimina o tipo de resultado:
 * <ul>
 *   <li>{@code "USER"} — representa um utilizador; {@code extraInfo} contém a tag.</li>
 *   <li>{@code "PODCAST"} — representa um podcast; {@code duracao} e {@code tags}
 *       estão preenchidos.</li>
 * </ul>
 */
public class SearchResultDto {
  /** ID da entidade (utilizador ou podcast). */
  private Long id;
  /** Tipo do resultado: {@code "USER"} ou {@code "PODCAST"}. */
  private String type;
  /** Título principal (username ou título do podcast). */
  private String title;
  /** Sub-título (email para USER, autor para PODCAST). */
  private String subtitle;
  /** URL da imagem de perfil ou capa do podcast. */
  private String imageUrl;
  /** Informação extra: tag do utilizador ({@code "#0042"}) ou {@code null} para podcasts. */
  private String extraInfo;
  /** Duração em segundos (apenas para PODCAST; {@code null} para USER). */
  private Integer duracao;
  /** Tags de categorização (apenas para PODCAST; {@code null} para USER). */
  private List<PodcastTag> tags;

  public SearchResultDto() {
  }

  public SearchResultDto(Long id, String type, String title, String subtitle, String imageUrl,
      String extraInfo) {
    this(id, type, title, subtitle, imageUrl, extraInfo, null, null);
  }

  public SearchResultDto(Long id, String type, String title, String subtitle, String imageUrl,
      String extraInfo, Integer duracao, List<PodcastTag> tags) {
    this.id = id;
    this.type = type;
    this.title = title;
    this.subtitle = subtitle;
    this.imageUrl = imageUrl;
    this.extraInfo = extraInfo;
    this.duracao = duracao;
    this.tags = tags;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getExtraInfo() {
    return extraInfo;
  }

  public void setExtraInfo(String extraInfo) {
    this.extraInfo = extraInfo;
  }

  public Integer getDuracao() {
    return duracao;
  }

  public void setDuracao(Integer duracao) {
    this.duracao = duracao;
  }

  public List<PodcastTag> getTags() {
    return tags;
  }

  public void setTags(List<PodcastTag> tags) {
    this.tags = tags;
  }
}
