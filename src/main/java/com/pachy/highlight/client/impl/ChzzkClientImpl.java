package com.pachy.highlight.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pachy.highlight.client.ChzzkClient;
import com.pachy.highlight.client.dto.ChzzkResponse;
import com.pachy.highlight.dto.ChzzkChatResponse;
import com.pachy.highlight.dto.ChzzkVideoResponse;
import com.pachy.highlight.entity.Chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;


import java.util.*;

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

    @Override
    public List<Chat> fetchAllChats(String videoId) {
        List<Chat> out = new ArrayList<>();
        long playerMessageTime = 0; // start from latest and walk back
        int pageSize = DEFAULT_PAGE_SIZE;
        int retry = 0;
        Set<String> seen = new HashSet<>(); // dedupe within fetch by composite key

        while (true) {
            try {
                final String uri;
                uri = String.format("/service/v1/videos/%s/chats?playerMessageTime=%d&previousVideoChatSize=%d", videoId, playerMessageTime, pageSize);

                ChzzkResponse<ChzzkChatResponse> resp = webClient.get().uri(uri)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<ChzzkResponse<ChzzkChatResponse>>() {})
                        .block();

                if (resp == null || resp.getContent() == null || resp.getContent().getVideoChats() == null || resp.getContent().getVideoChats().isEmpty()) {
                    break;
                }

                for (ChzzkChatResponse.VideoChat vc : resp.getContent().getVideoChats()) {
                    // composite key to avoid duplicates within a fetch run
                    String key = videoId + "|" + vc.getPlayerMessageTime() + "|" + vc.getUserIdHash();
                    if (seen.contains(key)) continue;
                    seen.add(key);

                    // message가 이모티콘만으로 구성된 경우 continue (실제 채팅 메시지가 없는 경우 제외)
                    // 이모티콘은 {:imoticon_name:}의 형식으로 이루어져 있음, 여러개도 사용 가능한 형태이므로 정규식으로 체크
                    if (vc.getContent() != null && vc.getContent().matches("^(\\{:[^:]+?:\\})+$")) {
                        continue;
                    }


                    Chat c = Chat.builder()
                            .videoId(videoId)
                            .message(vc.getContent())
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

                Long next = resp.getContent().getNextPlayerMessageTime();
                if (next == null || next.equals(playerMessageTime)) {
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
                if (retry++ >= MAX_RETRIES) {
                    log.error("Error while fetching chats for {}: {}", videoId, e.getMessage());
                    break;
                }
                backoffSleep(retry);
            }
        }

        return out;
    }

    public ChzzkVideoResponse fetchVideoInfo(String videoId) {
        try {
            String uri = String.format("/service/v3/videos/%s", videoId);
            ChzzkResponse<ChzzkVideoResponse> resp = webClient.get().uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ChzzkResponse<ChzzkVideoResponse>>() {})
                    .block();

            if (resp == null || resp.getContent() == null) {
                return null;
            }

            return resp.getContent();
        } catch (Exception e) {
            log.error("Error fetching video info for {}: {}", videoId, e.getMessage());
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
