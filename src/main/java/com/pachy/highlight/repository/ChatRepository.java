package com.pachy.highlight.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pachy.highlight.entity.Chat;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * 채팅 검색 (전체 사용자용). videoId 필수, keyword(메시지 부분검색)와 username(닉네임 부분검색)은 선택.
     * 빈 문자열이면 해당 조건을 무시한다. ILIKE 는 pg_trgm GIN 인덱스로 가속된다.
     */
    @Query(value = "SELECT * FROM chats " +
            "WHERE video_id = :videoId " +
            "AND (:keyword = '' OR message ILIKE '%' || :keyword || '%') " +
            "AND (:username = '' OR username ILIKE '%' || :username || '%') " +
            "ORDER BY player_message_time ASC",
            countQuery = "SELECT count(*) FROM chats " +
                    "WHERE video_id = :videoId " +
                    "AND (:keyword = '' OR message ILIKE '%' || :keyword || '%') " +
                    "AND (:username = '' OR username ILIKE '%' || :username || '%')",
            nativeQuery = true)
    Page<Chat> searchChats(@Param("videoId") String videoId,
                           @Param("keyword") String keyword,
                           @Param("username") String username,
                           Pageable pageable);

    /**
     * 정확 일치 검색. keyword/username 이 (대소문자 무시) 완전히 일치하는 채팅만 반환한다.
     */
    @Query(value = "SELECT * FROM chats " +
            "WHERE video_id = :videoId " +
            "AND (:keyword = '' OR lower(message) = lower(:keyword)) " +
            "AND (:username = '' OR lower(username) = lower(:username)) " +
            "ORDER BY player_message_time ASC",
            countQuery = "SELECT count(*) FROM chats " +
                    "WHERE video_id = :videoId " +
                    "AND (:keyword = '' OR lower(message) = lower(:keyword)) " +
                    "AND (:username = '' OR lower(username) = lower(:username))",
            nativeQuery = true)
    Page<Chat> searchChatsExact(@Param("videoId") String videoId,
                                @Param("keyword") String keyword,
                                @Param("username") String username,
                                Pageable pageable);

    long countByVideoId(String videoId);

    @Query(value = "SELECT (player_message_time - (player_message_time % 60000)) AS minute_epoch, MIN(player_message_time) AS first_player_time, count(*) AS cnt FROM chats WHERE video_id = :videoId GROUP BY (player_message_time - (player_message_time % 60000)) ORDER BY cnt DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findPeakMinute(@Param("videoId") String videoId);

    @Query(value = "SELECT (player_message_time - (player_message_time % 60000)) AS minute_epoch, MIN(player_message_time) AS first_player_time, count(*) AS cnt FROM chats WHERE video_id = :videoId AND message ILIKE '%' || :keyword || '%' GROUP BY (player_message_time - (player_message_time % 60000)) ORDER BY cnt DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findKeywordPeakMinute(@Param("videoId") String videoId, @Param("keyword") String keyword);

    List<Chat> findByVideoId(String videoId);

    @Query(value = "SELECT player_message_time FROM chats WHERE video_id = :videoId AND player_message_time IN (:times)", nativeQuery = true)
    List<Long> findExistingMessageTimes(@Param("videoId") String videoId, @Param("times") List<Long> times);
}
