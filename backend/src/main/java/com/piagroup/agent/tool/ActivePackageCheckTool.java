package com.piagroup.agent.tool;

import com.piagroup.agent.data.TelecomDataStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Step 5 (all issue types) of the diagnostic flow.
 * Returns the customer's active plan details, data usage, and remaining quota.
 * Quota exhaustion is a very common hidden cause of "slow data" complaints.
 */
@Component
public class ActivePackageCheckTool implements Tool {

    private final TelecomDataStore store;

    public ActivePackageCheckTool(TelecomDataStore store) {
        this.store = store;
    }

    @Override
    public String getName() { return "active_package_check"; }

    @Override
    public String getDescription() {
        return "Retrieves the customer's active plan: plan name, data limit, used/remaining " +
               "data, available voice minutes, and billing status. Use this for any issue " +
               "that may be quota- or plan-related (slow data, billing queries, plan upgrades).";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "phone_number", Map.of(
                    "type", "string",
                    "description", "Customer's phone number"
                )
            ),
            "required", new String[]{"phone_number"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String phone = String.valueOf(args.getOrDefault("phone_number", ""));
        return store.getCustomer(phone)
            .flatMap(c -> store.getPackage(c.packageId()))
            .map(p -> {
                // Data usage line
                String dataLine;
                if (p.dataLimitGB() < 0) {
                    dataLine = String.format("Data: UNLIMITED (%.1f GB used this cycle)", p.usedDataGB());
                } else {
                    double remaining = p.dataLimitGB() - p.usedDataGB();
                    double pct = (p.usedDataGB() / p.dataLimitGB()) * 100.0;
                    String quota = pct >= 95 ? "EXHAUSTED" : pct >= 80 ? "LOW" : "OK";
                    dataLine = String.format(
                        "Data: %.1f GB used / %d GB limit (%.0f%% used — %s) | Remaining: %.2f GB",
                        p.usedDataGB(), p.dataLimitGB(), pct, quota, Math.max(remaining, 0)
                    );
                }

                // Voice minutes line
                String voiceLine = p.voiceMinutes() < 0
                    ? "Voice: UNLIMITED"
                    : String.format("Voice: %d min used / %d min limit", p.usedVoiceMinutes(), p.voiceMinutes());

                return String.format(
                    "Plan: %s | Technology: %s | Billing: %s | %s | %s",
                    p.name(), p.technology(), p.billingStatus(), dataLine, voiceLine
                );
            })
            .orElse("Package data unavailable for this customer.");
    }
}
