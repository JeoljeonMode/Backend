package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import com.example.capstone.domain.Patient;

public interface PatientStore {
    Patient save(Patient patient);
    Optional<Patient> findById(String id);
    List<Patient> findAll();
    List<Patient> findByActive(boolean active);
}
