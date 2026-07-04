package com.piagroup.agent.data;

import com.piagroup.agent.model.DiagnosticRecord;
import com.piagroup.agent.repository.DiagnosticRecordRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seeds realistic historical diagnostic records at startup so the
 * case_history_check tool can demonstrate trend analysis on the very first run.
 *
 * Skips seeding if records already exist (idempotent across restarts).
 *
 * Demo scenarios:
 *  - David Wilson (05325551234): recurring SLOW_DATA with worsening signal &
 *    rising station load → triggers escalation recommendation
 *  - Bob Smith (05329876543): previous SLOW_DATA caused by quota exhaustion
 *  - Alice Johnson (05321234567): single past CALL_DROPS, resolved
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final DiagnosticRecordRepository repo;

    public DataInitializer(DiagnosticRecordRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;   // already seeded

        Instant now = Instant.now();

        // ── David Wilson (05325551234) — recurring SLOW_DATA, degrading network ──
        // 3 days ago: first complaint, signal still reasonable, station moderate
        repo.save(record(
            "05325551234", "David Wilson", "SLOW_DATA",
            -80, "FAIR", "5G", 68, "MODERATE", null,
            "Moderate base station load causing minor speed degradation",
            "1–2 hours (off-peak expected)",
            now.minus(3, ChronoUnit.DAYS)
        ));

        // 2 days ago: second complaint, signal weaker, station under higher load
        repo.save(record(
            "05325551234", "David Wilson", "SLOW_DATA",
            -84, "FAIR", "5G", 79, "HIGH_LOAD", null,
            "Increasing station congestion on IST-ASI-3318",
            "2–4 hours (congestion-dependent)",
            now.minus(2, ChronoUnit.DAYS)
        ));

        // 1 day ago: third complaint, situation worsening
        repo.save(record(
            "05325551234", "David Wilson", "SLOW_DATA",
            -86, "FAIR", "5G", 85, "HIGH_LOAD", null,
            "Persistent high load on IST-ASI-3318; standard self-service steps not resolving issue",
            "Escalated to Network Operations",
            now.minus(1, ChronoUnit.DAYS)
        ));

        // ── Bob Smith (05329876543) — previous SLOW_DATA from quota exhaustion ──
        repo.save(record(
            "05329876543", "Bob Smith", "SLOW_DATA",
            -68, "EXCELLENT", "4G+", 32, "NORMAL", 85.0,
            "Data quota 85% exhausted (8.5 GB of 10 GB used)",
            "Immediate — upon plan upgrade",
            now.minus(6, ChronoUnit.DAYS)
        ));

        // ── Alice Johnson (05321234567) — one past CALL_DROPS, resolved ──
        repo.save(record(
            "05321234567", "Alice Johnson", "CALL_DROPS",
            -74, "GOOD", "5G", 38, "NORMAL", null,
            "Temporary congestion on IST-EUR-4521 (resolved automatically)",
            "Resolved within 1 hour",
            now.minus(10, ChronoUnit.DAYS)
        ));
    }

    private DiagnosticRecord record(String phone, String name, String issueType,
                                    int signalDbm, String signalQuality, String tech,
                                    int stationLoad, String stationStatus, Double dataUsedPct,
                                    String rootCause, String eta, Instant createdAt) {
        DiagnosticRecord r = new DiagnosticRecord();
        r.setPhoneNumber(phone);
        r.setCustomerName(name);
        r.setIssueType(issueType);
        r.setSignalDbm(signalDbm);
        r.setSignalQuality(signalQuality);
        r.setTechnology(tech);
        r.setStationLoadPercent(stationLoad);
        r.setStationStatus(stationStatus);
        r.setDataUsedPercent(dataUsedPct);
        r.setRootCause(rootCause);
        r.setEstimatedResolution(eta);
        r.setCreatedAt(createdAt);
        return r;
    }
}
