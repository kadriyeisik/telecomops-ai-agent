package com.piagroup.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Step 1 of the diagnostic flow.
 * Classifies the customer's raw message into a structured intent type and severity,
 * and recommends which diagnostic tools to run next.
 */
@Component
public class IntentAnalysisTool implements Tool {

    @Override
    public String getName() { return "intent_analysis"; }

    @Override
    public String getDescription() {
        return "Analyzes the customer's raw message to classify the issue type, severity level, " +
               "and recommended diagnostic sequence. Always call this tool first when a customer " +
               "reports a problem.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "message", Map.of(
                    "type", "string",
                    "description", "The customer's raw complaint or question text"
                )
            ),
            "required", new String[]{"message"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String msg = String.valueOf(args.getOrDefault("message", "")).toLowerCase();

        String intentType;
        String severity;
        String diagnosticFlow;

        if (containsAny(msg, "slow", "speed", "fast", "internet", "mobile data", "4g", "5g", "lte", "loading")) {
            intentType      = "SLOW_DATA";
            severity        = "MEDIUM";
            diagnosticFlow  = "customer_verification → signal_check → base_station_load → active_package_check → suggest_resolution";
        } else if (containsAny(msg, "no signal", "no network", "no service", "offline", "dead zone", "no internet", "disconnected")) {
            intentType      = "NO_SIGNAL";
            severity        = "HIGH";
            diagnosticFlow  = "customer_verification → signal_check → base_station_load → suggest_resolution";
        } else if (containsAny(msg, "drop", "call drop", "disconnects", "cuts out", "voice", "call quality")) {
            intentType      = "CALL_DROPS";
            severity        = "HIGH";
            diagnosticFlow  = "customer_verification → signal_check → base_station_load → suggest_resolution";
        } else if (containsAny(msg, "sms", "text message", "message not", "cannot send")) {
            intentType      = "SMS_FAILURE";
            severity        = "MEDIUM";
            diagnosticFlow  = "customer_verification → signal_check → suggest_resolution";
        } else if (containsAny(msg, "bill", "charge", "invoice", "payment", "fee", "cost", "price")) {
            intentType      = "BILLING_QUERY";
            severity        = "LOW";
            diagnosticFlow  = "customer_verification → active_package_check → suggest_resolution";
        } else if (containsAny(msg, "roam", "abroad", "international", "foreign", "traveling")) {
            intentType      = "ROAMING_ISSUE";
            severity        = "MEDIUM";
            diagnosticFlow  = "customer_verification → active_package_check → suggest_resolution";
        } else if (containsAny(msg, "upgrade", "change plan", "package", "plan", "more data", "unlimited")) {
            intentType      = "PLAN_CHANGE_REQUEST";
            severity        = "LOW";
            diagnosticFlow  = "customer_verification → active_package_check → suggest_resolution";
        } else {
            intentType      = "GENERAL_INQUIRY";
            severity        = "LOW";
            diagnosticFlow  = "customer_verification → suggest_resolution";
        }

        return String.format(
            "Intent Type: %s | Severity: %s | Recommended Diagnostic Flow: %s",
            intentType, severity, diagnosticFlow
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }
}
