package com.example.capstone.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "bed_configs")
public class BedConfig {

    @Id
    private String id = UUID.randomUUID().toString();
    private String bedId;
    private String cameraId;
    private String location;
    private String patientId;
    private boolean active = true;
    private Instant createdAt = Instant.now();
}
