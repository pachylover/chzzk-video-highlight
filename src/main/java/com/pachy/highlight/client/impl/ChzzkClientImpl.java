package com.pachy.highlight.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pachy.highlight.client.ChzzkClient;
import com.pachy.highlight.client.dto.ChzzkResponse;
import com.pachy.highlight.dto.ChzzkChatResponse;
import com.pachy.highlight.dto.ChzzkVideoResponse;
import com.pachy.highlight.entity.Chat;
import com.pachy.highlight.util.func.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;


import java.time.Duration;
import java.util.*;
import java.util.function.IntConsumer;

@Primary
@Component
public class ChzzkClientImpl implements ChzzkClient {
    private static final Logger log = LoggerFactory.getLogger(ChzzkClientImpl.class);

    private final ObjectMapper mapper;
    private final WebClient webClient;

    public ChzzkClientImpl(ObjectMapper mapper, @Qualifier("chzzkWebClient") WebClient webClient) {
        this.mapper = mapper;
        this.webClient = webClient;
    }

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_RETRIES = 5;
    /** 새 채팅이 하나도 늘지 않는 페이지가 이만큼 연속되면 더 볼 것이 없다고 본다. */
    private static final int MAX_NO_PROGRESS_PAGES = 3;
    /** 한 영상 수집에 쓸 수 있는 최대 시간. 이걸 넘기면 지금까지 모은 것으로 진행한다. */
    private static final Duration MAX_COLLECT_DURATION = Duration.ofMinutes(20);
    /**
     * block() 자체의 상한. WebClient 의 응답 타임아웃이 못 잡는 구간(커넥션 풀 대기 등)까지
     * 덮어 어떤 경우에도 스레드가 영구히 묶이지 않게 한다. 초과하면 예외가 나고 재시도로 넘어간다.
     */
    private static final Duration REQUEST_BLOCK_TIMEOUT = Duration.ofSeconds(30);

    @Override
    public List<Chat> fetchAllChats(String videoId, IntConsumer onProgress) {
        List<Chat> out = new ArrayList<>();
        long playerMessageTime = 0; // start from latest and walk back
        int pageSize = DEFAULT_PAGE_SIZE;
        int retry = 0;
        Set<String> seen = new HashSet<>(); // dedupe within fetch by composite key

        // 종료 보장용 — 치지직이 커서를 앞으로 보내주지 않으면 아래 세 가지 중 하나로 반드시 멈춘다.
        Set<Long> visitedCursors = new HashSet<>();
        int noProgressPages = 0;
        long deadline = System.currentTimeMillis() + MAX_COLLECT_DURATION.toMillis();

        log.info("채팅 수집 시작 - videoId: {}", videoId);

        while (true) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("채팅 수집 시간 초과({}분) - videoId: {}, 지금까지 {}건",
                        MAX_COLLECT_DURATION.toMinutes(), videoId, out.size());
                break;
            }

