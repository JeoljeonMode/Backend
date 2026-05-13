package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import com.example.capstone.domain.BedConfig;

public interface BedConfigStore {
    BedConfig save(BedConfig bedConfig);
    Optional<BedConfig> findById(String id);
    Optional<BedConfig> findByBedId(String bedId);
    List<BedConfig> findAll();
    List<BedConfig> findByActive(boolean active);
    boolean existsByBedId(String bedId);
}
