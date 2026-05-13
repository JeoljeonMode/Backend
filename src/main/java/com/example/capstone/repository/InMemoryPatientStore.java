package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.capstone.domain.Patient;

@Repository
@Profile("!mongo")
public class InMemoryPatientStore implements PatientStore {

    private final List<Patient> patients = new CopyOnWriteArrayList<>();

    @Override
    public Patient save(Patient patient) {
        patients.removeIf(p -> p.getId().equals(patient.getId()));
        patients.add(patient);
        return patient;
    }

    @Override
    public Optional<Patient> findById(String id) {
        return patients.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    @Override
    public List<Patient> findAll() {
        return List.copyOf(patients);
    }

    @Override
    public List<Patient> findByActive(boolean active) {
        return patients.stream().filter(p -> p.isActive() == active).toList();
    }
}
