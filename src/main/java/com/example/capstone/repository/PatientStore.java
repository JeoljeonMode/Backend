package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capstone.domain.Patient;

public interface PatientStore extends JpaRepository<Patient, String> {
    List<Patient> findByActive(boolean active);
    Optional<Patient> findByPatientNumber(String patientNumber);
}
