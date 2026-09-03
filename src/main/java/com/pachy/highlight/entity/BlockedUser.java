package com.pachy.highlight.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 수집 거부(블랙리스트) 회원.
 *
 * <p>{@code uid} 는 치지직 채널 URL 뒤의 긴 문자열로, 채팅 수집 시 저장하는
 * {@code chats.user_id}(userIdHash) 와 같은 값이다. 등록되면 이후 수집에서 제외되고,
 * 등록 시점에 이미 저장돼 있던 채팅은 삭제된다.
 */
@Entity
@Table(name = "blocked_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "bigserial", updatable = false, nullable = false)
    private Long id;

    @Column(name = "uid", nullable = false, unique = true)
    private String uid;

    /** 참고용 닉네임(요청자가 알려준 값 또는 기존 채팅에서 확인된 값) */
    @Column(name = "nickname")
    private String nickname;

    @Column(name = "memo")
    private String memo;

    /** 등록 시 삭제된 채팅 수 (처리 결과 확인용) */
    @Column(name = "deleted_chats", nullable = false)
    private Integer deletedChats;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.deletedChats == null) this.deletedChats = 0;
    }
}
