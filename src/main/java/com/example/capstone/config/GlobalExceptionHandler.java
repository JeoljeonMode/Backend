package com.example.capstone.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 컨트롤러에서 처리되지 않은 예외를 모두 잡아 스택트레이스와 요청 정보를 로그에 남긴다.
 * (디버깅/임시 운영용 - 예외 메시지/클래스명을 응답에도 그대로 노출한다)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("[예외 처리][잘못된 요청] status=400 method={} path={} user={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), currentUser(), e.getClass().getName(), e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException e, HttpServletRequest request) {
        log.warn("[예외 처리][응답 상태 예외] status={} method={} path={} user={} exception={} reason={}",
                e.getStatusCode().value(), request.getMethod(), request.getRequestURI(), currentUser(),
                e.getClass().getName(), e.getReason());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", e.getReason() != null ? e.getReason() : e.getMessage());
        body.put("exception", e.getClass().getName());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        log.error("[예외 처리][서버 오류] status=500 method={} path={} user={} exception={} message={}",
                request.getMethod(), request.getRequestURI(), currentUser(), e.getClass().getName(), e.getMessage(), e);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", e.getMessage());
        body.put("exception", e.getClass().getName());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonymous";
        }
        return auth.getName();
    }
}
