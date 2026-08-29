package com.pachy.highlight.dto.progress;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 하이라이트 생성 / 채팅 재수집의 진행 상황 이벤트.
 * SSE 로 클라이언트에 그대로 직렬화되어 전달된다.
 */
@Getter
@Builder
public class ProgressEvent {

    public enum Phase {
        /** 작업 시작 */
        START,
        /** 치지직에서 채팅 수집 중 */
        FETCH_CHATS,
        /** 수집한 채팅 DB 저장 중 */
        SAVE_CHATS,
        /** 채팅 분석 및 하이라이트 추출 중 */
        ANALYZE,
        /** 정상 완료 */
        DONE,
        /** 실패 */
        ERROR
    }

    private String videoId;
    private Phase phase;
    private String message;
    /** 0 ~ 100 */
    private int percent;
    /** 현재까지 수집/저장된 채팅 수 (알 수 없으면 null) */
    private Integer chatCount;
    /** 생성된 하이라이트 수 (완료 시에만) */
    private Integer highlightCount;
    private Instant at;

    @JsonIgnore
    public boolean isTerminal() {
        return phase == Phase.DONE || phase == Phase.ERROR;
    }

    public static ProgressEvent of(String videoId, Phase phase, String message, int percent) {
        return ProgressEvent.builder()
                .videoId(videoId)
                .phase(phase)
                .message(message)
                .percent(percent)
                .at(Instant.now())
                .build();
    }
}
