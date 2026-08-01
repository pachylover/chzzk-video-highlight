package com.pachy.highlight.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pachy.highlight.dto.HighlightResponse;
import com.pachy.highlight.dto.admin.RecentHighlightItem;
import com.pachy.highlight.dto.response.Response;
import com.pachy.highlight.dto.response.ResponseList;
import com.pachy.highlight.entity.Highlight;
import com.pachy.highlight.repository.HighlightRepository;
import com.pachy.highlight.service.HighlightService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/highlights")
@RequiredArgsConstructor
public class HighlightController {

    private static final Map<String, Boolean> processing = new ConcurrentHashMap<>();
    private final HighlightService highlightService;
    private final HighlightRepository highlightRepository;

    // 공개 홈용: 최근 생성된 하이라이트 (영상별 최신 1건, 기본 6건)
    @GetMapping("/recent")
    public ResponseEntity<ResponseList<RecentHighlightItem>> recent(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Highlight> highlights = highlightRepository.findRecentDistinctVideoHighlights(safeLimit);
        List<RecentHighlightItem> items = highlights.stream().map(RecentHighlightItem::from).toList();

        ResponseList<RecentHighlightItem> response = new ResponseList<>(HttpStatus.OK);
        response.setCount(items.size());
        response.setList(items);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseList<HighlightResponse>> get(@PathVariable("id") String id) {
        ResponseList<HighlightResponse> response = new ResponseList<>();
        HttpStatus status;
        List<HighlightResponse> result = highlightService.getHighlight(id);
        if (result == null || result.isEmpty()) status = HttpStatus.NOT_FOUND;
        else {
            if (processing.containsKey(id)) {
                status = HttpStatus.ACCEPTED; // Still processing, but we have partial data to show
            } else {
                status = HttpStatus.OK;
            }
            status = HttpStatus.OK;
            response.setCount(result.size());
            response.setList(result);
        }
        response.setResultCode(status.value());
        response.setResultMsg(status.getReasonPhrase());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}")
    public ResponseEntity<Response> create(@PathVariable("id") String videoId,
                                           @RequestParam(value = "type", required = false) String highlightType) {
        Response response = new Response();
        if (processing.putIfAbsent(videoId, true) != null) {
            response.setResultCode(HttpStatus.CONFLICT.value());
            response.setResultMsg("Highlight creation already in progress for this video");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        // 이미 하이라이트가 존재하는경우 400 응답
        List<HighlightResponse> result = highlightService.getHighlight(videoId);
        if (result != null && !result.isEmpty()) {
            response.setResultCode(HttpStatus.BAD_REQUEST.value());
            response.setResultMsg("Highlight already exists for this video");
            processing.remove(videoId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        highlightService.createHighlight(videoId, highlightType);
        response.setResultCode(HttpStatus.CREATED.value());
        response.setResultMsg("Highlight creation started");
        processing.remove(videoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
