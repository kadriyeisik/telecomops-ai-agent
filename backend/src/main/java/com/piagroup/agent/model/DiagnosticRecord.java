package com.piagroup.agent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persists a diagnostic snapshot each time suggest_resolution runs.
 * Enables the case_history_check tool to compare previous vs. current
 * readings and surface trends (e.g. degrading signal, rising station load).
 */
@Entity
@Table(name = "diagnostic_records")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;
    private String customerName;

    /** Classified issue type from intent_analysis (e.g. SLOW_DATA, NO_SIGNAL) */
    private String issueType;

    /** Signal reading at the time of diagnosis */
    private Integer signalDbm;
    private String  signalQuality;  // EXCELLENT / GOOD / FAIR / POOR
    private String  technology;     // 5G / 4G+ / 4G

    /** Base station reading at the time of diagnosis */
    private Integer stationLoadPercent;
    private String  stationStatus;  // NORMAL / MODERATE / HIGH_LOAD / CRITICAL

    /** Data usage percentage (null for unlimited plans) */
    private Double dataUsedPercent;

    /** Condensed root cause from the resolution report */
    @Column(length = 1000)
    private String rootCause;

    /** Estimated resolution time from the resolution report */
    private String estimatedResolution;

    private Instant createdAt = Instant.now();
}
