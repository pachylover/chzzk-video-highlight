package com.pachy.highlight.dto;

import com.pachy.highlight.entity.Chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSearchItem {
    private Long id;
    private String userId;
    private String username;
    private String message;
    private Long playerMessageTime; // epoch millis offset (player 기준)
    private Long seconds;           // playerMessageTime 를 초 단위로 변환 (링크 이동용)

    public static ChatSearchItem from(Chat c) {
        Long pmt = c.getPlayerMessageTime();
        return ChatSearchItem.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .username(c.getUsername())
                .message(c.getMessage())
                .playerMessageTime(pmt)
                .seconds(pmt != null ? pmt / 1000 : null)
                .build();
    }
}
