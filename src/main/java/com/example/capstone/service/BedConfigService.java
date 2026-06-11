package com.example.capstone.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.capstone.domain.BedConfig;
import com.example.capstone.domain.Patient;
import com.example.capstone.domain.Room;
import com.example.capstone.dto.BedCreateRequest;
import com.example.capstone.dto.BedCreateResponse;
import com.example.capstone.dto.BedConfigRequest;
import com.example.capstone.repository.BedConfigStore;
import com.example.capstone.repository.PatientStore;
import com.example.capstone.repository.RoomStore;

@Service
public class BedConfigService {

    private final BedConfigStore bedConfigStore;
    private final RoomStore roomStore;
    private final PatientStore patientStore;

    public BedConfigService(BedConfigStore bedConfigStore, RoomStore roomStore, PatientStore patientStore) {
        this.bedConfigStore = bedConfigStore;
        this.roomStore = roomStore;
        this.patientStore = patientStore;
    }

    public List<BedConfig> getAllBedConfigs() {
        return bedConfigStore.findAll();
    }

    public BedConfig getBedConfig(String id) {
        return bedConfigStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("병상 설정을 찾을 수 없습니다: " + id));
    }

    public BedConfig createBedConfig(BedConfigRequest request) {
        BedConfig config = new BedConfig();
        applyRequest(config, request);
        return bedConfigStore.save(config);
    }

    public BedCreateResponse createBed(BedCreateRequest request) {
        validateBedCreateRequest(request);
        if (bedConfigStore.existsByBedId(request.bedId())) {
            throw new IllegalArgumentException("이미 존재하는 병상 ID입니다.");
        }

        Room room = roomStore.findByRoomId(request.roomId())
                .orElseThrow(() -> new IllegalArgumentException("병실을 찾을 수 없습니다: " + request.roomId()));
        Patient patient = patientStore.findByPatientNumber(request.patientNo()).orElseGet(Patient::new);
        patient.setPatientNumber(request.patientNo());
        patient.setName(request.patientName());
        patient.setBedId(request.bedId());
        patient.setGender(toPatientGender(room.getGender()));
        Patient savedPatient = patientStore.save(patient);

        BedConfig config = new BedConfig();
        config.setBedId(request.bedId());
        config.setRoomId(room.getRoomId());
        config.setCameraId(room.getCameraId());
        config.setLocation(room.getLabel());
        config.setPatientId(savedPatient.getId());
        config.setPatientName(request.patientName());
        config.setPatientNo(request.patientNo());
        bedConfigStore.save(config);

        return new BedCreateResponse(config.getBedId(), config.getRoomId(), config.getPatientName(), config.getPatientNo());
    }

    public BedConfig updateBedConfig(String id, BedConfigRequest request) {
        BedConfig config = getBedConfig(id);
        applyRequest(config, request);
        return bedConfigStore.save(config);
    }

    public BedConfig deleteBedConfig(String id) {
        BedConfig config = getBedConfig(id);
        config.setActive(false);
        return bedConfigStore.save(config);
    }

    private void applyRequest(BedConfig config, BedConfigRequest req) {
        if (req.bedId() != null) config.setBedId(req.bedId());
        if (req.cameraId() != null) config.setCameraId(req.cameraId());
        if (req.location() != null) config.setLocation(req.location());
        config.setPatientId(req.patientId());
    }

    private void validateBedCreateRequest(BedCreateRequest request) {
        if (request.bedId() == null || request.bedId().isBlank()) {
            throw new IllegalArgumentException("병상 ID를 입력해 주세요.");
        }
        if (request.roomId() == null || request.roomId().isBlank()) {
            throw new IllegalArgumentException("병실을 선택해 주세요.");
        }
        if (request.patientName() == null || request.patientName().isBlank()) {
            throw new IllegalArgumentException("환자명을 입력해 주세요.");
        }
        if (request.patientNo() == null || request.patientNo().isBlank()) {
            throw new IllegalArgumentException("관리번호를 입력해 주세요.");
        }
    }

    private String toPatientGender(String roomGender) {
        if ("남자".equals(roomGender)) {
            return "MALE";
        }
        if ("여자".equals(roomGender)) {
            return "FEMALE";
        }
        return null;
    }
}
