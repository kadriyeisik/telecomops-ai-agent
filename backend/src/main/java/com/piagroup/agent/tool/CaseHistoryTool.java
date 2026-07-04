package com.piagroup.agent.tool;

import com.piagroup.agent.data.TelecomDataStore;
import com.piagroup.agent.model.DiagnosticRecord;
import com.piagroup.agent.repository.DiagnosticRecordRepository;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Retrieves the customer's diagnostic history from the database, compares it
 * with the current live network readings, and surfaces trends such as:
 *  - degrading signal quality over time
 *  - rising base-station load across visits
 *  - recurring issue types that warrant escalation
 *
 * Call this AFTER signal_check / base_station_load / active_package_check
 * and BEFORE suggest_resolution so the LLM has all context when formulating
 * the final recommendation.
 */
@Component
public class CaseHistoryTool implements Tool {

    private static final int MAX_HISTORY = 5;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Europe/Istanbul"));

    private final DiagnosticRecordRepository repo;
    private final TelecomDataStore           store;

    public CaseHistoryTool(DiagnosticRecordRepository repo, TelecomDataStore store) {
        this.repo  = repo;
        this.store = store;
    }

    @Override public String getName() { return "case_history_check"; }

    @Override
    public String getDescription() {
        return "Retrieves the customer's previous diagnostic cases from the database and compares " +
               "them with current live network readings to identify trends (degrading signal, " +
               "rising station load, recurring issues). Call this after all live diagnostics " +
               "and before suggest_resolution.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "phone_number", Map.of(
                    "type", "string",
                    "description", "Customer's phone number"
                ),
                "issue_type", Map.of(
                    "type", "string",
                    "description", "Current issue type from intent_analysis (optional, used for recurring-issue detection)"
                )
            ),
            "required", new String[]{"phone_number"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String phone     = String.valueOf(args.getOrDefault("phone_number", ""));
        String issueType = String.valueOf(args.getOrDefault("issue_type", "")).toUpperCase();

        var customerOpt = store.getCustomer(phone);
        if (customerOpt.isEmpty()) return "Case history unavailable — customer not found.";
        var customer = customerOpt.get();

        List<DiagnosticRecord> records =
                repo.findByPhoneNumberOrderByCreatedAtDesc(customer.phoneNumber());

        if (records.isEmpty()) {
            return "No previous cases found for " + customer.name() +
                   " (" + customer.phoneNumber() + "). This is the first recorded diagnostic.";
        }

        // Limit to MAX_HISTORY
        List<DiagnosticRecord> recent = records.subList(0, Math.min(records.size(), MAX_HISTORY));

        StringBuilder sb = new StringBuilder();
        sb.append("=== CASE HISTORY: ").append(customer.name())
          .append(" (").append(customer.phoneNumber()).append(") ===\n");
        sb.append("Total previous cases: ").append(records.size()).append("\n\n");

        // ── List recent cases ──────────────────────────────────────────────
        sb.append("RECENT CASES (newest first):\n");
        for (int i = 0; i < recent.size(); i++) {
            DiagnosticRecord r = recent.get(i);
            sb.append("  Case ").append(i + 1)
              .append(" (").append(FMT.format(r.getCreatedAt())).append("): ")
              .append(r.getIssueType() != null ? r.getIssueType() : "UNKNOWN");

            if (r.getSignalDbm() != null)
                sb.append(" | Signal: ").append(r.getSignalDbm()).append(" dBm (").append(r.getSignalQuality()).append(")");
            if (r.getStationLoadPercent() != null)
                sb.append(" | Station: ").append(r.getStationLoadPercent()).append("% (").append(r.getStationStatus()).append(")");
            if (r.getDataUsedPercent() != null)
                sb.append(String.format(" | Data: %.0f%%", r.getDataUsedPercent()));
            if (r.getRootCause() != null)
                sb.append("\n    Root cause: ").append(r.getRootCause());

            sb.append("\n");
        }

        // ── Trend analysis ─────────────────────────────────────────────────
        sb.append("\nTREND ANALYSIS:\n");
        List<String> trends = new ArrayList<>();

        var currentSignal  = store.getSignal(customer.region()).orElse(null);
        var currentStation = store.getStation(customer.region()).orElse(null);

        DiagnosticRecord latest = recent.get(0);

        // Signal trend: latest record vs. current live reading
        if (latest.getSignalDbm() != null && currentSignal != null) {
            int prev = latest.getSignalDbm();
            int curr = currentSignal.signalDbm();
            int delta = curr - prev;
            if (delta < -5)
                trends.add("📉 Signal degrading: " + prev + " → " + curr + " dBm (Δ" + delta + ") — coverage is getting worse");
            else if (delta > 5)
                trends.add("📈 Signal recovering: " + prev + " → " + curr + " dBm (Δ+" + delta + ")");
            else
                trends.add("↔ Signal stable at ~" + curr + " dBm since last case");
        }

        // Station load trend: latest record vs. current live reading
        if (latest.getStationLoadPercent() != null && currentStation != null) {
            int prev = latest.getStationLoadPercent();
            int curr = currentStation.loadPercent();
            int delta = curr - prev;
            if (delta > 8)
                trends.add("📈 Station load increasing: " + prev + "% → " + curr + "% — congestion is worsening");
            else if (delta < -8)
                trends.add("📉 Station load decreasing: " + prev + "% → " + curr + "% — congestion easing");
            else
                trends.add("↔ Station load stable at ~" + curr + "%");
        }

        // Across-records signal degradation (first vs. most-recent record)
        if (recent.size() >= 3) {
            DiagnosticRecord oldest = recent.get(recent.size() - 1);
            if (oldest.getSignalDbm() != null && latest.getSignalDbm() != null) {
                int totalDelta = latest.getSignalDbm() - oldest.getSignalDbm();
                if (totalDelta < -8)
                    trends.add("⚠️ Long-term signal decline: " + oldest.getSignalDbm() + " → " + latest.getSignalDbm() +
                               " dBm over " + recent.size() + " cases — persistent coverage degradation");
            }
        }

        // Recurring issue detection
        long sameIssueCount = recent.stream()
                .filter(r -> issueType.equals(r.getIssueType()))
                .count();
        if (!issueType.isBlank() && sameIssueCount >= 2) {
            trends.add("🔁 Recurring " + issueType + ": " + sameIssueCount + " of the last " +
                       recent.size() + " case(s) — standard self-service steps have not resolved the issue");
            trends.add("🚨 RECOMMENDATION: Escalate to Network Operations Team for root-cause investigation");
        }

        if (trends.isEmpty()) {
            sb.append("  No significant trends detected.\n");
        } else {
            for (String t : trends) sb.append("  ").append(t).append("\n");
        }

        return sb.toString().trim();
    }
}
