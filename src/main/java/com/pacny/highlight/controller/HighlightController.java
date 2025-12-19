package com.pacny.highlight.controller;

import com.pacny.highlight.dto.CreateHighlightRequest;
import com.pacny.highlight.dto.HighlightResponse;
import com.pacny.highlight.service.HighlightService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/highlights")
@RequiredArgsConstructor
public class HighlightController {

    private final HighlightService highlightService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateHighlightRequest req) {
        UUID id = highlightService.createHighlight(req.getUrl(), req.getCallbackUrl());
        return ResponseEntity.accepted().body(new CreateResponse(id.toString(), "accepted"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HighlightResponse> get(@PathVariable("id") UUID id) {
        HighlightResponse r = highlightService.getHighlight(id);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(r);
    }

    private record CreateResponse(String taskId, String status) {}
}
