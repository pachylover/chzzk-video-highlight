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
  private static final int DEFAULT_PER_MINUTE = 100;
  private final int perMinute;

  public RateLimitFilter() {
    this(DEFAULT_PER_MINUTE);
  }

  public RateLimitFilter(int perMinute) {
    this.perMinute = perMinute > 0 ? perMinute : DEFAULT_PER_MINUTE;
  }

  private Bucket createNewBucket() {
    // 1분에 perMinute 개의 요청만 허용하는 설정 (기본 100)
    Refill refill = Refill.intervally(perMinute, Duration.ofMinutes(1));
    Bandwidth limit = Bandwidth.classic(perMinute, refill);
    return Bucket.builder().addLimit(limit).build();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // 관리자 데이터 API(로그인 제외)는 인증(JWT)으로 보호되므로 IP 기반 속도 제한에서 제외한다.
    // (대시보드 한 번 로드에도 여러 요청이 발생하므로 10회/분 제한에 걸려 UX 가 깨지는 것을 방지)
    if (request instanceof jakarta.servlet.http.HttpServletRequest httpReq) {
      String uri = httpReq.getRequestURI();
      if (uri != null && uri.startsWith("/api/v1/admin/") && !uri.equals("/api/v1/admin/auth/login")) {
        chain.doFilter(request, response);
        return;
      }
    }

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