package com.pachy.highlight.entity;

import lombok.*;

import com.pachy.highlight.dto.HighlightResponse;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "highlights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Highlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "bigserial", updatable = false, nullable = false)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "minute", nullable = false, columnDefinition = "bigint")
    private Long minute;

    @Column(name = "start_ts", columnDefinition = "bigint")
    private Long startTs;

    @Column(name = "end_ts", columnDefinition = "bigint")
    private Long endTs;

    @Column(name = "chat_count")
    private Integer chatCount;

    @Column(name = "title")
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.updatedAt == null) this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public HighlightResponse toResponse() {
        return HighlightResponse.builder()
                .taskId(id != null ? id.toString() : null)
                .status(status)
                .videoId(videoId)
                .minute(minute != null ? minute : 0L)
                .start(startTs != null ? startTs : 0L)
                .end(endTs != null ? endTs : 0L)
                .chatCount(chatCount)
                .title(title)
                .summary(summary)
                .build();
    }
}
