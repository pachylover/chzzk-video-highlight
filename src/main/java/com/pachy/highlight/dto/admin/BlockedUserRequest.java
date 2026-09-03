package com.pachy.highlight.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlockedUserRequest {
    /** 치지직 채널 URL 뒤의 긴 문자열(uid) */
    @NotBlank
    private String uid;
    private String nickname;
    private String memo;
}
