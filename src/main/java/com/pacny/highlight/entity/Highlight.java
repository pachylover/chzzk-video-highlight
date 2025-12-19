package com.pacny.highlight.entity;

import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "highlights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Highlight {

    @Id
    @UuidGenerator
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "minute", nullable = false)
    private Instant minute;

    @Column(name = "start_ts")
    private Instant startTs;

    @Column(name = "end_ts")
    private Instant endTs;

    @Column(name = "chat_count")
    private Integer chatCount;

    @Column(name = "title")
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "chat_snapshot", columnDefinition = "jsonb")
    private String chatSnapshot;

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
}
