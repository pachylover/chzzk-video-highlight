package com.pachy.highlight.util;

import org.junit.jupiter.api.Test;

import com.pachy.highlight.util.VideoIdExtractor;

import static org.junit.jupiter.api.Assertions.*;

class VideoIdExtractorTest {

    @Test
    void extractFromQueryParam() {
        String id = VideoIdExtractor.extractVideoId("https://example.com/watch?v=abc123");
        assertEquals("abc123", id);
    }

    @Test
    void extractFromPathLastSegment() {
        String id = VideoIdExtractor.extractVideoId("https://example.com/videos/abc_xyz-9");
        assertEquals("abc_xyz-9", id);
    }

    @Test
    void extractFromChzzkPath() {
        String id = VideoIdExtractor.extractVideoId("https://chzzk.naver.com/video/10442147");
        assertEquals("10442147", id);

        // trailing slash or query should still work
        String id2 = VideoIdExtractor.extractVideoId("https://chzzk.naver.com/video/10442147/");
        assertEquals("10442147", id2);
        String id3 = VideoIdExtractor.extractVideoId("https://chzzk.naver.com/video/10442147?foo=bar");
        assertEquals("10442147", id3);
    }

    @Test
    void nullOnInvalid() {
        String id = VideoIdExtractor.extractVideoId(null);
        assertNull(id);
    }
}
