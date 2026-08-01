package com.pachy.highlight.controller;

import com.pachy.highlight.dto.admin.BannerRequest;
import com.pachy.highlight.dto.response.Response;
import com.pachy.highlight.dto.response.ResponseData;
import com.pachy.highlight.dto.response.ResponseList;
import com.pachy.highlight.entity.Banner;
import com.pachy.highlight.repository.BannerRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 배너 관리 (관리자 전용). ROLE_ADMIN 필요.
 */
@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerRepository bannerRepository;

    @GetMapping
    public ResponseEntity<ResponseList<Banner>> list() {
        List<Banner> banners = bannerRepository.findAllByOrderBySortOrderAscIdAsc();
        ResponseList<Banner> response = new ResponseList<>(HttpStatus.OK);
        response.setCount(banners.size());
        response.setList(banners);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ResponseData<Banner>> create(@Valid @RequestBody BannerRequest req) {
        Banner banner = Banner.builder()
                .title(req.getTitle())
                .imageUrl(req.getImageUrl())
                .linkUrl(req.getLinkUrl())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .build();
        Banner saved = bannerRepository.save(banner);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>(HttpStatus.CREATED, saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<? extends Response> update(@PathVariable("id") Long id,
                                                     @Valid @RequestBody BannerRequest req) {
        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(HttpStatus.NOT_FOUND));
        }
        banner.setTitle(req.getTitle());
        banner.setImageUrl(req.getImageUrl());
        banner.setLinkUrl(req.getLinkUrl());
        if (req.getSortOrder() != null) banner.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) banner.setIsActive(req.getIsActive());
        banner.setStartsAt(req.getStartsAt());
        banner.setEndsAt(req.getEndsAt());
        Banner saved = bannerRepository.save(banner);
        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK, saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> delete(@PathVariable("id") Long id) {
        if (!bannerRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(HttpStatus.NOT_FOUND));
        }
        bannerRepository.deleteById(id);
        return ResponseEntity.ok(new Response(HttpStatus.OK));
    }
}
