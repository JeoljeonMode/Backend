package com.example.capstone.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.capstone.domain.RiskLevel;
import com.example.capstone.dto.AiEventRequest;

class RiskAssessmentServiceTest {

	private final RiskAssessmentService service = new RiskAssessmentService();

	@Test
	void calculatesDangerWhenMultipleRiskFactorsArePresent() {
		var result = service.assess(new AiEventRequest(
				null, "CAM-01", "BED-01", "김환자", null, "right_edge", "exit_attempt", false, false, null, null, null));

		assertThat(result.score()).isEqualTo(9);
		assertThat(result.level()).isEqualTo(RiskLevel.DANGER);
		assertThat(result.factors()).hasSize(4);
	}

	@Test
	void calculatesNormalWhenNoRiskFactorsArePresent() {
		var result = service.assess(new AiEventRequest(
				null, "CAM-01", "BED-01", "김환자", null, "center", "lying", true, true, null, null, null));

		assertThat(result.score()).isZero();
		assertThat(result.level()).isEqualTo(RiskLevel.NORMAL);
	}
}
