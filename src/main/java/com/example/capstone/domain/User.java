package com.example.capstone.domain;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id = UUID.randomUUID().toString();
    private String username;
    private String password; // BCrypt hash
    private String role = "STAFF"; // STAFF, ADMIN
    private String displayName;
    private boolean active = true;
}
