package com.pachy.highlight.controller;

import com.pachy.highlight.dto.response.ResponseList;
import com.pachy.highlight.entity.Announcement;
import com.pachy.highlight.repository.AnnouncementRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * 공개 안내문구 조회 (전체 사용자용).
 */
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    @GetMapping
    public ResponseEntity<ResponseList<Announcement>> activeAnnouncements() {
        List<Announcement> items = announcementRepository.findActiveAnnouncements(Instant.now());
        ResponseList<Announcement> response = new ResponseList<>(HttpStatus.OK);
        response.setCount(items.size());
        response.setList(items);
        return ResponseEntity.ok(response);
    }
}
