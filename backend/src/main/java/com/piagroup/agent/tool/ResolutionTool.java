package com.piagroup.agent.tool;

import com.piagroup.agent.data.TelecomDataStore;
import com.piagroup.agent.model.DiagnosticRecord;
import com.piagroup.agent.repository.DiagnosticRecordRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Final step of the diagnostic flow.
 * Synthesises all gathered diagnostic data into a structured root-cause analysis
 * and actionable resolution plan with an estimated resolution time.
 */
@Component
public class ResolutionTool implements Tool {

    private final TelecomDataStore           store;
    private final DiagnosticRecordRepository repo;

    public ResolutionTool(TelecomDataStore store, DiagnosticRecordRepository repo) {
        this.store = store;
        this.repo  = repo;
    }

    @Override
    public String getName() { return "suggest_resolution"; }

    @Override
    public String getDescription() {
        return "Synthesises all diagnostic findings and generates a structured resolution plan " +
               "with root cause, action steps, and estimated resolution time. " +
               "Always call this as the final step after all diagnostics are complete.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "phone_number", Map.of(
                    "type", "string",
                    "description", "Customer's phone number — used to look up all diagnostic data"
                ),
                "issue_type", Map.of(
                    "type", "string",
                    "description", "Classified issue type from intent_analysis (e.g. SLOW_DATA, NO_SIGNAL)"
                )
            ),
            "required", new String[]{"phone_number", "issue_type"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String phone     = String.valueOf(args.getOrDefault("phone_number", ""));
        String issueType = String.valueOf(args.getOrDefault("issue_type", "GENERAL_INQUIRY")).toUpperCase();

        var customerOpt = store.getCustomer(phone);
        if (customerOpt.isEmpty()) {
            return "Resolution Error: Customer not found — cannot generate resolution plan.";
        }

        var customer = customerOpt.get();
        var signal   = store.getSignal(customer.region()).orElse(null);
        var station  = store.getStation(customer.region()).orElse(null);
        var pkg      = store.getPackage(customer.packageId()).orElse(null);

        // ── Account suspension check (highest priority) ────────────────────────
        if ("SUSPENDED".equals(customer.accountStatus())) {
            return buildResolution(
                "ACCOUNT_SUSPENDED",
                "Customer account is currently SUSPENDED.",
                List.of(
                    "Contact the billing department to clear any outstanding balance.",
                    "Request account reactivation via the operator's app or customer care line.",
                    "Service will be restored within 30 minutes of payment confirmation."
                ),
                "Immediate — upon payment verification"
            );
        }

        // ── Build findings ────────────────────────────────────────────────────
        List<String> rootCauses = new ArrayList<>();
        List<String> steps      = new ArrayList<>();
        String eta;

        boolean highStationLoad = station != null && station.loadPercent() >= 70;
        boolean poorSignal      = signal  != null && ("POOR".equals(signal.quality()) || "FAIR".equals(signal.quality()));
        boolean quotaExhausted  = pkg     != null && pkg.dataLimitGB() > 0
                                  && (pkg.usedDataGB() / pkg.dataLimitGB()) >= 0.95;
        boolean quotaLow        = pkg     != null && pkg.dataLimitGB() > 0
                                  && (pkg.usedDataGB() / pkg.dataLimitGB()) >= 0.80 && !quotaExhausted;

        switch (issueType) {
            case "SLOW_DATA" -> {
                if (quotaExhausted) {
                    rootCauses.add("Data quota nearly exhausted (" +
                        String.format("%.1f", pkg.usedDataGB()) + " GB of " + pkg.dataLimitGB() + " GB used)");
                    steps.add("Upgrade to a higher-tier plan (5G Pro Unlimited recommended) for immediate speed restoration.");
                    steps.add("Purchase a top-up data add-on as a short-term fix.");
                    eta = "Immediate — upon plan upgrade";
                } else if (highStationLoad) {
                    rootCauses.add("Base station " + (station != null ? station.stationId() : "N/A") +
                        " is under high load (" + (station != null ? station.loadPercent() : "?") + "%)");
                    steps.add("Connect to a Wi-Fi network if available as an immediate workaround.");
                    steps.add("Network operations team has been alerted; load-balancing to adjacent cells is in progress.");
                    steps.add("Try again in 2–4 hours; congestion typically eases during off-peak hours.");
                    eta = "2–4 hours (congestion-dependent)";
                } else if (poorSignal) {
                    rootCauses.add("Weak signal in region (" +
                        (signal != null ? signal.signalDbm() : "?") + " dBm, quality: " +
                        (signal != null ? signal.quality() : "?") + ")");
                    steps.add("Move to an open area or higher floor to improve reception.");
                    steps.add("Restart your device to force a fresh network attachment.");
                    steps.add("If issue persists indoors, consider a Wi-Fi calling / femtocell solution.");
                    eta = "Immediate — upon environment change";
                } else {
                    steps.add("Toggle airplane mode off and on to force network re-registration.");
                    steps.add("Restart the device.");
                    steps.add("Verify APN settings are correctly configured for this operator.");
                    eta = "5–15 minutes";
                }
            }
            case "NO_SIGNAL" -> {
                if (highStationLoad) {
                    rootCauses.add("Severe congestion on base station " +
                        (station != null ? station.stationId() : "N/A"));
                    steps.add("Emergency alert sent to network operations center.");
                    steps.add("Try connecting via Wi-Fi Calling as a temporary workaround.");
                    eta = "1–3 hours (NOC investigation in progress)";
                } else if (poorSignal) {
                    rootCauses.add("Very weak signal coverage (" +
                        (signal != null ? signal.coveragePercent() : "?") + "% coverage in region)");
                    steps.add("Move to a location with better line-of-sight to the nearest tower.");
                    steps.add("Check for local outages at the operator's status page.");
                    eta = "Immediate — upon location change";
                } else {
                    steps.add("Toggle airplane mode off and on.");
                    steps.add("Remove and reinsert the SIM card.");
                    steps.add("Check for a pending OS update that might affect radio drivers.");
                    eta = "5–10 minutes";
                }
            }
            case "CALL_DROPS" -> {
                if (highStationLoad) rootCauses.add("Base station congestion causing VoLTE packet loss");
                if (poorSignal)      rootCauses.add("Marginal signal quality leading to handover failures");
                steps.add("Enable Wi-Fi Calling in device settings for improved voice quality indoors.");
                steps.add("Ensure VoLTE is enabled in network settings for HD voice.");
                if (highStationLoad) steps.add("Load on the serving cell is being investigated; expected improvement within 2 hours.");
                eta = highStationLoad ? "2 hours" : "Immediate";
            }
            case "BILLING_QUERY" -> {
                steps.add("Your current plan is: " + (pkg != null ? pkg.name() : "N/A"));
                if (pkg != null && pkg.dataLimitGB() > 0) {
                    steps.add(String.format("Data usage: %.1f GB of %d GB used this billing cycle.",
                        pkg.usedDataGB(), pkg.dataLimitGB()));
                }
                steps.add("For detailed invoice history, log in to the operator's self-service portal.");
                steps.add("For disputed charges, raise a billing dispute via the app within 60 days.");
                eta = "N/A — informational";
            }
            default -> {
                steps.add("A support ticket has been created and assigned to the relevant team.");
                steps.add("You will receive an update via SMS within 24 hours.");
                eta = "Within 24 hours";
            }
        }

        if (rootCauses.isEmpty()) rootCauses.add("No single dominant cause identified; standard recovery steps recommended.");
        String rootCauseStr = String.join("; ", rootCauses);

        // ── Persist a diagnostic snapshot for future case_history_check calls ──
        saveRecord(phone, issueType, customer, signal, station, pkg, rootCauseStr, eta);

        return buildResolution(issueType, rootCauseStr, steps, eta);
    }

    private void saveRecord(String phone,
                            String issueType,
                            TelecomDataStore.Customer customer,
                            TelecomDataStore.SignalStatus signal,
                            TelecomDataStore.BaseStation station,
                            TelecomDataStore.Package pkg,
                            String rootCause,
                            String eta) {
        DiagnosticRecord r = new DiagnosticRecord();
        r.setPhoneNumber(customer.phoneNumber());
        r.setCustomerName(customer.name());
        r.setIssueType(issueType);
        if (signal != null) {
            r.setSignalDbm(signal.signalDbm());
            r.setSignalQuality(signal.quality());
            r.setTechnology(signal.technology());
        }
        if (station != null) {
            r.setStationLoadPercent(station.loadPercent());
            r.setStationStatus(station.status());
        }
        if (pkg != null && pkg.dataLimitGB() > 0) {
            r.setDataUsedPercent(pkg.usedDataGB() / pkg.dataLimitGB() * 100.0);
        }
        r.setRootCause(rootCause);
        r.setEstimatedResolution(eta);
        repo.save(r);
    }

    private String buildResolution(String type, String rootCause, List<String> steps, String eta) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RESOLUTION REPORT ===\n");
        sb.append("Issue Type  : ").append(type).append("\n");
        sb.append("Root Cause  : ").append(rootCause).append("\n");
        sb.append("Action Steps:\n");
        for (int i = 0; i < steps.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }
        sb.append("Est. Resolution: ").append(eta);
        return sb.toString();
    }
}
