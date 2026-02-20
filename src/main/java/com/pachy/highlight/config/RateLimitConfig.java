package com.pachy.highlight.config;

import com.pachy.highlight.util.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the RateLimitFilter for all endpoints.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RateLimitFilter());
        // Apply to all endpoints
        reg.addUrlPatterns("/*");
        reg.setName("rateLimitFilter");
        // Execute early in the filter chain
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
