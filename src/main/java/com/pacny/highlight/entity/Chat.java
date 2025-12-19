package com.pacny.highlight.entity;

import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @UuidGenerator
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "username")
    private String username;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "raw", columnDefinition = "jsonb")
    private String raw;

    // 원본 API의 messageTime (ms)
    @Column(name = "message_time")
    private Long messageTime;

    // API의 playerMessageTime (player 기준 밀리초?)
    @Column(name = "player_message_time")
    private Long playerMessageTime;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
