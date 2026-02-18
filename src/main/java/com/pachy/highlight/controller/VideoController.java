package com.pachy.highlight.controller;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pachy.highlight.client.ChzzkClient;
import com.pachy.highlight.dto.ChzzkVideoResponse;
import com.pachy.highlight.dto.response.ResponseData;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {
    private final Map<String, ChzzkVideoResponse> videoCache = new ConcurrentHashMap<>();
    private final ChzzkClient chzzkClient;

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<ChzzkVideoResponse>> get(@PathVariable("id") String videoId) {
        HttpStatus status;
        ChzzkVideoResponse r = videoCache.computeIfAbsent(videoId, chzzkClient::fetchVideoInfo);
        if (r == null) status = HttpStatus.NOT_FOUND;
        else status = HttpStatus.OK;
        return ResponseEntity.ok(new ResponseData<>(status, r));
    }
}
