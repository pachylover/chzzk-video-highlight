package com.pachy.highlight.repository;

import com.pachy.highlight.entity.Chat;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatBatchInsertRepository {

    private static final String INSERT_SQL = """
            INSERT INTO chats (video_id, user_id, username, message, player_message_time, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public int insertBatch(List<Chat> chats) {
        if (chats == null || chats.isEmpty()) {
            return 0;
        }

        int[][] counts = jdbcTemplate.batchUpdate(INSERT_SQL, chats, chats.size(), this::bind);

        int total = 0;
        for (int[] batchCounts : counts) {
            for (int count : batchCounts) {
                if (count > 0) {
                    total += count;
                }
            }
        }
        return total;
    }

    private void bind(PreparedStatement ps, Chat chat) throws SQLException {
        ps.setString(1, chat.getVideoId());
        setNullableString(ps, 2, chat.getUserId());
        setNullableString(ps, 3, chat.getUsername());
        setNullableString(ps, 4, chat.getMessage());

        if (chat.getPlayerMessageTime() == null) {
            ps.setNull(5, Types.BIGINT);
        } else {
            ps.setLong(5, chat.getPlayerMessageTime());
        }

        Instant createdAt = chat.getCreatedAt() != null ? chat.getCreatedAt() : Instant.now();
        ps.setObject(6, OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC));
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }
}