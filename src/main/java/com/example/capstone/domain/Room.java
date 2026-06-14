package com.example.capstone.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    private String id = UUID.randomUUID().toString();
    private String roomId;
    private String label;
    private String cameraId;
    private String gender;
    private int capacity;
    private boolean cameraEnabled;
}
