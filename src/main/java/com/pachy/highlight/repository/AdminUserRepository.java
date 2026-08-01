package com.pachy.highlight.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pachy.highlight.entity.AdminUser;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
