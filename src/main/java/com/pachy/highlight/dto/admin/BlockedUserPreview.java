package com.pachy.highlight.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 비수집 등록 전 확인용 — 해당 uid 로 저장된 채팅 수와 닉네임. */
@Data
@Builder
public class BlockedUserPreview {
    private String uid;
    private long chatCount;
    private List<String> nicknames;
    private boolean alreadyBlocked;
}
