package com.pachy.highlight.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pachy.highlight.client.ChzzkClient;
import com.pachy.highlight.dto.HighlightResponse;
import com.pachy.highlight.entity.Chat;
import com.pachy.highlight.entity.Highlight;
import com.pachy.highlight.repository.ChatRepository;
import com.pachy.highlight.repository.HighlightRepository;
import com.pachy.highlight.service.HighlightService;
import com.pachy.highlight.util.VideoIdExtractor;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class HighlightServiceImpl implements HighlightService {

    private final HighlightRepository highlightRepository;
    private final ChatRepository chatRepository;
    private final ChzzkClient chzzkClient;

    @Override
    @Transactional
    public Long createHighlight(String videoId) {
        // set a non-null placeholder minute (epoch millis, truncated to minute) to satisfy DB NOT NULL
        long placeholderMinute = Instant.now().truncatedTo(ChronoUnit.MINUTES).toEpochMilli();

        // 비동기 처리 시작
        processVideoAsync(videoId);
        return null;
    }

    @Async
    public void processVideoAsync(String videoId) {
        try {
            // 채팅 수집
            List<Chat> chats = chzzkClient.fetchAllChats(videoId);
            if (!chats.isEmpty()) {
                // DB에 이미 존재하는 player_message_time(epoch ms) 값을 조회하여 중복 제거
                List<Long> times = chats.stream()
                        .map(Chat::getPlayerMessageTime)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                List<Long> existing = times.isEmpty() ? List.of() : chatRepository.findExistingMessageTimes(videoId, times);
                Set<Long> existSet = new HashSet<>(existing);
                List<Chat> toSave = chats.stream().filter(c -> c.getPlayerMessageTime() == null || !existSet.contains(c.getPlayerMessageTime())).toList();

                int batch = 500;
                for (int i = 0; i < toSave.size(); i += batch) {
                    int toIndex = Math.min(i + batch, toSave.size());
                    chatRepository.saveAll(toSave.subList(i, toIndex));
                }
            }

            // 최다 채팅 1분 찾기
            List<Object[]> peakRows = chatRepository.findPeakMinute(videoId);

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
        } catch (Exception e) {
            // 예외 처리: 로그 기록 등
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
