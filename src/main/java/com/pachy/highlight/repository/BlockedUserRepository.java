package com.pachy.highlight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pachy.highlight.entity.BlockedUser;

import java.util.List;
import java.util.Optional;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    List<BlockedUser> findAllByOrderByCreatedAtDesc();

    Optional<BlockedUser> findByUid(String uid);

    boolean existsByUid(String uid);

    /** 수집 필터용: 등록된 uid 전체 */
    @Query(value = "SELECT uid FROM blocked_users", nativeQuery = true)
    List<String> findAllUids();
}
