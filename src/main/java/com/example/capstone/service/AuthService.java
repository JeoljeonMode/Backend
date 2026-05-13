package com.example.capstone.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.capstone.domain.User;
import com.example.capstone.dto.AuthResponse;
import com.example.capstone.dto.LoginRequest;
import com.example.capstone.dto.RegisterRequest;
import com.example.capstone.repository.UserStore;

@Service
public class AuthService {

    private final UserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserStore userStore, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userStore.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!user.isActive()) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getDisplayName());
    }

    public AuthResponse register(RegisterRequest request) {
        if (userStore.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(request.role() != null ? request.role() : "STAFF");
        userStore.save(user);
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getDisplayName());
    }

    public AuthResponse me(String username) {
        User user = userStore.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new AuthResponse(null, user.getUsername(), user.getRole(), user.getDisplayName());
    }
}
