package com.example.capstone.config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.capstone.domain.User;
import com.example.capstone.repository.UserStore;
import com.example.capstone.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final long CACHE_TTL_SECONDS = 60;

    private record CachedUser(Optional<User> user, Instant expiresAt) {}

    private final Map<String, CachedUser> userCache = new ConcurrentHashMap<>();

    private final JwtService jwtService;
    private final UserStore userStore;

    public JwtAuthFilter(JwtService jwtService, UserStore userStore) {
        this.jwtService = jwtService;
        this.userStore = userStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (supportsTokenQueryParam(request.getServletPath())) {
            token = request.getParameter("token");
        }
        if (token != null && jwtService.isTokenValid(token)) {
            String username = jwtService.extractUsername(token);
            lookupUser(username)
                    .filter(User::isActive)
                    .ifPresent(user -> {
                        String role = normalizeRole(user.getRole());
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        filterChain.doFilter(request, response);
    }

    private Optional<User> lookupUser(String username) {
        Instant now = Instant.now();
        CachedUser cached = userCache.get(username);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.user();
        }
        Optional<User> user = userStore.findByUsername(username);
        userCache.put(username, new CachedUser(user, now.plusSeconds(CACHE_TTL_SECONDS)));
        return user;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "STAFF";
        }
        return role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
    }

    private boolean supportsTokenQueryParam(String path) {
        return path.startsWith("/sse/") || "/api/video-stream".equals(path) || "/api/ai/video-stream".equals(path);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/api/auth/login".equals(path);
    }
}
