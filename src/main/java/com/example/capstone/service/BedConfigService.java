package com.example.capstone.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.capstone.domain.BedConfig;
import com.example.capstone.dto.BedConfigRequest;
import com.example.capstone.repository.BedConfigStore;

@Service
public class BedConfigService {

    private final BedConfigStore bedConfigStore;

    public BedConfigService(BedConfigStore bedConfigStore) {
        this.bedConfigStore = bedConfigStore;
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
}
