package com.pachy.highlight.entity;

import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "bigserial", updatable = false, nullable = false)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "username")
    private String username;

    @Column(name = "message", columnDefinition = "text")
    private String message;

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
