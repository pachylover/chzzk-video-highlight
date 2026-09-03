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

    /** 채널(스트리머) 정보. 하이라이트에 채널을 기록하기 위해 다른 패키지에서도 읽는다. */
    @Data
    public static class Channel {
        private String channelId;
        private String channelName;
        private String channelImageUrl;
        private boolean verifiedMark;
    }

    @Data
    public static class VideoIncr {
        private int likeCount;
        private int commentCount;
    }
}
