package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.capstone.domain.User;

@Repository
@Profile("!mongo")
public class InMemoryUserStore implements UserStore {

    private final List<User> users = new CopyOnWriteArrayList<>();

    @Override
    public User save(User user) {
        users.removeIf(u -> u.getId().equals(user.getId()));
        users.add(user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users);
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.stream().anyMatch(u -> u.getUsername().equals(username));
    }
}
