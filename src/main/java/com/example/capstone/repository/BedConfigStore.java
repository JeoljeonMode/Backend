package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.capstone.domain.BedConfig;

public interface BedConfigStore extends JpaRepository<BedConfig, String> {
    Optional<BedConfig> findByBedId(String bedId);
    List<BedConfig> findByActive(boolean active);
    boolean existsByBedId(String bedId);
}
