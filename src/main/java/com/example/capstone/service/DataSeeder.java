package com.example.capstone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.capstone.domain.BedConfig;
import com.example.capstone.domain.Patient;
import com.example.capstone.domain.User;
import com.example.capstone.repository.BedConfigStore;
import com.example.capstone.repository.PatientStore;
import com.example.capstone.repository.UserStore;

@Service
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    private final UserStore userStore;
    private final BedConfigStore bedConfigStore;
    private final PatientStore patientStore;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserStore userStore, BedConfigStore bedConfigStore, PatientStore patientStore, PasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.bedConfigStore = bedConfigStore;
        this.patientStore = patientStore;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedBedConfigs();
        seedPatients();
    }

    private void seedAdmin() {
        if (userStore.existsByUsername("admin")) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setDisplayName("관리자");
        userStore.save(admin);
        log.info("[시드] admin 계정 생성 완료");
    }

    private void seedBedConfigs() {
        if (bedConfigStore.existsByBedId("BED-01")) {
            return;
        }
        BedConfig bed1 = new BedConfig();
        bed1.setBedId("BED-01");
        bed1.setCameraId("CAM-01");
        bed1.setLocation("A병동 301호");
        bedConfigStore.save(bed1);

        BedConfig bed2 = new BedConfig();
        bed2.setBedId("BED-02");
        bed2.setCameraId("CAM-02");
        bed2.setLocation("A병동 302호");
        bedConfigStore.save(bed2);

        log.info("[시드] 병상 설정 BED-01, BED-02 생성 완료");
    }

    private void seedPatients() {
        if (!patientStore.findAll().isEmpty()) {
            return;
        }
        Patient p1 = new Patient();
        p1.setPatientNumber("P-001");
        p1.setName("김인하");
        p1.setAge(78);
        p1.setGender("FEMALE");
        p1.setBedId("BED-01");
        p1.setDiagnosis("치매, 고혈압");
        patientStore.save(p1);

        Patient p2 = new Patient();
        p2.setPatientNumber("P-002");
        p2.setName("박하늘");
        p2.setAge(82);
        p2.setGender("MALE");
        p2.setBedId("BED-02");
        p2.setDiagnosis("파킨슨병");
        patientStore.save(p2);

        log.info("[시드] 샘플 환자 2명 생성 완료");
    }
}
