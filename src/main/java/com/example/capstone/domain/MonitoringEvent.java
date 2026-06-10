package com.example.capstone.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "monitoring_events")
public class MonitoringEvent {

	@Id
	private String id = UUID.randomUUID().toString();
	private Instant occurredAt = Instant.now();
	private String cameraId;
	private String bedId;
	private String patientName;
	private String patientNo;
	private String patientPosition;
	private String posture;
	private boolean guardrailUp;
	private boolean caregiverPresent;
	private int riskScore;

	@Enumerated(EnumType.STRING)
	private RiskLevel riskLevel;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "monitoring_event_risk_factors", joinColumns = @JoinColumn(name = "event_id"))
	@OrderColumn(name = "factor_order")
	@Column(name = "factor")
	private List<String> riskFactors = new ArrayList<>();

	private String summary;
	private String frameUrl;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "x", column = @Column(name = "roi_x")),
			@AttributeOverride(name = "y", column = @Column(name = "roi_y")),
			@AttributeOverride(name = "width", column = @Column(name = "roi_width")),
			@AttributeOverride(name = "height", column = @Column(name = "roi_height"))
	})
	private DetectionBox roi;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "x", column = @Column(name = "patient_box_x")),
			@AttributeOverride(name = "y", column = @Column(name = "patient_box_y")),
			@AttributeOverride(name = "width", column = @Column(name = "patient_box_width")),
			@AttributeOverride(name = "height", column = @Column(name = "patient_box_height"))
	})
	private DetectionBox patientBox;

	private boolean acknowledged;
	private Instant acknowledgedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public String getCameraId() {
		return cameraId;
	}

	public void setCameraId(String cameraId) {
		this.cameraId = cameraId;
	}

	public String getBedId() {
		return bedId;
	}

	public void setBedId(String bedId) {
		this.bedId = bedId;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getPatientNo() {
		return patientNo;
	}

	public void setPatientNo(String patientNo) {
		this.patientNo = patientNo;
	}

	public String getPatientPosition() {
		return patientPosition;
	}

	public void setPatientPosition(String patientPosition) {
		this.patientPosition = patientPosition;
	}

	public String getPosture() {
		return posture;
	}

	public void setPosture(String posture) {
		this.posture = posture;
	}

	public boolean isGuardrailUp() {
		return guardrailUp;
	}

	public void setGuardrailUp(boolean guardrailUp) {
		this.guardrailUp = guardrailUp;
	}

	public boolean isCaregiverPresent() {
		return caregiverPresent;
	}

	public void setCaregiverPresent(boolean caregiverPresent) {
		this.caregiverPresent = caregiverPresent;
	}

	public int getRiskScore() {
		return riskScore;
	}

	public void setRiskScore(int riskScore) {
		this.riskScore = riskScore;
	}

	public RiskLevel getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(RiskLevel riskLevel) {
		this.riskLevel = riskLevel;
	}

	public List<String> getRiskFactors() {
		return riskFactors;
	}

	public void setRiskFactors(List<String> riskFactors) {
		this.riskFactors = riskFactors;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getFrameUrl() {
		return frameUrl;
	}

	public void setFrameUrl(String frameUrl) {
		this.frameUrl = frameUrl;
	}

	public DetectionBox getRoi() {
		return roi;
	}

	public void setRoi(DetectionBox roi) {
		this.roi = roi;
	}

	public DetectionBox getPatientBox() {
		return patientBox;
	}

	public void setPatientBox(DetectionBox patientBox) {
		this.patientBox = patientBox;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	public Instant getAcknowledgedAt() {
		return acknowledgedAt;
	}

	public void setAcknowledgedAt(Instant acknowledgedAt) {
		this.acknowledgedAt = acknowledgedAt;
	}

	@Embeddable
	public record DetectionBox(double x, double y, double width, double height) {
	}
}
