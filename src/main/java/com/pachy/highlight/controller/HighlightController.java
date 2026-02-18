package com.pachy.highlight.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pachy.highlight.dto.CreateHighlightRequest;
import com.pachy.highlight.dto.HighlightResponse;
import com.pachy.highlight.dto.response.Response;
import com.pachy.highlight.dto.response.ResponseList;
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
    public ResponseEntity<Response> create(@PathVariable("id") String videoId) {
        Response response = new Response();
        if (processing.putIfAbsent(videoId, true) != null) {
            response.setResultCode(HttpStatus.CONFLICT.value());
            response.setResultMsg("Highlight creation already in progress for this video");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        highlightService.createHighlight(videoId);
        response.setResultCode(HttpStatus.CREATED.value());
        response.setResultMsg("Highlight creation started");
        processing.remove(videoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
