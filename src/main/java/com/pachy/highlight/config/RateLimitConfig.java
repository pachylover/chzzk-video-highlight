package com.pachy.highlight.config;

import com.pachy.highlight.util.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the RateLimitFilter for all endpoints.
 * 분당 허용 요청 수는 rate-limit.per-minute (env: RATE_LIMIT_PER_MINUTE) 로 조정 가능 (기본 100).
 */
@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.per-minute:100}")
    private int perMinute;

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RateLimitFilter(perMinute));
        // Apply to all endpoints
        reg.addUrlPatterns("/*");
        reg.setName("rateLimitFilter");
        // Execute early in the filter chain
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
