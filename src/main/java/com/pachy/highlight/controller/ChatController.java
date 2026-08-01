package com.pachy.highlight.controller;

import com.pachy.highlight.dto.ChatSearchItem;
import com.pachy.highlight.dto.response.PageResult;
import com.pachy.highlight.entity.Chat;
import com.pachy.highlight.repository.ChatRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRepository chatRepository;

    /**
     * 채팅 검색 (전체 사용자용).
     * - keyword: 메시지 부분검색
     * - username: 닉네임 부분검색 (닉네임별 채팅 찾기)
     * 둘 다 비어 있으면 해당 영상의 전체 채팅을 시간순으로 반환한다.
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<PageResult<ChatSearchItem>> search(
            @PathVariable("videoId") String videoId,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "username", required = false, defaultValue = "") String username,
            // mode: partial(부분 일치, 기본) | exact(정확히 일치)
            @RequestParam(value = "mode", required = false, defaultValue = "partial") String mode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        Page<Chat> result = "exact".equalsIgnoreCase(mode)
                ? chatRepository.searchChatsExact(videoId, keyword.trim(), username.trim(), pageable)
                : chatRepository.searchChats(videoId, keyword.trim(), username.trim(), pageable);

        return ResponseEntity.ok(PageResult.of(result, ChatSearchItem::from));
    }
}
