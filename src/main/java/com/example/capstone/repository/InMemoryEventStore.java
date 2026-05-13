package com.example.capstone.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.capstone.domain.MonitoringEvent;
import com.example.capstone.domain.RiskLevel;

@Repository
@Profile("!mongo")
public class InMemoryEventStore implements EventStore {

	private final List<MonitoringEvent> events = new CopyOnWriteArrayList<>();

	@Override
	public MonitoringEvent save(MonitoringEvent event) {
		events.removeIf(existing -> existing.getId().equals(event.getId()));
		events.add(event);
		return event;
	}

	@Override
	public Optional<MonitoringEvent> findLatest() {
		return events.stream().max(Comparator.comparing(MonitoringEvent::getOccurredAt));
	}

	@Override
	public List<MonitoringEvent> findRecent(int limit) {
		return events.stream()
				.sorted(Comparator.comparing(MonitoringEvent::getOccurredAt).reversed())
				.limit(limit)
				.toList();
	}

	@Override
	public Optional<MonitoringEvent> findById(String id) {
		return events.stream()
				.filter(event -> event.getId().equals(id))
				.findFirst();
	}

	@Override
	public List<MonitoringEvent> findRecent(String bedId, RiskLevel riskLevel, Boolean acknowledged, int limit) {
		return events.stream()
				.filter(event -> bedId == null || bedId.isBlank() || bedId.equals(event.getBedId()))
				.filter(event -> riskLevel == null || riskLevel == event.getRiskLevel())
				.filter(event -> acknowledged == null || acknowledged == event.isAcknowledged())
				.sorted(Comparator.comparing(MonitoringEvent::getOccurredAt).reversed())
				.limit(limit)
				.toList();
	}
}
