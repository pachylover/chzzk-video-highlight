package com.pachy.highlight.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class AnnouncementRequest {
    @NotBlank
    private String message;
    private String level;   // INFO | WARNING | SUCCESS
    private String linkUrl;
    private Boolean isActive;
    private Instant startsAt;
    private Instant endsAt;
}