            try {
                // lambda에서 로컬 변수를 캡처할 때는 해당 변수가 final 또는 effectively final이어야 합니다.
                // `playerMessageTime`은 루프 안에서 갱신되므로 여기서는 복사본을 만들어 사용합니다.
                long pmt = playerMessageTime;

                ChzzkResponse<ChzzkChatResponse> resp = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/service/v1/videos/{videoId}/chats") // 템플릿화
                                .queryParam("playerMessageTime", pmt)
                                .queryParam("previousVideoChatSize", pageSize)
                                .build(videoId))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ChzzkResponse<ChzzkChatResponse>>() {})
                        .block(REQUEST_BLOCK_TIMEOUT);

                if (resp == null || resp.getContent() == null || resp.getContent().getVideoChats() == null || resp.getContent().getVideoChats().isEmpty()) {
                    break;
                }

                int sizeBeforePage = out.size();

                for (ChzzkChatResponse.VideoChat vc : resp.getContent().getVideoChats()) {
                    // composite key to avoid duplicates within a fetch run
                    String key = videoId + "|" + vc.getPlayerMessageTime() + "|" + vc.getUserIdHash();
                    if (seen.contains(key)) continue;
                    seen.add(key);

                    // message가 이모티콘만으로 구성된 경우 continue (실제 채팅 메시지가 없는 경우 제외)
                    // 이모티콘은 {:imoticon_name:}의 형식으로 이루어져 있음, 여러개도 사용 가능한 형태이므로 정규식으로 체크
                    String contents = vc.getContent();
                    if (contents != null && contents.matches("^(\\{:[^:]+?:\\})+$")) {
                        continue;
                    }
                    // sanitize chat text early to avoid inserting invalid bytes and to
                    // allow emptiness check after trimming
                    contents = StringUtil.sanitizeChat(contents);
                    // contents 데이터 sanity check (null 또는 빈 문자열인 경우 continue)
                    if (StringUtil.isEmpty(contents)) {
                        continue;
                    }

                    Chat c = Chat.builder()
                            .videoId(videoId)
                            .message(contents)
                            .userId(vc.getUserIdHash())
                            .playerMessageTime(vc.getPlayerMessageTime())
                            .build();

                    // try to extract nickname from profile JSON if available (fallback to extras if needed)
                    if (vc.getProfile() != null) {
                        try {
                            JsonNode p = mapper.readTree(vc.getProfile());
                            if (p.has("nickname")) {
                                c.setUsername(p.get("nickname").asText());
                            }
                        } catch (Exception e) {
                            // ignore parsing failure
                        }
                    }

                    // if profile didn't yield a username, try extras (sometimes provided there)
                    if (c.getUsername() == null && vc.getExtras() != null) {
                        try {
                            JsonNode ex = mapper.readTree(vc.getExtras());
                            if (ex.has("nickname")) {
                                c.setUsername(ex.get("nickname").asText());
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    out.add(c);
                }

                onProgress.accept(out.size());

                // 이번 페이지에서 새로 담긴 게 없으면 같은 구간을 다시 받고 있는 것이다.
                // out 이 늘지 않으므로 아래 건수 기반 안전장치로는 걸러지지 않아 따로 센다.
                if (out.size() == sizeBeforePage) {
                    if (++noProgressPages >= MAX_NO_PROGRESS_PAGES) {
                        log.warn("새로 수집되는 채팅이 없어 중단 - videoId: {}, 총 {}건", videoId, out.size());
                        break;
                    }
                } else {
                    noProgressPages = 0;
                }

                Long next = resp.getContent().getNextPlayerMessageTime();
                if (next == null || next.equals(playerMessageTime)) {
                    break;
                }

                // 커서가 이미 지나온 값으로 되돌아오면(A → B → A) 무한히 돈다. 여기서 끊는다.
                if (!visitedCursors.add(next)) {
                    log.warn("커서가 이전 값으로 되돌아와 중단 - videoId: {}, cursor: {}, 총 {}건",
                            videoId, next, out.size());
                    break;
                }

                playerMessageTime = next;

                // reset retry on success
                retry = 0;

                // small safety limit (avoid infinite loops)
                if (out.size() > 200_000) {
                    log.warn("fetchAllChats reached safety limit for video {}: {} records", videoId, out.size());
                    break;
                }

            } catch (WebClientResponseException we) {
                int code = we.getStatusCode().value();
                if (code == 429) {
                    if (retry++ >= MAX_RETRIES) {
                        log.error("Too many 429 responses while fetching chats for {}", videoId);
                        break;
                    }
                    backoffSleep(retry);
                    continue;
                } else if (code >= 500 && code < 600) {
                    if (retry++ >= MAX_RETRIES) {
                        log.error("Server errors while fetching chats for {}: {}", videoId, we.getMessage());
                        break;
                    }
                    backoffSleep(retry);
                    continue;
                } else {
                    // we 전체 에러 표시
                    log.error(" Error response while fetching chats for {}: {} - {}", videoId, code, we.getResponseBodyAsString());
                    log.error("Unexpected response while fetching chats for {}: {}", videoId, we.getMessage());
                    break;
                }
            } catch (Exception e) {
                // 타임아웃(ReadTimeout / block 시간 초과)도 여기로 온다.
                // 어디서 몇 건까지 받고 끊겼는지 남겨야 다음에 원인을 추적할 수 있다.
                log.warn("채팅 요청 실패 - videoId: {}, 누적 {}건, {}번째 시도: {}",
                        videoId, out.size(), retry + 1, e.toString());
                if (retry++ >= MAX_RETRIES) {
                    log.error("재시도 한도 초과로 수집 중단 - videoId: {}, 총 {}건", videoId, out.size());
                    break;
                }
                backoffSleep(retry);
            }
        }

        return out;
    }

    public ChzzkVideoResponse fetchVideoInfo(String videoId) {
        try {
            
            ChzzkResponse<ChzzkVideoResponse> resp = webClient.get()
                    .uri(urlBuilder -> urlBuilder.path("/service/v3/videos/{videoId}").build(videoId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ChzzkResponse<ChzzkVideoResponse>>() {})
                    .block(REQUEST_BLOCK_TIMEOUT);

            if (resp == null || resp.getContent() == null) {
                return null;
            }

            return resp.getContent();
        } catch (Exception e) {
            log.error("Error fetching video info for {}: {}", videoId, e.getMessage());
            e.printStackTrace();
            return null;
        }
    }



    private void backoffSleep(int retry) {
        try {
            long wait = Math.min(5_000, (long) Math.pow(2, retry) * 500L);
            Thread.sleep(wait);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
