package com.pachy.highlight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pachy.highlight.entity.Banner;

import java.time.Instant;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderBySortOrderAscIdAsc();

    // 공개용: 활성 상태 + 노출 기간에 해당하는 배너
    @Query(value = "SELECT * FROM banners b " +
            "WHERE b.is_active = true " +
            "AND (b.starts_at IS NULL OR b.starts_at <= :now) " +
            "AND (b.ends_at IS NULL OR b.ends_at >= :now) " +
            "ORDER BY b.sort_order ASC, b.id ASC", nativeQuery = true)
    List<Banner> findActiveBanners(Instant now);
}
