package com.pacny.highlight.client;

import org.springframework.stereotype.Component;

/**
 * 로컬 개발용 간단한 Mock 구현체입니다. 실제 Gemini(OpenAI) API 호출 구현체로 교체하세요.
 */
@Component
public class MockGeminiClient implements GeminiClient {
    @Override
    public SummaryResult summarize(String chatText) {
        // 로컬/개발용 매우 단순한 요약 예시
        String title = "채팅 폭발 순간";
        String summary = "이 구간에서 채팅이 집중적으로 발생했습니다. (샘플 요약)";
        return new SummaryResult(title, summary);
    }
}
