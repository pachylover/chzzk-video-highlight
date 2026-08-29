package com.pachy.highlight.service;

import java.util.List;

import com.pachy.highlight.dto.HighlightResponse;

public interface HighlightService {
    /**
     * 하이라이트 생성을 백그라운드로 시작한다.
     * @param videoId id of the video
     * @param highlightType optional category (e.g. "AUTO", "MANUAL"); null/empty will default to "AUTO"
     * @return 작업이 시작되었으면 true, 이미 같은 영상의 작업이 진행 중이면 false
     */
    boolean createHighlight(String videoId, String highlightType);

    /**
     * 채팅만 다시 수집한다. 채팅 보관 기간이 지나 사라진 영상을 복구할 때 사용한다.
     * @return 작업이 시작되었으면 true, 이미 같은 영상의 작업이 진행 중이면 false
     */
    boolean reloadChats(String videoId);

    List<HighlightResponse> getHighlight(String videoId);
}
