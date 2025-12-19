package com.pacny.highlight.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class HighlightResponse {
    private String taskId;
    private String status;
    private String videoId;
    private Instant minute;
    private Instant start;
    private Instant end;
    private Integer chatCount;
    private String title;
    private String summary;
    private List<Object> chatSnippet;
}
