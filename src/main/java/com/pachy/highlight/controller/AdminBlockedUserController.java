package com.pachy.highlight.controller;

import com.pachy.highlight.dto.admin.BlockedUserPreview;
import com.pachy.highlight.dto.admin.BlockedUserRequest;
import com.pachy.highlight.dto.response.Response;
import com.pachy.highlight.dto.response.ResponseData;
import com.pachy.highlight.dto.response.ResponseList;
import com.pachy.highlight.entity.BlockedUser;
import com.pachy.highlight.service.BlockedUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 비수집(블랙리스트) 회원 관리 (관리자 전용). ROLE_ADMIN 필요.
 *
 * <p>수집을 원하지 않는다는 메일을 받으면 uid 를 등록한다. 등록 즉시 저장된 채팅이 삭제되고,
 * 이후 수집에서도 해당 uid 는 제외된다.
 */
@RestController
@RequestMapping("/api/v1/admin/blocked-users")
@RequiredArgsConstructor
public class AdminBlockedUserController {

    private final BlockedUserService blockedUserService;

    @GetMapping
    public ResponseEntity<ResponseList<BlockedUser>> list() {
        List<BlockedUser> users = blockedUserService.list();
        ResponseList<BlockedUser> response = new ResponseList<>(HttpStatus.OK);
        response.setCount(users.size());
        response.setList(users);
        return ResponseEntity.ok(response);
    }

    /** 등록 전 확인: 이 uid 로 몇 건의 채팅이 저장돼 있고 어떤 닉네임을 썼는지 보여준다. */
    @GetMapping("/preview")
    public ResponseEntity<ResponseData<BlockedUserPreview>> preview(@RequestParam("uid") String uid) {
        String trimmed = uid.trim();
        BlockedUserPreview preview = BlockedUserPreview.builder()
                .uid(trimmed)
                .chatCount(blockedUserService.chatCountOf(trimmed))
                .nicknames(blockedUserService.nicknamesOf(trimmed))
                .alreadyBlocked(blockedUserService.isRegistered(trimmed))
                .build();
        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK, preview));
    }

    @PostMapping
    public ResponseEntity<ResponseData<BlockedUser>> create(@Valid @RequestBody BlockedUserRequest req) {
        BlockedUser saved = blockedUserService.block(req.getUid(), req.getNickname(), req.getMemo());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>(HttpStatus.CREATED, saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> delete(@PathVariable("id") Long id) {
        if (!blockedUserService.unblock(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(HttpStatus.NOT_FOUND));
        }
        return ResponseEntity.ok(new Response(HttpStatus.OK));
    }
}
