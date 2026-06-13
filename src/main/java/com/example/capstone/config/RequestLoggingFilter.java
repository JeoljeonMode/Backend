package com.example.capstone.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 모든 HTTP 요청/응답을 누가, 무엇을, 어떻게 호출했는지 추적할 수 있도록 상세히 로깅한다.
 * (디버깅/임시 운영용 - 응답 바디까지 로그에 남기므로 운영 보안 요구사항이 있는 환경에서는 사용하지 말 것)
 *
 * SecurityConfig에서 직접 인스턴스화하여 Security 필터체인에만 등록한다.
 * (@Component로 선언하면 서블릿 컨테이너 필터로도 중복 등록되어 두 번 실행된다)
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("HTTP_TRACE");
    private static final int MAX_PAYLOAD_LENGTH = 4000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // SSE 같은 장시간 스트리밍 응답은 바디 캐싱 시 깨질 수 있으므로 요청 정보만 가볍게 로깅한다.
        if (request.getRequestURI().startsWith("/sse/")) {
            log.info(">>> {} {} | ip={} | user={}",
                    request.getMethod(), request.getRequestURI(), clientIp(request), currentUser());
            filterChain.doFilter(request, response);
            return;
        }

        String reqId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("reqId", reqId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        String query = request.getQueryString();

        log.info(">>> {} {}{} | ip={} | user-agent={}",
                request.getMethod(),
                request.getRequestURI(),
                query != null ? "?" + query : "",
                clientIp(request),
                request.getHeader("User-Agent"));

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String user = currentUser();
            int status = wrappedResponse.getStatus();
            String reqBody = readPayload(wrappedRequest.getContentAsByteArray(), request.getCharacterEncoding());
            String resBody = readPayload(wrappedResponse.getContentAsByteArray(), response.getCharacterEncoding());

            String message = "<<< {} {} | status={} | user={} | durationMs={} | reqBody={} | resBody={}";
            Object[] args = {request.getMethod(), request.getRequestURI(), status, user, duration, reqBody, resBody};

            if (status >= 500) {
                log.error(message, args);
            } else if (status >= 400) {
                log.warn(message, args);
            } else {
                log.info(message, args);
            }

            wrappedResponse.copyBodyToResponse();
            MDC.remove("reqId");
        }
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonymous";
        }
        return auth.getName();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String readPayload(byte[] buf, String encoding) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        String charset = encoding != null ? encoding : "UTF-8";
        try {
            int len = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
            String content = new String(buf, 0, len, charset);
            return buf.length > MAX_PAYLOAD_LENGTH ? content + "...(truncated)" : content;
        } catch (Exception e) {
            return "(unreadable payload)";
        }
    }
}
