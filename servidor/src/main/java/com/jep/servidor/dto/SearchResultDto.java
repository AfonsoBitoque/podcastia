package com.jep.servidor.dto;

public class SearchResultDto {
    private Long id;
    private String type; // "USER" or "PODCAST"
    private String title;
    private String subtitle;
    private String imageUrl;
    private String extraInfo; // Ex: tag do user

    public SearchResultDto() {
    }

    public SearchResultDto(Long id, String type, String title, String subtitle, String imageUrl, String extraInfo) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.extraInfo = extraInfo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
}
