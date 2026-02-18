package com.pachy.highlight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pachy.highlight.entity.Chat;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query(value = "SELECT (player_message_time - (player_message_time % 60000)) AS minute_epoch, MIN(player_message_time) AS first_player_time, count(*) AS cnt FROM chats WHERE video_id = :videoId GROUP BY (player_message_time - (player_message_time % 60000)) ORDER BY cnt DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findPeakMinute(@Param("videoId") String videoId);

    List<Chat> findByVideoId(String videoId);

    @Query(value = "SELECT player_message_time FROM chats WHERE video_id = :videoId AND player_message_time IN (:times)", nativeQuery = true)
    List<Long> findExistingMessageTimes(@Param("videoId") String videoId, @Param("times") List<Long> times);
}
