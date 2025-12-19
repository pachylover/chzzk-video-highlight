package com.pacny.highlight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("chzzkWebClient")
    public WebClient chzzkWebClient(WebClient.Builder builder, @Value("${chzzk.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
