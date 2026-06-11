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
import com.example.capstone.domain.Room;
import com.example.capstone.domain.User;
import com.example.capstone.repository.BedConfigStore;
import com.example.capstone.repository.PatientStore;
import com.example.capstone.repository.RoomStore;
import com.example.capstone.repository.UserStore;

@Service
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    private final UserStore userStore;
    private final RoomStore roomStore;
    private final BedConfigStore bedConfigStore;
    private final PatientStore patientStore;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserStore userStore, RoomStore roomStore, BedConfigStore bedConfigStore,
            PatientStore patientStore, PasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.roomStore = roomStore;
        this.bedConfigStore = bedConfigStore;
        this.patientStore = patientStore;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedRooms();
        seedBedConfigs();
        seedPatients();
    }

    private void seedAdmin() {
        User admin = userStore.findByUsername("admin").orElseGet(User::new);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setDisplayName("관리자");
        userStore.save(admin);
        log.info("[시드] admin 계정 upsert 완료");
    }

    private void seedRooms() {
        upsertRoom("101호", "101호 병실", "CAM-01", "남자", 3);
        upsertRoom("102호", "102호 병실", "CAM-02", "남자", 3);
        upsertRoom("103호", "103호 병실", "CAM-03", "남자", 4);
        upsertRoom("201호", "201호 병실", "CAM-04", "여자", 3);
        upsertRoom("202호", "202호 병실", "CAM-05", "여자", 2);
        log.info("[시드] 병실 5개 upsert 완료");
    }

    private void seedBedConfigs() {
        // 101호 병실 (남자, 정원 3, CAM-01)
        upsertBedConfig("B-101", "CAM-01", "101호", "101호 병실", "김철수", "24-1011");
        upsertBedConfig("B-102", "CAM-01", "101호", "101호 병실", "박영호", "24-1012");
        upsertBedConfig("B-103", "CAM-01", "101호", "101호 병실", "이민준", "24-1013");
        // 102호 병실 (남자, 정원 3, CAM-02)
        upsertBedConfig("B-104", "CAM-02", "102호", "102호 병실", "최진혁", "24-1021");
        upsertBedConfig("B-105", "CAM-02", "102호", "102호 병실", "정우성", "24-1022");
        upsertBedConfig("B-106", "CAM-02", "102호", "102호 병실", "윤기준", "24-1023");
        // 103호 병실 (남자, 정원 4, CAM-03)
        upsertBedConfig("B-107", "CAM-03", "103호", "103호 병실", "강민호", "24-1031");
        upsertBedConfig("B-108", "CAM-03", "103호", "103호 병실", "한동훈", "24-1032");
        upsertBedConfig("B-109", "CAM-03", "103호", "103호 병실", "조재원", "24-1033");
        upsertBedConfig("B-110", "CAM-03", "103호", "103호 병실", "임성규", "24-1034");
        // 201호 병실 (여자, 정원 3, CAM-04)
        upsertBedConfig("B-201", "CAM-04", "201호", "201호 병실", "김지연", "24-2011");
        upsertBedConfig("B-202", "CAM-04", "201호", "201호 병실", "박서현", "24-2012");
        upsertBedConfig("B-203", "CAM-04", "201호", "201호 병실", "이수현", "24-2013");
        // 202호 병실 (여자, 정원 2, CAM-05)
        upsertBedConfig("B-204", "CAM-05", "202호", "202호 병실", "최민서", "24-2021");
        upsertBedConfig("B-205", "CAM-05", "202호", "202호 병실", "정지현", "24-2022");
        log.info("[시드] 병상 설정 15개 upsert 완료");
    }

    private void seedPatients() {
        upsertPatient("24-1011", "김철수", "MALE", "B-101");
        upsertPatient("24-1012", "박영호", "MALE", "B-102");
        upsertPatient("24-1013", "이민준", "MALE", "B-103");
        upsertPatient("24-1021", "최진혁", "MALE", "B-104");
        upsertPatient("24-1022", "정우성", "MALE", "B-105");
        upsertPatient("24-1023", "윤기준", "MALE", "B-106");
        upsertPatient("24-1031", "강민호", "MALE", "B-107");
        upsertPatient("24-1032", "한동훈", "MALE", "B-108");
        upsertPatient("24-1033", "조재원", "MALE", "B-109");
        upsertPatient("24-1034", "임성규", "MALE", "B-110");
        upsertPatient("24-2011", "김지연", "FEMALE", "B-201");
        upsertPatient("24-2012", "박서현", "FEMALE", "B-202");
        upsertPatient("24-2013", "이수현", "FEMALE", "B-203");
        upsertPatient("24-2021", "최민서", "FEMALE", "B-204");
        upsertPatient("24-2022", "정지현", "FEMALE", "B-205");
        log.info("[시드] 환자 15명 upsert 완료");
    }

    private void upsertRoom(String roomId, String label, String cameraId, String gender, int capacity) {
        Room room = roomStore.findByRoomId(roomId).orElseGet(Room::new);
        room.setRoomId(roomId);
        room.setLabel(label);
        room.setCameraId(cameraId);
        room.setGender(gender);
        room.setCapacity(capacity);
        roomStore.save(room);
    }

    private void upsertBedConfig(String bedId, String cameraId, String roomId, String location,
            String patientName, String patientNo) {
        BedConfig config = bedConfigStore.findByBedId(bedId).orElseGet(BedConfig::new);
        config.setBedId(bedId);
        config.setCameraId(cameraId);
        config.setRoomId(roomId);
        config.setLocation(location);
        config.setPatientName(patientName);
        config.setPatientNo(patientNo);
        bedConfigStore.save(config);
    }

    private void upsertPatient(String patientNumber, String name, String gender, String bedId) {
        Patient patient = patientStore.findByPatientNumber(patientNumber).orElseGet(Patient::new);
        patient.setPatientNumber(patientNumber);
        patient.setName(name);
        patient.setGender(gender);
        patient.setBedId(bedId);
        if (patient.getAge() == 0) {
            patient.setAge(70);
        }
        patientStore.save(patient);
    }
}
