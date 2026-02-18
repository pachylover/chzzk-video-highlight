package com.pachy.highlight.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pachy.highlight.client.ChzzkClient;
import com.pachy.highlight.dto.HighlightResponse;
import com.pachy.highlight.entity.Chat;
import com.pachy.highlight.entity.Highlight;
import com.pachy.highlight.repository.ChatBatchInsertRepository;
import com.pachy.highlight.repository.ChatRepository;
import com.pachy.highlight.repository.HighlightRepository;
import com.pachy.highlight.service.HighlightService;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighlightServiceImpl implements HighlightService {

    private final HighlightRepository highlightRepository;
    private final ChatRepository chatRepository;
    private final ChatBatchInsertRepository chatBatchInsertRepository;
    private final ChzzkClient chzzkClient;

    @Override
    @Transactional
    public Long createHighlight(String videoId) {
        log.info("하이라이트 생성 시작 - videoId: {}", videoId);
        // 비동기 처리 시작
        processVideoAsync(videoId);
        return null;
    }

    @Async
    @Transactional
    public void processVideoAsync(String videoId) {
        try {
            // 채팅 수집
            List<Chat> chats = chzzkClient.fetchAllChats(videoId);
            log.info("채팅 수집 완료 - videoId: {}, 채팅 수: {}", videoId, chats.size());

            if (!chats.isEmpty()) {
                final int CHUNK = 500;
                for (int i = 0; i < chats.size(); i += CHUNK) {
                    int toIndex = Math.min(i + CHUNK, chats.size());
                    List<Chat> chunk = chats.subList(i, toIndex);
                    long t0 = System.currentTimeMillis();

                    int inserted = chatBatchInsertRepository.insertBatch(chunk);

                    long dt = System.currentTimeMillis() - t0;

                    log.info("Batch inserted chunk [{} - {}) requested={} inserted={} in {} ms",
                            i, toIndex, chunk.size(), inserted, dt);
                }
            }


            // 최다 채팅 1분 찾기
            log.info("최다 채팅 찾기 시작");
            List<Object[]> peakRows = chatRepository.findPeakMinute(videoId);
            log.info("최다 채팅 찾기 종료 - 찾은 수: {}", peakRows.size());

            for (Object[] row : peakRows) {
                Highlight h = new Highlight();
                if (row == null || row.length < 3 || row[1] == null) continue;

                Object minuteObj = row[1];
                Object cntObj = row[2];

                Long minuteEpoch = null;
                // minute column now expected as bigint (epoch millis) but handle other driver types safely
                if (minuteObj instanceof Number) {
                    minuteEpoch = ((Number) minuteObj).longValue();
                } else if (minuteObj instanceof Timestamp) {
                    minuteEpoch = ((Timestamp) minuteObj).toInstant().toEpochMilli();
                } else if (minuteObj instanceof java.time.OffsetDateTime) {
                    minuteEpoch = ((java.time.OffsetDateTime) minuteObj).toInstant().toEpochMilli();
                } else if (minuteObj instanceof java.time.LocalDateTime) {
                    minuteEpoch = ((java.time.LocalDateTime) minuteObj).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                } else if (minuteObj instanceof java.time.Instant) {
                    minuteEpoch = ((java.time.Instant) minuteObj).toEpochMilli();
                } else if (minuteObj instanceof Object[]) {
                    Object[] inner = (Object[]) minuteObj;
                    if (inner.length > 0 && inner[0] instanceof Number) minuteEpoch = ((Number) inner[0]).longValue();
                }

                if (minuteEpoch != null) {
                    Number cnt = cntObj instanceof Number ? (Number) cntObj : null;
                    h.setVideoId(videoId);
                    h.setMinute(minuteEpoch);
                    h.setChatCount(cnt != null ? cnt.intValue() : 0);
                    h.setTitle(cnt != null ? String.format("%d 개의 채팅이 발생한 구간", cnt.intValue()) : "하이라이트");

                    long startEpoch = minuteEpoch - Duration.ofSeconds(30).toMillis();
                    long endEpoch = minuteEpoch + Duration.ofSeconds(90).toMillis();

                    h.setStartTs(startEpoch);
                    h.setEndTs(endEpoch);

                    h.setStatus("DONE");
                    highlightRepository.save(h);
                }
            }
            log.info("하이라이트 생성 완료 - videoId: {}", videoId);
        } catch (Exception e) {
            // 예외 처리: 로그 기록 등
            log.error("하이라이트 생성 중 예외 발생 - videoId: {}", videoId, e);
            e.printStackTrace();
        }
    }

    @Override
    public List<HighlightResponse> getHighlight(String id) {
        List<Highlight> highlights = highlightRepository.findAllByVideoId(id);
        if (highlights.isEmpty()) return null;
        return highlights.stream().map(Highlight::toResponse).toList();
    }
}
