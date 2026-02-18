package com.pachy.highlight.util;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pachy.highlight.util.func.WebUtil;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RateLimitFilter implements Filter {
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  private Bucket createNewBucket() {
    // 1분에 10개의 요청만 허용하는 설정
    Refill refill = Refill.intervally(10, Duration.ofMinutes(1));
    Bandwidth limit = Bandwidth.classic(10, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String ip = WebUtil.getRemoteIP(request);
    Bucket bucket = buckets.computeIfAbsent(ip, k -> createNewBucket());

    if (bucket.tryConsume(1)) { // 토큰이 있으면 통과
      log.warn("Rate limit allowed for IP: {}", ip);
      chain.doFilter(request, response);
    } else { // 토큰 없으면 429 에러
      ((HttpServletResponse) response).setStatus(429);
      response.getWriter().write("Too Many Requests - Slow down!");
      log.warn("Rate limit exceeded for IP: {}", ip);
    }
  }
}