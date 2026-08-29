package com.pachy.highlight.controller;

import com.pachy.highlight.dto.ChatSearchItem;
import com.pachy.highlight.dto.ChatTimelineResponse;
import com.pachy.highlight.dto.response.PageResult;
import com.pachy.highlight.dto.response.Response;
import com.pachy.highlight.dto.response.ResponseData;
import com.pachy.highlight.entity.Chat;
import com.pachy.highlight.repository.ChatRepository;
import com.pachy.highlight.service.HighlightService;
import com.pachy.highlight.service.progress.ProgressService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    /** 그래프 점이 너무 촘촘해지지 않도록 하는 상한. 초과하면 여러 분을 한 점으로 묶는다. */
    private static final int MAX_POINTS = 600;

    private final ChatRepository chatRepository;
    private final HighlightService highlightService;
    private final ProgressService progressService;

    /**
     * 채팅 검색 (전체 사용자용).
     * - keyword: 메시지 부분검색
     * - username: 닉네임 부분검색 (닉네임별 채팅 찾기)
     * 둘 다 비어 있으면 해당 영상의 전체 채팅을 시간순으로 반환한다.
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<PageResult<ChatSearchItem>> search(
            @PathVariable("videoId") String videoId,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "username", required = false, defaultValue = "") String username,
            // mode: partial(부분 일치, 기본) | exact(정확히 일치)
            @RequestParam(value = "mode", required = false, defaultValue = "partial") String mode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        Page<Chat> result = "exact".equalsIgnoreCase(mode)
                ? chatRepository.searchChatsExact(videoId, keyword.trim(), username.trim(), pageable)
                : chatRepository.searchChats(videoId, keyword.trim(), username.trim(), pageable);

        return ResponseEntity.ok(PageResult.of(result, ChatSearchItem::from));
    }

    /**
     * 분당 채팅량 타임라인. 그래프의 한 점을 클릭하면 해당 구간(seconds)으로 이동한다.
     * 채팅이 하나도 없으면 points 는 비고 totalChats 가 0 이 되어, 클라이언트가
     * "채팅 다시 불러오기" 버튼을 활성화한다.
     */
    @GetMapping("/{videoId}/timeline")
    public ResponseEntity<ResponseData<ChatTimelineResponse>> timeline(@PathVariable("videoId") String videoId) {
        List<Object[]> rows = chatRepository.findChatCountsPerMinute(videoId);

        Map<Integer, Long> byMinute = new HashMap<>();
        int maxMinute = 0;
        long total = 0;
        for (Object[] row : rows) {
            if (row == null || row[0] == null) continue;
            int minute = ((Number) row[0]).intValue();
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (minute < 0) continue;
            byMinute.merge(minute, count, Long::sum);
            maxMinute = Math.max(maxMinute, minute);
            total += count;
        }

        // 채팅이 없는 분을 0 으로 채워 연속된 시계열을 만들고, 길면 구간을 묶는다
        int bucketSize = Math.max(1, (maxMinute + 1 + MAX_POINTS - 1) / MAX_POINTS);
        List<ChatTimelineResponse.Point> points = new ArrayList<>();
        long peak = 0;
        if (!byMinute.isEmpty()) {
            for (int start = 0; start <= maxMinute; start += bucketSize) {
                long count = 0;
                for (int m = start; m < start + bucketSize && m <= maxMinute; m++) {
                    count += byMinute.getOrDefault(m, 0L);
                }
                peak = Math.max(peak, count);
                points.add(ChatTimelineResponse.Point.builder()
                        .minute(start)
                        .seconds(start * 60)
                        .label(formatLabel(start))
                        .count(count)
                        .build());
            }
        }

        ChatTimelineResponse body = ChatTimelineResponse.builder()
                .videoId(videoId)
                .totalChats(total)
                .peakCount(peak)
                .processing(progressService.isRunning(videoId))
                .points(points)
                .build();

        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK, body));
    }

    /**
     * 채팅 다시 불러오기. 보관 기간이 지나 채팅이 사라진 영상을 치지직에서 재수집한다.
     * 진행 상황은 /api/v1/highlights/{videoId}/stream (SSE) 로 확인한다.
     */
    @PostMapping("/{videoId}/reload")
    public ResponseEntity<Response> reload(@PathVariable("videoId") String videoId) {
        Response response = new Response();

        if (chatRepository.countByVideoId(videoId) > 0) {
            response.setResultCode(HttpStatus.BAD_REQUEST.value());
            response.setResultMsg("이미 채팅이 저장되어 있습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!highlightService.reloadChats(videoId)) {
            response.setResultCode(HttpStatus.CONFLICT.value());
            response.setResultMsg("이미 처리 중인 작업이 있습니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        response.setResultCode(HttpStatus.ACCEPTED.value());
        response.setResultMsg("채팅을 다시 불러오는 중입니다.");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    private String formatLabel(int minute) {
        int h = minute / 60;
        int m = minute % 60;
        return h > 0 ? String.format("%d:%02d", h, m) : String.format("0:%02d", m);
    }
}
