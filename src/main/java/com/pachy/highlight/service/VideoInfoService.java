package com.pachy.highlight.service;

import com.pachy.highlight.client.ChzzkClient;
import com.pachy.highlight.dto.ChzzkVideoResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 치지직 영상 메타 정보 조회 + 인메모리 캐시.
 * 컨트롤러/하이라이트 생성/제목 백필이 모두 같은 캐시를 공유하도록 한 곳에 모았다.
 */
@Service
@RequiredArgsConstructor
public class VideoInfoService {

    private final ChzzkClient chzzkClient;
    private final Map<String, ChzzkVideoResponse> cache = new ConcurrentHashMap<>();

    /** 캐시에 있으면 캐시를, 없으면 치지직 API 를 조회한다. 실패 시 null. */
    public ChzzkVideoResponse get(String videoId) {
        ChzzkVideoResponse cached = cache.get(videoId);
        if (cached != null) return cached;

        ChzzkVideoResponse fetched = chzzkClient.fetchVideoInfo(videoId);
        // null 은 캐시하지 않는다 (일시적 실패 후 재시도 가능하도록)
        if (fetched != null) cache.put(videoId, fetched);
        return fetched;
    }

    /** 영상 제목만 필요할 때. 조회 실패 시 null. */
    public String getTitle(String videoId) {
        ChzzkVideoResponse info = get(videoId);
        if (info == null) return null;
        String title = info.getVideoTitle();
        return (title == null || title.isBlank()) ? null : title;
    }
}
