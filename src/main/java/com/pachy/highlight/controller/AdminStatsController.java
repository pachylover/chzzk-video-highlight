package com.pachy.highlight.controller;

import com.pachy.highlight.dto.admin.RecentHighlightItem;
import com.pachy.highlight.dto.admin.StatsResponse;
import com.pachy.highlight.dto.response.ResponseData;
import com.pachy.highlight.dto.response.ResponseList;
import com.pachy.highlight.entity.Highlight;
import com.pachy.highlight.repository.ChatRepository;
import com.pachy.highlight.repository.HighlightRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final HighlightRepository highlightRepository;
    private final ChatRepository chatRepository;

    @GetMapping("/stats")
    public ResponseEntity<ResponseData<StatsResponse>> stats() {
        Instant startOfToday = Instant.now().atZone(KST).toLocalDate().atStartOfDay(KST).toInstant();
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant fourteenDaysAgo = Instant.now().minus(14, ChronoUnit.DAYS);

        List<StatsResponse.DailyCount> daily = highlightRepository.dailyCreatedCounts(fourteenDaysAgo).stream()
                .map(row -> StatsResponse.DailyCount.builder()
                        .date(row[0] != null ? row[0].toString() : "")
                        .count(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .build())
                .toList();

        List<StatsResponse.TopVideo> topVideos = highlightRepository.topVideosByHighlightCount(5).stream()
                .map(row -> StatsResponse.TopVideo.builder()
                        .videoId(row[0] != null ? row[0].toString() : "")
                        .highlightCount(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .maxChatCount(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                        .build())
                .toList();

        StatsResponse stats = StatsResponse.builder()
                .totalVideos(highlightRepository.countDistinctVideos())
                .totalHighlights(highlightRepository.count())
                .totalChats(chatRepository.count())
                .highlightsToday(highlightRepository.countCreatedSince(startOfToday))
                .highlightsLast7Days(highlightRepository.countCreatedSince(sevenDaysAgo))
                .dailyTrend(daily)
                .topVideos(topVideos)
                .build();

        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK, stats));
    }

    @GetMapping("/highlights/recent")
    public ResponseEntity<ResponseList<RecentHighlightItem>> recentHighlights(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<Highlight> highlights = highlightRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
        List<RecentHighlightItem> items = highlights.stream().map(RecentHighlightItem::from).toList();

        ResponseList<RecentHighlightItem> response = new ResponseList<>(HttpStatus.OK);
        response.setCount(items.size());
        response.setList(items);
        return ResponseEntity.ok(response);
    }
}
