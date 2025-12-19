package com.pacny.highlight.client;

import com.pacny.highlight.entity.Chat;

import java.util.List;

public interface ChzzkClient {
    /**
     * 비디오 ID에 대해 페이징을 반복하여 모든 채팅 메시지를 가져옵니다.
     * 구현체는 속도 제한(rate limiting)과 재시도(retry) 로직을 처리해야 합니다.
     */
    List<Chat> fetchAllChats(String videoId);
}
