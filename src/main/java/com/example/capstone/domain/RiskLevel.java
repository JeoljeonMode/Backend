package com.example.capstone.domain;

public enum RiskLevel {
	NORMAL("정상"),
	CAUTION("주의"),
	DANGER("위험");

	private final String label;

	RiskLevel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public static RiskLevel fromScore(int score) {
		if (score >= 6) {
			return DANGER;
		}
		if (score >= 3) {
			return CAUTION;
		}
		return NORMAL;
	}
}
