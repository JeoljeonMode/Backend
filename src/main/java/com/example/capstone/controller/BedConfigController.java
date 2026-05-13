package com.example.capstone.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.capstone.domain.BedConfig;
import com.example.capstone.dto.BedConfigRequest;
import com.example.capstone.service.BedConfigService;

@RestController
@RequestMapping("/api/bed-configs")
public class BedConfigController {

    private final BedConfigService bedConfigService;

    public BedConfigController(BedConfigService bedConfigService) {
        this.bedConfigService = bedConfigService;
    }

    @GetMapping
    public List<BedConfig> getBedConfigs() {
        return bedConfigService.getAllBedConfigs();
    }

    @PostMapping
    public ResponseEntity<?> createBedConfig(@RequestBody BedConfigRequest request) {
        try {
            return ResponseEntity.ok(bedConfigService.createBedConfig(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBedConfig(@PathVariable String id, @RequestBody BedConfigRequest request) {
        try {
            return ResponseEntity.ok(bedConfigService.updateBedConfig(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBedConfig(@PathVariable String id) {
        try {
            return ResponseEntity.ok(bedConfigService.deleteBedConfig(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
