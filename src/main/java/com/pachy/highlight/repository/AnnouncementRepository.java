package com.pachy.highlight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pachy.highlight.entity.Announcement;

import java.time.Instant;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findAllByOrderByIdDesc();

    // 공개용: 활성 상태 + 노출 기간에 해당하는 안내문구
    @Query(value = "SELECT * FROM announcements a " +
            "WHERE a.is_active = true " +
            "AND (a.starts_at IS NULL OR a.starts_at <= :now) " +
            "AND (a.ends_at IS NULL OR a.ends_at >= :now) " +
            "ORDER BY a.id DESC", nativeQuery = true)
    List<Announcement> findActiveAnnouncements(Instant now);
}
