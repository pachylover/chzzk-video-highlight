package com.pacny.highlight.service;

import com.pacny.highlight.dto.HighlightResponse;

import java.util.UUID;

public interface HighlightService {
    UUID createHighlight(String url, String callbackUrl);

    HighlightResponse getHighlight(UUID id);
}
