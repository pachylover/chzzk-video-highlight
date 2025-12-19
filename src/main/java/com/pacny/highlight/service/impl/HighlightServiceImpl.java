package com.pacny.highlight.service.impl;

import com.pacny.highlight.client.ChzzkClient;
import com.pacny.highlight.dto.HighlightResponse;
import com.pacny.highlight.entity.Chat;
import com.pacny.highlight.entity.Highlight;
import com.pacny.highlight.repository.ChatRepository;
import com.pacny.highlight.repository.HighlightRepository;
import com.pacny.highlight.service.HighlightService;
import com.pacny.highlight.util.VideoIdExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HighlightServiceImpl implements HighlightService {

    private final HighlightRepository highlightRepository;
    private final ChatRepository chatRepository;
    private final ChzzkClient chzzkClient;

    @Override
    @Transactional
    public UUID createHighlight(String url, String callbackUrl) {
        String videoId = VideoIdExtractor.extractVideoId(url);
        Highlight h = Highlight.builder()
                .videoId(videoId)
                .status("PENDING")
                .build();
        h = highlightRepository.save(h);

        // 비동기 처리 시작
        processVideoAsync(videoId, h.getId());
        return h.getId();
    }

    @Async
    public void processVideoAsync(String videoId, UUID highlightId) {
        try {
            // 채팅 수집
            List<Chat> chats = chzzkClient.fetchAllChats(videoId);
            if (!chats.isEmpty()) {
                // DB에 이미 존재하는 message_time을 조회하여 중복 제거
                List<Long> times = chats.stream().map(Chat::getMessageTime).filter(Objects::nonNull).distinct().toList();
                List<Long> existing = times.isEmpty() ? List.of() : chatRepository.findExistingMessageTimes(videoId, times);
                Set<Long> existSet = new HashSet<>(existing);
                List<Chat> toSave = chats.stream().filter(c -> c.getMessageTime() == null || !existSet.contains(c.getMessageTime())).toList();

                int batch = 500;
                for (int i = 0; i < toSave.size(); i += batch) {
                    int toIndex = Math.min(i + batch, toSave.size());
                    chatRepository.saveAll(toSave.subList(i, toIndex));
                }
            }

            // 최다 채팅 1분 찾기
            Object[] peak = chatRepository.findPeakMinute(videoId);
            Optional<Highlight> oh = highlightRepository.findById(highlightId);
            if (oh.isEmpty()) return;
            Highlight h = oh.get();

            if (peak != null && peak.length >= 2 && peak[0] != null) {
                // peak[0]은 타임스탬프, peak[1]은 카운트
                Timestamp ts = (Timestamp) peak[0];
                Instant minute = ts.toInstant();
                Number cnt = (Number) peak[1];
                h.setMinute(minute);
                h.setChatCount(cnt != null ? cnt.intValue() : 0);

                // 간단 윈도우: -30초 ~ +90초
                h.setStartTs(minute.minus(Duration.ofSeconds(30)));
                h.setEndTs(minute.plus(Duration.ofSeconds(90)));

                h.setStatus("DONE");
                h = highlightRepository.save(h);
            } else {
                h.setStatus("FAILED");
                highlightRepository.save(h);
            }
        } catch (Exception e) {
            try {
                Optional<Highlight> oh = highlightRepository.findById(highlightId);
                oh.ifPresent(h -> {
                    h.setStatus("FAILED");
                    highlightRepository.save(h);
                });
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public HighlightResponse getHighlight(UUID id) {
        Optional<Highlight> oh = highlightRepository.findById(id);
        if (oh.isEmpty()) return null;
        Highlight h = oh.get();
        return HighlightResponse.builder()
                .taskId(h.getId().toString())
                .status(h.getStatus())
                .videoId(h.getVideoId())
                .minute(h.getMinute())
                .start(h.getStartTs())
                .end(h.getEndTs())
                .chatCount(h.getChatCount())
                .title(h.getTitle())
                .summary(h.getSummary())
                .build();
    }
}
