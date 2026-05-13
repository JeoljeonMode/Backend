package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import com.example.capstone.domain.RiskLevel;
import com.example.capstone.domain.MonitoringEvent;

public interface EventStore {

	MonitoringEvent save(MonitoringEvent event);

	Optional<MonitoringEvent> findLatest();

	List<MonitoringEvent> findRecent(int limit);

	Optional<MonitoringEvent> findById(String id);

	List<MonitoringEvent> findRecent(String bedId, RiskLevel riskLevel, Boolean acknowledged, int limit);
}
