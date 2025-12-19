package com.pacny.highlight.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChzzkResponse {
    private Integer code;
    private String message;
    private Content content;

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        @JsonProperty("nextPlayerMessageTime")
        private Long nextPlayerMessageTime;
        @JsonProperty("previousVideoChats")
        private List<Object> previousVideoChats;
        @JsonProperty("videoChats")
        private List<VideoChat> videoChats;

        public Long getNextPlayerMessageTime() { return nextPlayerMessageTime; }
        public void setNextPlayerMessageTime(Long nextPlayerMessageTime) { this.nextPlayerMessageTime = nextPlayerMessageTime; }
        public List<Object> getPreviousVideoChats() { return previousVideoChats; }
        public void setPreviousVideoChats(List<Object> previousVideoChats) { this.previousVideoChats = previousVideoChats; }
        public List<VideoChat> getVideoChats() { return videoChats; }
        public void setVideoChats(List<VideoChat> videoChats) { this.videoChats = videoChats; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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

        public String getChatChannelId() { return chatChannelId; }
        public void setChatChannelId(String chatChannelId) { this.chatChannelId = chatChannelId; }
        public Long getMessageTime() { return messageTime; }
        public void setMessageTime(Long messageTime) { this.messageTime = messageTime; }
        public String getUserIdHash() { return userIdHash; }
        public void setUserIdHash(String userIdHash) { this.userIdHash = userIdHash; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getExtras() { return extras; }
        public void setExtras(String extras) { this.extras = extras; }
        public Integer getMessageTypeCode() { return messageTypeCode; }
        public void setMessageTypeCode(Integer messageTypeCode) { this.messageTypeCode = messageTypeCode; }
        public String getMessageStatusType() { return messageStatusType; }
        public void setMessageStatusType(String messageStatusType) { this.messageStatusType = messageStatusType; }
        public String getProfile() { return profile; }
        public void setProfile(String profile) { this.profile = profile; }
        public Long getPlayerMessageTime() { return playerMessageTime; }
        public void setPlayerMessageTime(Long playerMessageTime) { this.playerMessageTime = playerMessageTime; }
    }
}
