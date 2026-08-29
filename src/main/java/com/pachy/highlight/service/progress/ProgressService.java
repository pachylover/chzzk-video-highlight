package com.pachy.highlight.service.progress;

import com.pachy.highlight.dto.progress.ProgressEvent;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 영상별 작업 진행 상황을 SSE 로 중계한다.
 *
 * <p>구독자는 연결 직후 마지막 이벤트를 즉시 받으므로, 작업이 시작된 뒤에 접속한
 * 클라이언트도 현재 단계를 바로 표시할 수 있다. 단일 인스턴스 기준 in-memory 구현이다.
 */
@Service
@Slf4j
public class ProgressService {

    /** SSE 연결 유지 시간 (10분). 채팅 수집이 오래 걸리는 영상까지 커버한다. */
    private static final long EMITTER_TIMEOUT_MS = 10 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, ProgressEvent> lastEvent = new ConcurrentHashMap<>();
    private final Map<String, Boolean> running = new ConcurrentHashMap<>();

    /** 이미 진행 중인 작업이면 false 를 반환한다 (중복 실행 방지). */
    public boolean tryStart(String videoId) {
        return running.putIfAbsent(videoId, Boolean.TRUE) == null;
    }

    public void finish(String videoId) {
        running.remove(videoId);
    }

    public boolean isRunning(String videoId) {
        return running.containsKey(videoId);
    }

    public SseEmitter subscribe(String videoId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> list = emitters.computeIfAbsent(videoId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> remove(videoId, emitter));
        emitter.onTimeout(() -> {
            remove(videoId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> remove(videoId, emitter));

        // 연결 확인용 + 프록시의 유휴 연결 차단 방지
        send(emitter, "open", ProgressEvent.of(videoId, ProgressEvent.Phase.START, "연결되었습니다", 0));

        // 진행 중이던 작업이 있으면 마지막 상태를 즉시 재생한다
        ProgressEvent last = lastEvent.get(videoId);
        if (last != null) {
            send(emitter, "progress", last);
        }
        return emitter;
    }

    public void publish(ProgressEvent event) {
        lastEvent.put(event.getVideoId(), event);
        List<SseEmitter> list = emitters.get(event.getVideoId());
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            send(emitter, "progress", event);
        }

        if (event.isTerminal()) {
            for (SseEmitter emitter : list) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // 이미 닫힌 연결
                }
            }
            emitters.remove(event.getVideoId());
        }
    }

    public void publish(String videoId, ProgressEvent.Phase phase, String message, int percent) {
        publish(ProgressEvent.of(videoId, phase, message, percent));
    }

    private void send(SseEmitter emitter, String name, ProgressEvent event) {
        try {
            emitter.send(SseEmitter.event().name(name).data(event));
        } catch (IOException | IllegalStateException e) {
            // 클라이언트가 끊은 경우 — 정리만 하고 넘어간다
            remove(event.getVideoId(), emitter);
        }
    }

    private void remove(String videoId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(videoId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) emitters.remove(videoId);
    }
}
