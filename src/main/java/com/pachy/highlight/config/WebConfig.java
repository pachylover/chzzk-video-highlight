package com.pachy.highlight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 는 SecurityConfig 의 CorsConfigurationSource 에서 중앙 관리한다.
 * (Spring Security 필터 체인과 헤더 중복을 피하기 위해 이곳에서는 CORS 를 설정하지 않는다.)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
