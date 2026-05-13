package com.example.capstone.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.example.capstone.domain.MonitoringEvent;
import com.example.capstone.domain.RiskLevel;

@Repository
@Profile("mongo")
public class MongoEventStore implements EventStore {

	private final MongoTemplate mongoTemplate;

	public MongoEventStore(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public MonitoringEvent save(MonitoringEvent event) {
		return mongoTemplate.save(event);
	}

	@Override
	public Optional<MonitoringEvent> findLatest() {
		Query query = new Query().with(Sort.by(Sort.Direction.DESC, "occurredAt")).limit(1);
		return Optional.ofNullable(mongoTemplate.findOne(query, MonitoringEvent.class));
	}

	@Override
	public List<MonitoringEvent> findRecent(int limit) {
		Query query = new Query().with(Sort.by(Sort.Direction.DESC, "occurredAt")).limit(limit);
		return mongoTemplate.find(query, MonitoringEvent.class);
	}

	@Override
	public Optional<MonitoringEvent> findById(String id) {
		return Optional.ofNullable(mongoTemplate.findById(id, MonitoringEvent.class));
	}

	@Override
	public List<MonitoringEvent> findRecent(String bedId, RiskLevel riskLevel, Boolean acknowledged, int limit) {
		Query query = new Query().with(Sort.by(Sort.Direction.DESC, "occurredAt")).limit(limit);
		if (bedId != null && !bedId.isBlank()) {
			query.addCriteria(Criteria.where("bedId").is(bedId));
		}
		if (riskLevel != null) {
			query.addCriteria(Criteria.where("riskLevel").is(riskLevel));
		}
		if (acknowledged != null) {
			query.addCriteria(Criteria.where("acknowledged").is(acknowledged));
		}
		return mongoTemplate.find(query, MonitoringEvent.class);
	}
}
