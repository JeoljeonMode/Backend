package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import com.example.capstone.domain.User;

public interface UserStore {
    User save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);
    List<User> findAll();
    boolean existsByUsername(String username);
}
