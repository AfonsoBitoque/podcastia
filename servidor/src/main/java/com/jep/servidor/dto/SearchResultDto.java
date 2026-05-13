package com.jep.servidor.dto;

import com.jep.servidor.model.PodcastTag;
import java.util.List;

public class SearchResultDto {
  private Long id;
  private String type; // "USER" or "PODCAST"
  private String title;
  private String subtitle;
  private String imageUrl;
  private String extraInfo; // Ex: tag do user
  private Integer duracao;
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
