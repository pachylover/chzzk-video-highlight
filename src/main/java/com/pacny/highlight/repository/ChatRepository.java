package com.pacny.highlight.repository;

import com.pacny.highlight.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query(value = "SELECT time_bucket('1 minute', ts) AS minute, count(*) AS cnt FROM chats WHERE video_id = :videoId GROUP BY minute ORDER BY cnt DESC LIMIT 1", nativeQuery = true)
    Object[] findPeakMinute(@Param("videoId") String videoId);

    List<Chat> findByVideoId(String videoId);

    @Query(value = "SELECT message_time FROM chats WHERE video_id = :videoId AND message_time IN (:times)", nativeQuery = true)
    List<Long> findExistingMessageTimes(@Param("videoId") String videoId, @Param("times") List<Long> times);
}
