package com.pacny.highlight.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
public class CreateHighlightRequest {
    @NotBlank(message = "url은 필수입니다")
    @URL(message = "유효한 URL을 입력하세요")
    private String url;

    @URL(message = "callbackUrl은 유효한 URL이어야 합니다")
    private String callbackUrl;
}
