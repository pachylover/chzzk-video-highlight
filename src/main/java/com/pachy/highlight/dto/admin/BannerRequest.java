package com.pachy.highlight.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class BannerRequest {
    private String title;
    @NotBlank
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private Instant startsAt;
    private Instant endsAt;
}
