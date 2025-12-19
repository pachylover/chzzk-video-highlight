package com.pacny.highlight.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacny.highlight.client.ChzzkClient;
import com.pacny.highlight.client.dto.ChzzkResponse;
import com.pacny.highlight.entity.Chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.*;

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
        long playerMessageTime = Long.MAX_VALUE; // start from latest and walk back
        int pageSize = DEFAULT_PAGE_SIZE;
        int retry = 0;
        Set<String> seen = new HashSet<>(); // dedupe within fetch by composite key

        while (true) {
            try {
                String uri = String.format("/service/v1/videos/%s/chats?playerMessageTime=%d&previousVideoChatSize=%d", videoId, playerMessageTime, pageSize);
                ChzzkResponse resp = webClient.get().uri(uri)
                        .retrieve()
                        .bodyToMono(ChzzkResponse.class)
                        .block();

                if (resp == null || resp.getContent() == null || resp.getContent().getVideoChats() == null || resp.getContent().getVideoChats().isEmpty()) {
                    break;
                }

                for (ChzzkResponse.VideoChat vc : resp.getContent().getVideoChats()) {
                    // composite key to avoid duplicates within a fetch run
                    String key = videoId + "|" + vc.getMessageTime() + "|" + vc.getUserIdHash();
                    if (seen.contains(key)) continue;
                    seen.add(key);

                    Chat c = Chat.builder()
                            .videoId(videoId)
                            .ts(vc.getMessageTime() != null ? Instant.ofEpochMilli(vc.getMessageTime()) : Instant.now())
                            .message(vc.getContent())
                            .userId(vc.getUserIdHash())
                            .raw(serializeSafe(vc))
                            .messageTime(vc.getMessageTime())
                            .playerMessageTime(vc.getPlayerMessageTime())
                            .build();

                    // try to extract nickname from profile JSON if available
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

    private String serializeSafe(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
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
