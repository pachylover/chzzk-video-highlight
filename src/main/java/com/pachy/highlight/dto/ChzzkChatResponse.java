package com.pachy.highlight.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChzzkChatResponse {
  @JsonProperty("nextPlayerMessageTime")
  private Long nextPlayerMessageTime;
  @JsonProperty("previousVideoChats")
  private List<Object> previousVideoChats;
  @JsonProperty("videoChats")
  private List<VideoChat> videoChats;

  public Long getNextPlayerMessageTime() {
    return nextPlayerMessageTime;
  }

  public void setNextPlayerMessageTime(Long nextPlayerMessageTime) {
    this.nextPlayerMessageTime = nextPlayerMessageTime;
  }

  public List<Object> getPreviousVideoChats() {
    return previousVideoChats;
  }

  public void setPreviousVideoChats(List<Object> previousVideoChats) {
    this.previousVideoChats = previousVideoChats;
  }

  public List<VideoChat> getVideoChats() {
    return videoChats;
  }

  public void setVideoChats(List<VideoChat> videoChats) {
    this.videoChats = videoChats;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @Getter
  @Setter
  public static class VideoChat {
    @JsonProperty("chatChannelId")
    private String chatChannelId;
    @JsonProperty("messageTime")
    private Long messageTime;
    @JsonProperty("userIdHash")
    private String userIdHash;
    @JsonProperty("content")
    private String content;
    @JsonProperty("extras")
    private String extras;
    @JsonProperty("messageTypeCode")
    private Integer messageTypeCode;
    @JsonProperty("messageStatusType")
    private String messageStatusType;
    @JsonProperty("profile")
    private String profile;
    @JsonProperty("playerMessageTime")
    private Long playerMessageTime;
  }
}
