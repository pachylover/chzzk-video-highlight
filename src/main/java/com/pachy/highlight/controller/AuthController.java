package com.pachy.highlight.controller;

import com.pachy.highlight.dto.admin.LoginRequest;
import com.pachy.highlight.dto.admin.LoginResponse;
import com.pachy.highlight.dto.response.Response;
import com.pachy.highlight.entity.AdminUser;
import com.pachy.highlight.repository.AdminUserRepository;
import com.pachy.highlight.security.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AdminUser user = adminUserRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            Response err = new Response(HttpStatus.UNAUTHORIZED);
            err.setResultMsg("아이디 또는 비밀번호가 올바르지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getRole()));
    }

    // 토큰 유효성 확인 (관리자 페이지 가드용)
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(HttpStatus.UNAUTHORIZED));
        }
        return ResponseEntity.ok(Map.of(
                "resultCode", 200,
                "resultMsg", "OK",
                "username", authentication.getName(),
                "role", authentication.getAuthorities().stream().findFirst()
                        .map(Object::toString).orElse("ROLE_ADMIN")
        ));
    }
}
