package com.pachy.highlight.service;

import java.util.List;

import com.pachy.highlight.dto.HighlightResponse;

public interface HighlightService {
    Long createHighlight(String videoId);
    List<HighlightResponse> getHighlight(String videoId);
}
