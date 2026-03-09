package com.pachy.highlight.entity;

import com.pachy.highlight.dto.HighlightResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HighlightTest {
    @Test
    void toResponse_includesType() {
        Highlight h = Highlight.builder()
                .id(123L)
                .videoId("vid")
                .minute(60000L)
                .startTs(50000L)
                .endTs(70000L)
                .chatCount(10)
                .title("test")
                .summary("sum")
                .status("DONE")
                .highlightType("MANUAL")
                .build();

        HighlightResponse resp = h.toResponse();
        assertThat(resp).isNotNull();
        assertThat(resp.getTaskId()).isEqualTo("123");
        assertThat(resp.getHighlightType()).isEqualTo("MANUAL");
    }
}
