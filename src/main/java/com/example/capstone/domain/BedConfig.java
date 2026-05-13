package com.example.capstone.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "bed_configs")
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
