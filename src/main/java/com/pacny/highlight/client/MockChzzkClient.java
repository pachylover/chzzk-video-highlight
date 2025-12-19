package com.pacny.highlight.client;

import com.pacny.highlight.entity.Chat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 실제 구현이 제공될 때까지 로컬 개발/테스트에 사용하는 간단한 Mock 클라이언트입니다.
 */
@Component
public class MockChzzkClient implements ChzzkClient {

    @Override
    public List<Chat> fetchAllChats(String videoId) {
        // 기본적으로 빈 리스트를 반환합니다; 테스트에서 대체하거나 모의 구현을 주입하세요.
        List<Chat> list = new ArrayList<>();
        // 수동 테스트용 샘플 데이터 예시(주석 처리)
        // list.add(Chat.builder().videoId(videoId).ts(Instant.now()).username("user").message("hello").build());
        return list;
    }
}
