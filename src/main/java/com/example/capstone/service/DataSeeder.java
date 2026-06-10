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
        if (bedConfigStore.existsByBedId("B-101")) {
            return;
        }
        // 101호 병실 (남자, 정원 3, CAM-01)
        createBedConfig("B-101", "CAM-01", "101호 병실");
        createBedConfig("B-102", "CAM-01", "101호 병실");
        createBedConfig("B-103", "CAM-01", "101호 병실");
        // 102호 병실 (남자, 정원 3, CAM-02)
        createBedConfig("B-104", "CAM-02", "102호 병실");
        createBedConfig("B-105", "CAM-02", "102호 병실");
        createBedConfig("B-106", "CAM-02", "102호 병실");
        // 103호 병실 (남자, 정원 4, CAM-03)
        createBedConfig("B-107", "CAM-03", "103호 병실");
        createBedConfig("B-108", "CAM-03", "103호 병실");
        createBedConfig("B-109", "CAM-03", "103호 병실");
        createBedConfig("B-110", "CAM-03", "103호 병실");
        // 201호 병실 (여자, 정원 3, CAM-04)
        createBedConfig("B-201", "CAM-04", "201호 병실");
        createBedConfig("B-202", "CAM-04", "201호 병실");
        createBedConfig("B-203", "CAM-04", "201호 병실");
        // 202호 병실 (여자, 정원 2, CAM-05)
        createBedConfig("B-204", "CAM-05", "202호 병실");
        createBedConfig("B-205", "CAM-05", "202호 병실");
        log.info("[시드] 병상 설정 15개 생성 완료");
    }

    private void seedPatients() {
        if (!patientStore.findAll().isEmpty()) {
            return;
        }
        createPatient("24-1011", "김철수", 72, "MALE",   "B-101", "당뇨, 고혈압");
        createPatient("24-1012", "이영희", 68, "FEMALE", "B-102", "골절");
        createPatient("24-1013", "박민준", 81, "MALE",   "B-103", "치매, 파킨슨병");
        createPatient("24-1014", "최수진", 75, "FEMALE", "B-104", "고혈압, 심부전");
        createPatient("24-1015", "정우성", 77, "MALE",   "B-105", "골다공증");
        createPatient("24-1016", "강민호", 83, "MALE",   "B-106", "치매");
        createPatient("24-1017", "조현우", 79, "MALE",   "B-107", "뇌졸중");
        createPatient("24-1018", "윤미래", 65, "FEMALE", "B-108", "폐렴");
        createPatient("24-2011", "김유진", 70, "FEMALE", "B-201", "골절, 고혈압");
        createPatient("24-2012", "이서연", 74, "FEMALE", "B-202", "당뇨");
        log.info("[시드] 샘플 환자 10명 생성 완료");
    }

    private void createBedConfig(String bedId, String cameraId, String location) {
        BedConfig config = new BedConfig();
        config.setBedId(bedId);
        config.setCameraId(cameraId);
        config.setLocation(location);
        bedConfigStore.save(config);
    }

    private void createPatient(String number, String name, int age, String gender, String bedId, String diagnosis) {
        Patient p = new Patient();
        p.setPatientNumber(number);
        p.setName(name);
        p.setAge(age);
        p.setGender(gender);
        p.setBedId(bedId);
        p.setDiagnosis(diagnosis);
        patientStore.save(p);
    }
}
