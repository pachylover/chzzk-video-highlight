package com.pachy.highlight.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

	@Bean
	@Qualifier("chzzkWebClient")
	public WebClient chzzkWebClient(WebClient.Builder builder,
			@Value("${chzzk.base-url}") String baseUrl,
			@Value("${chzzk.max-in-memory-size:10485760}") int maxInMemorySize,
			@Value("${chzzk.connect-timeout-ms:10000}") int connectTimeoutMs,
			@Value("${chzzk.response-timeout-ms:20000}") int responseTimeoutMs,
			@Value("${chzzk.user-agent}") String userAgent) {
		ExchangeStrategies strategies = ExchangeStrategies.builder()
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
				.build();

		if (baseUrl == null || baseUrl.isEmpty()) {
			throw new IllegalArgumentException("chzzk.base-url must be configured");
		}

		// 타임아웃이 없으면 치지직이 연결만 받고 응답하지 않을 때 block() 이 영원히 멈춘다.
		// 채팅 수집은 백그라운드 스레드에서 돌기 때문에 그 스레드가 그대로 묶이고,
		// 로그도 CPU 사용도 없이 진행률만 멈춰 있는 상태가 된다.
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
				.responseTimeout(Duration.ofMillis(responseTimeoutMs))
				.doOnConnected(conn -> conn
						.addHandlerLast(new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS))
						.addHandlerLast(new WriteTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS)));

		return builder.baseUrl(baseUrl)
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.exchangeStrategies(strategies)
				// 익명 클라이언트로 보이지 않도록 서비스와 연락처를 밝힌다.
				// 치지직은 마음에 들지 않는 클라이언트에게 응답을 아예 주지 않는 방식으로 막는데
				// (curl 기본 UA 가 그렇다), 그 경우 요청이 그대로 매달린다.
				.defaultHeader("User-Agent", userAgent)
				.build();
	}
}
