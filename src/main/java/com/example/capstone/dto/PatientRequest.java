package com.example.capstone.dto;

public record PatientRequest(
        String patientNumber,
        String name,
        int age,
        String gender,
        String bedId,
        String diagnosis,
        String notes) {}
