package com.pachy.highlight.controller;

import com.pachy.highlight.dto.response.ResponseList;
import com.pachy.highlight.entity.Banner;
import com.pachy.highlight.repository.BannerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * 공개 배너 조회 (전체 사용자용).
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerRepository bannerRepository;

    @GetMapping
    public ResponseEntity<ResponseList<Banner>> activeBanners() {
        List<Banner> banners = bannerRepository.findActiveBanners(Instant.now());
        ResponseList<Banner> response = new ResponseList<>(HttpStatus.OK);
        response.setCount(banners.size());
        response.setList(banners);
        return ResponseEntity.ok(response);
    }
}
