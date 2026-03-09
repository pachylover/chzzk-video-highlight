package com.pachy.highlight.service;

import java.util.List;

import com.pachy.highlight.dto.HighlightResponse;

public interface HighlightService {
    /**
     * schedule creation of highlights for a video
     * @param videoId id of the video
     * @param highlightType optional category (e.g. "AUTO", "MANUAL"); null/empty will default to "AUTO"
     * @return task id (not currently used)
     */
    Long createHighlight(String videoId, String highlightType);
    List<HighlightResponse> getHighlight(String videoId);
}
