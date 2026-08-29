package com.pachy.highlight.client;

import java.util.List;
import java.util.function.IntConsumer;

import com.pachy.highlight.dto.ChzzkVideoResponse;
import com.pachy.highlight.entity.Chat;

public interface ChzzkClient {
    /**
     * 비디오 ID에 대해 페이징을 반복하여 모든 채팅 메시지를 가져옵니다.
     * 구현체는 속도 제한(rate limiting)과 재시도(retry) 로직을 처리해야 합니다.
     */
    default List<Chat> fetchAllChats(String videoId) {
        return fetchAllChats(videoId, count -> {});
    }

    /**
     * 채팅을 수집하면서 페이지를 한 번 읽을 때마다 누적 수집 건수를 콜백으로 알려줍니다.
     * 진행 상황 스트리밍(SSE)에 사용됩니다.
     */
    List<Chat> fetchAllChats(String videoId, IntConsumer onProgress);

    ChzzkVideoResponse fetchVideoInfo(String videoId);
}
