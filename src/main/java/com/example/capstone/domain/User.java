package com.example.capstone.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    private String id = UUID.randomUUID().toString();
    private String username;
    private String password; // BCrypt hash
    private String role = "STAFF"; // STAFF, ADMIN
    private String displayName;
    private boolean active = true;
}
