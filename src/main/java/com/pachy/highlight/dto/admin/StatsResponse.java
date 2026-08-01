package com.pachy.highlight.dto.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class StatsResponse {

    private long totalVideos;      // 하이라이트가 생성된 distinct 영상 수
    private long totalHighlights;  // 총 하이라이트 수
    private long totalChats;       // 총 수집 채팅 수
    private long highlightsToday;  // 오늘 생성된 하이라이트 수
    private long highlightsLast7Days; // 최근 7일 생성 하이라이트 수

    private List<DailyCount> dailyTrend;  // 일자별 생성 추이
    private List<TopVideo> topVideos;     // 상위 영상

    @Getter
    @Setter
    @Builder
    public static class DailyCount {
        private String date;   // YYYY-MM-DD
        private long count;
    }

    @Getter
    @Setter
    @Builder
    public static class TopVideo {
        private String videoId;
        private long highlightCount;
        private long maxChatCount;
    }
}
