package com.pachy.highlight.exception;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationError(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errors", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(violation -> Map.of("path", violation.getPropertyPath().toString(), "message", violation.getMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("errors", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        // 예외를 반드시 남긴다. 이전에는 아무 것도 기록하지 않고 일반 메시지만 돌려줘서
        // 앱에서 무슨 일이 나든 로그에 흔적이 남지 않았다.
        log.error("처리되지 않은 예외 - {} {}", request.getMethod(), request.getRequestURI(), ex);

        // SSE(text/event-stream) 처럼 이미 응답이 시작된 요청에는 JSON 본문을 쓸 수 없다.
        // 억지로 쓰면 HttpMessageNotWritableException 이 나면서 원래 예외가 가려진다.
        if (response.isCommitted()
                || MediaType.TEXT_EVENT_STREAM_VALUE.equals(response.getContentType())) {
            return null;
        }

        // 상세 내용은 클라이언트에 노출하지 않는다
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("error", "internal_server_error", "message", "Internal server error"));
    }
}
