package com.pachy.highlight.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 분당 채팅량 그래프 데이터. 채팅이 없는 분도 0 으로 채워 연속된 시계열을 만든다.
 */
@Getter
@Builder
public class ChatTimelineResponse {

    private String videoId;
    /** 해당 영상에 저장된 전체 채팅 수 (0 이면 채팅 다시 불러오기 대상) */
    private long totalChats;
    /** 가장 채팅이 많았던 분의 채팅 수 */
    private long peakCount;
    /** 진행 중인 수집 작업이 있는지 */
    private boolean processing;
    private List<Point> points;

    @Getter
    @Builder
    public static class Point {
        /** 영상 시작 기준 분 (0, 1, 2 ...) */
        private int minute;
        /** 해당 구간의 시작 초 — 다시보기 이동 링크(currentTime)에 그대로 쓴다 */
        private int seconds;
        /** 표시용 라벨 (HH:MM 또는 MM) */
        private String label;
        private long count;
    }
}
