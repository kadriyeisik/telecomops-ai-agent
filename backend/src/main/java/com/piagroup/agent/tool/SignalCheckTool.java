package com.piagroup.agent.tool;

import com.piagroup.agent.data.TelecomDataStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Step 3 (connectivity issues) of the diagnostic flow.
 * Returns signal strength, quality, and network technology for the
 * customer's registered region.
 */
@Component
public class SignalCheckTool implements Tool {

    private final TelecomDataStore store;

    public SignalCheckTool(TelecomDataStore store) {
        this.store = store;
    }

    @Override
    public String getName() { return "signal_check"; }

    @Override
    public String getDescription() {
        return "Checks the current signal quality and network technology (5G/4G+/4G) for " +
               "the customer's registered region. Use this for SLOW_DATA, NO_SIGNAL, " +
               "or CALL_DROPS issues.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "phone_number", Map.of(
                    "type", "string",
                    "description", "Customer's phone number to determine their region"
                )
            ),
            "required", new String[]{"phone_number"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String phone = String.valueOf(args.getOrDefault("phone_number", ""));
        return store.getCustomer(phone)
            .flatMap(c -> store.getSignal(c.region()))
            .map(s -> {
                String assessment = switch (s.quality()) {
                    case "EXCELLENT" -> "Optimal — no signal-related issues expected.";
                    case "GOOD"      -> "Good — minor degradation possible under heavy load.";
                    case "FAIR"      -> "Fair — speed reduction likely, especially indoors.";
                    case "POOR"      -> "Poor — significant impact on data speed and call quality.";
                    default          -> "Unknown quality level.";
                };
                return String.format(
                    "Region: %s | Signal Strength: %d dBm | Quality: %s | " +
                    "Technology: %s | Coverage: %.1f%% | Assessment: %s",
                    s.region(), s.signalDbm(), s.quality(),
                    s.technology(), s.coveragePercent(), assessment
                );
            })
            .orElse("Signal data unavailable for this customer's region.");
    }
}
