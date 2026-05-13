package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.capstone.domain.BedConfig;

@Repository
@Profile("!mongo")
public class InMemoryBedConfigStore implements BedConfigStore {

    private final List<BedConfig> configs = new CopyOnWriteArrayList<>();

    @Override
    public BedConfig save(BedConfig bedConfig) {
        configs.removeIf(c -> c.getId().equals(bedConfig.getId()));
        configs.add(bedConfig);
        return bedConfig;
    }

    @Override
    public Optional<BedConfig> findById(String id) {
        return configs.stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<BedConfig> findByBedId(String bedId) {
        return configs.stream().filter(c -> c.getBedId().equals(bedId)).findFirst();
    }

    @Override
    public List<BedConfig> findAll() {
        return List.copyOf(configs);
    }

    @Override
    public List<BedConfig> findByActive(boolean active) {
        return configs.stream().filter(c -> c.isActive() == active).toList();
    }

    @Override
    public boolean existsByBedId(String bedId) {
        return configs.stream().anyMatch(c -> c.getBedId().equals(bedId));
    }
}
