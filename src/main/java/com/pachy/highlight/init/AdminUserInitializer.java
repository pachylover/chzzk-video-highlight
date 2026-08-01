package com.pachy.highlight.init;

import com.pachy.highlight.entity.AdminUser;
import com.pachy.highlight.repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 최초 관리자 계정을 환경변수 ADMIN_USERNAME / ADMIN_PASSWORD 로 생성한다.
 * 동일 username 이 이미 존재하면 아무것도 하지 않는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.username:${ADMIN_USERNAME:}}")
    private String username;

    @Value("${admin.bootstrap.password:${ADMIN_PASSWORD:}}")
    private String password;

    @Override
    public void run(String... args) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.info("관리자 부트스트랩 건너뜀 - ADMIN_USERNAME/ADMIN_PASSWORD 미설정");
            return;
        }
        if (adminUserRepository.existsByUsername(username)) {
            log.info("관리자 계정 이미 존재 - username: {}", username);
            return;
        }
        AdminUser admin = AdminUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role("ADMIN")
                .build();
        adminUserRepository.save(admin);
        log.info("관리자 계정 생성 완료 - username: {}", username);
    }
}
