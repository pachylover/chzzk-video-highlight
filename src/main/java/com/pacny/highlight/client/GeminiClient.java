package com.pacny.highlight.client;

public interface GeminiClient {
    /**
     * 채팅 텍스트를 요약하여 제목(title)과 요약(summary)을 반환합니다. (출력 형식은 추후 확정)
     */
    SummaryResult summarize(String chatText);

    class SummaryResult {
        public final String title;
        public final String summary;

        public SummaryResult(String title, String summary) {
            this.title = title;
            this.summary = summary;
        }
    }
}
