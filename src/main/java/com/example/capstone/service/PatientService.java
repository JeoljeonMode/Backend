package com.example.capstone.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.capstone.domain.Patient;
import com.example.capstone.dto.PatientRequest;
import com.example.capstone.repository.PatientStore;

@Service
public class PatientService {

    private final PatientStore patientStore;

    public PatientService(PatientStore patientStore) {
        this.patientStore = patientStore;
    }

    public List<Patient> getPatients(Boolean active) {
        if (active != null) {
            return patientStore.findByActive(active);
        }
        return patientStore.findAll();
    }

    public Patient getPatient(String id) {
        return patientStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("환자를 찾을 수 없습니다: " + id));
    }

    public Patient createPatient(PatientRequest request) {
        Patient patient = new Patient();
        applyRequest(patient, request);
        return patientStore.save(patient);
    }

    public Patient updatePatient(String id, PatientRequest request) {
        Patient patient = getPatient(id);
        applyRequest(patient, request);
        return patientStore.save(patient);
    }

    public Patient deletePatient(String id) {
        Patient patient = getPatient(id);
        patient.setActive(false);
        return patientStore.save(patient);
    }

    private void applyRequest(Patient patient, PatientRequest req) {
        if (req.patientNumber() != null) patient.setPatientNumber(req.patientNumber());
        if (req.name() != null) patient.setName(req.name());
        patient.setAge(req.age());
        if (req.gender() != null) patient.setGender(req.gender());
        if (req.bedId() != null) patient.setBedId(req.bedId());
        if (req.diagnosis() != null) patient.setDiagnosis(req.diagnosis());
        if (req.notes() != null) patient.setNotes(req.notes());
    }
}
