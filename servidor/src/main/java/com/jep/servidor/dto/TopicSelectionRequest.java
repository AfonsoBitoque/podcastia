package com.jep.servidor.dto;

import java.util.List;

public class TopicSelectionRequest {
  private List<String> topicIds;

  public List<String> getTopicIds() {
    return topicIds;
  }

  public void setTopicIds(List<String> topicIds) {
    this.topicIds = topicIds;
  }
}
