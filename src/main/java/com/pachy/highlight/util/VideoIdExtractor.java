package com.pachy.highlight.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoIdExtractor {

    // query 파라미터 v= 또는 /watch?v= 형식, 또는 경로의 마지막 세그먼트에서 ID 추출을 시도합니다.
    private static final Pattern V_PARAM = Pattern.compile("[?&]v=([a-zA-Z0-9_-]+)");
    private static final Pattern LAST_SEGMENT = Pattern.compile(".*/([a-zA-Z0-9_-]+)(?:\\?.*)?$");
    // chzzk.naver.com의 /video/{id} 경로 지원 (숫자 ID)
    private static final Pattern CHZZK_VIDEO = Pattern.compile("^/video/(\\d+)(?:/.*)?$");

    public static String extractVideoId(String url) {
        if (url == null) return null;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String path = uri.getPath();

            // chzzk.naver.com 특화 파싱 (예: https://chzzk.naver.com/video/10442147)
            if (host != null && host.contains("chzzk.naver.com") && path != null) {
                Matcher mch = CHZZK_VIDEO.matcher(path);
                if (mch.find()) return mch.group(1);
            }

            // 일반적인 query param v= 우선 확인
            Matcher m = V_PARAM.matcher(url);
            if (m.find()) return m.group(1);

            // 마지막 path segment
            if (path != null) {
                Matcher m2 = LAST_SEGMENT.matcher(path);
                if (m2.find()) return m2.group(1);
            }
        } catch (URISyntaxException ignored) {
        }
        return null;
    }
}
