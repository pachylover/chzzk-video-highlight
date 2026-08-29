package com.pachy.highlight.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.pachy.highlight.dto.HighlightResponse;
import com.pachy.highlight.dto.progress.ProgressEvent.Phase;
import com.pachy.highlight.entity.Highlight;
import com.pachy.highlight.repository.HighlightRepository;
import com.pachy.highlight.service.HighlightService;
import com.pachy.highlight.service.progress.ProgressService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighlightServiceImpl implements HighlightService {

    private final HighlightRepository highlightRepository;
    private final HighlightProcessor highlightProcessor;
    private final ProgressService progressService;

    @Override
    public boolean createHighlight(String videoId, String highlightType) {
        log.info("하이라이트 생성 시작 - videoId: {}, type: {}", videoId, highlightType);
        if (!progressService.tryStart(videoId)) {
            return false;
        }
        progressService.publish(videoId, Phase.START, "하이라이트 생성을 준비하는 중입니다", 0);
        highlightProcessor.process(videoId);
        return true;
    }

    @Override
    public boolean reloadChats(String videoId) {
        log.info("채팅 재수집 시작 - videoId: {}", videoId);
        if (!progressService.tryStart(videoId)) {
            return false;
        }
        progressService.publish(videoId, Phase.START, "채팅을 다시 불러오는 중입니다", 0);
        highlightProcessor.reloadChats(videoId);
        return true;
    }

    @Override
    public List<HighlightResponse> getHighlight(String id) {
        List<Highlight> highlights = highlightRepository.findAllByVideoIdOrderByChatCountDesc(id);
        if (highlights.isEmpty()) return List.of();
        return highlights.stream().map(Highlight::toResponse).toList();
    }
}
