package com.pachy.highlight.controller;

import com.pachy.highlight.dto.admin.AnnouncementRequest;
import com.pachy.highlight.dto.response.Response;
import com.pachy.highlight.dto.response.ResponseData;
import com.pachy.highlight.dto.response.ResponseList;
import com.pachy.highlight.entity.Announcement;
import com.pachy.highlight.repository.AnnouncementRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 안내문구 관리 (관리자 전용). ROLE_ADMIN 필요.
 */
@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementRepository announcementRepository;

    @GetMapping
    public ResponseEntity<ResponseList<Announcement>> list() {
        List<Announcement> items = announcementRepository.findAllByOrderByIdDesc();
        ResponseList<Announcement> response = new ResponseList<>(HttpStatus.OK);
        response.setCount(items.size());
        response.setList(items);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ResponseData<Announcement>> create(@Valid @RequestBody AnnouncementRequest req) {
        Announcement a = Announcement.builder()
                .message(req.getMessage())
                .level(req.getLevel() != null ? req.getLevel() : "INFO")
                .linkUrl(req.getLinkUrl())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .build();
        Announcement saved = announcementRepository.save(a);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>(HttpStatus.CREATED, saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<? extends Response> update(@PathVariable("id") Long id,
                                                     @Valid @RequestBody AnnouncementRequest req) {
        Announcement a = announcementRepository.findById(id).orElse(null);
        if (a == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(HttpStatus.NOT_FOUND));
        }
        a.setMessage(req.getMessage());
        if (req.getLevel() != null) a.setLevel(req.getLevel());
        a.setLinkUrl(req.getLinkUrl());
        if (req.getIsActive() != null) a.setIsActive(req.getIsActive());
        a.setStartsAt(req.getStartsAt());
        a.setEndsAt(req.getEndsAt());
        Announcement saved = announcementRepository.save(a);
        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK, saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> delete(@PathVariable("id") Long id) {
        if (!announcementRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(HttpStatus.NOT_FOUND));
        }
        announcementRepository.deleteById(id);
        return ResponseEntity.ok(new Response(HttpStatus.OK));
    }
}
