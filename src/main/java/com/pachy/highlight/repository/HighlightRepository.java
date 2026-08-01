package com.pachy.highlight.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pachy.highlight.entity.Highlight;

import java.time.Instant;
import java.util.List;

public interface HighlightRepository extends JpaRepository<Highlight, Long> {
  List<Highlight> findAllByVideoIdOrderByChatCountDesc(String videoId);

  // 최근 생성된 하이라이트 (관리자 대시보드)
  List<Highlight> findAllByOrderByCreatedAtDesc(Pageable pageable);

  // 공개 홈용: 영상별 최신 하이라이트 1건씩, 최근 생성 순 (중복 영상 제거)
  @Query(value = "SELECT * FROM (" +
          "  SELECT DISTINCT ON (video_id) * FROM highlights ORDER BY video_id, created_at DESC" +
          ") t ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
  List<Highlight> findRecentDistinctVideoHighlights(@Param("limit") int limit);

  // 통계
  @Query(value = "SELECT count(DISTINCT video_id) FROM highlights", nativeQuery = true)
  long countDistinctVideos();

  @Query(value = "SELECT count(*) FROM highlights WHERE created_at >= :from", nativeQuery = true)
  long countCreatedSince(@Param("from") Instant from);

  // 일자별 하이라이트 생성 추이 (YYYY-MM-DD, count)
  @Query(value = "SELECT to_char(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS d, count(*) AS c " +
          "FROM highlights WHERE created_at >= :from " +
          "GROUP BY d ORDER BY d ASC", nativeQuery = true)
  List<Object[]> dailyCreatedCounts(@Param("from") Instant from);

  // 상위 영상 (하이라이트 개수 기준)
  @Query(value = "SELECT video_id, count(*) AS c, COALESCE(max(chat_count), 0) AS max_chat " +
          "FROM highlights GROUP BY video_id ORDER BY c DESC, max_chat DESC LIMIT :limit", nativeQuery = true)
  List<Object[]> topVideosByHighlightCount(@Param("limit") int limit);
}
