package com.pachy.highlight.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pachy.highlight.dto.ChzzkVideoResponse;
import com.pachy.highlight.dto.response.ResponseData;
import com.pachy.highlight.service.VideoInfoService;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoInfoService videoInfoService;

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<ChzzkVideoResponse>> get(@PathVariable("id") String videoId) {
        HttpStatus status;
        ChzzkVideoResponse r = videoInfoService.get(videoId);
        if (r == null) status = HttpStatus.NOT_FOUND;
        else status = HttpStatus.OK;
        return ResponseEntity.ok(new ResponseData<>(status, r));
    }
}
