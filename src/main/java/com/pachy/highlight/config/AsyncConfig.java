package com.pachy.highlight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 하이라이트 생성/채팅 재수집용 백그라운드 실행기.
 *
 * <p>치지직 채팅 수집은 영상 하나에 수 분이 걸릴 수 있어 동시 실행 수를 제한한다.
 * SSE(진행상황 스트리밍)의 비동기 요청 타임아웃도 여기서 넉넉히 잡는다.
 */
@Configuration
public class AsyncConfig implements WebMvcConfigurer {

    @Bean(name = "applicationTaskExecutor")
    public TaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("highlight-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // SseEmitter 자체 타임아웃(10분)보다 길게 잡아 컨테이너가 먼저 끊지 않도록 한다
        configurer.setDefaultTimeout(11 * 60 * 1000L);
    }
}
