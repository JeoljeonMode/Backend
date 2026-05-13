package com.example.capstone.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "patients")
public class Patient {

    @Id
    private String id = UUID.randomUUID().toString();
    private String patientNumber;
    private String name;
    private int age;
    private String gender; // MALE, FEMALE
    private String bedId;
    private String diagnosis;
    private String notes;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}
