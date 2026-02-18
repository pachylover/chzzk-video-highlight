package com.pachy.highlight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Bean
	@Qualifier("chzzkWebClient")
	public WebClient chzzkWebClient(WebClient.Builder builder,
			@Value("${chzzk.base-url}") String baseUrl,
			@Value("${chzzk.max-in-memory-size:10485760}") int maxInMemorySize) {
		ExchangeStrategies strategies = ExchangeStrategies.builder()
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
				.build();

		if (baseUrl == null || baseUrl.isEmpty()) {
			throw new IllegalArgumentException("chzzk.base-url must be configured");
		}

		return builder.baseUrl(baseUrl)
				.exchangeStrategies(strategies)
				.build();
	}
}
