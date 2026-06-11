package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.capstone.domain.MonitoringEvent;
import com.example.capstone.domain.RiskLevel;

public interface EventStore extends JpaRepository<MonitoringEvent, String> {

	Optional<MonitoringEvent> findFirstByOrderByOccurredAtDescIdDesc();

	default Optional<MonitoringEvent> findLatest() {
		return findFirstByOrderByOccurredAtDescIdDesc();
	}

	List<MonitoringEvent> findByOrderByOccurredAtDescIdDesc(Pageable pageable);

	default List<MonitoringEvent> findRecent(int limit) {
		return findByOrderByOccurredAtDescIdDesc(PageRequest.of(0, limit));
	}

	@Query("""
			SELECT e FROM MonitoringEvent e
			WHERE (:bedId IS NULL OR e.bedId = :bedId)
			  AND (:riskLevel IS NULL OR e.riskLevel = :riskLevel)
			  AND (:acknowledged IS NULL OR e.acknowledged = :acknowledged)
			ORDER BY e.occurredAt DESC, e.id DESC
			""")
	List<MonitoringEvent> findRecent(@Param("bedId") String bedId,
			@Param("riskLevel") RiskLevel riskLevel,
			@Param("acknowledged") Boolean acknowledged,
			Pageable pageable);

	default List<MonitoringEvent> findRecent(String bedId, RiskLevel riskLevel, Boolean acknowledged, int limit) {
		return findRecent(bedId, riskLevel, acknowledged, PageRequest.of(0, limit));
	}
}
