package com.pachy.highlight.dto;


import lombok.Data;

@Data
public class ChzzkVideoResponse {
    private String videoNo;
    private String videoId;
    private String videoTitle;
    private String videoType;
    private String publishDate; 
    private String thumbnailImageUrl;
    private String trailerUrl;
    private Integer duration;
    private Long readCount;
    private String publishDateAt;
    private String categoryType;
    private String videoCategory;
    private String videoCategoryValue;
    private String exposureSection;
    
    private Channel channel;
    private VideoIncr videoIncr;
    
    // 유료 콘텐츠나 성인 제한 관련 필드
    private boolean adult;
    private boolean clipVideo;
    private boolean paidPromotion;
}

@Data
class Channel {
    private String channelId;
    private String channelName;
    private String channelImageUrl;
    private boolean verifiedMark;
}

@Data
class VideoIncr {
    private int likeCount;
    private int commentCount;
}