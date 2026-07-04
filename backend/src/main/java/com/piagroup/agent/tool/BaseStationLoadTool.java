package com.piagroup.agent.tool;

import com.piagroup.agent.data.TelecomDataStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Step 4 (connectivity issues) of the diagnostic flow.
 * Returns the load percentage, operational status, and connected device count
 * of the base station serving the customer's region.
 */
@Component
public class BaseStationLoadTool implements Tool {

    private final TelecomDataStore store;

    public BaseStationLoadTool(TelecomDataStore store) {
        this.store = store;
    }

    @Override
    public String getName() { return "base_station_load"; }

    @Override
    public String getDescription() {
        return "Checks the load and operational status of the base station (cell tower) " +
               "serving the customer's region. High load (>80%) indicates network congestion " +
               "that can cause slow speeds or dropped connections.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "phone_number", Map.of(
                    "type", "string",
                    "description", "Customer's phone number to determine the serving base station"
                )
            ),
            "required", new String[]{"phone_number"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String phone = String.valueOf(args.getOrDefault("phone_number", ""));
        return store.getCustomer(phone)
            .flatMap(c -> store.getStation(c.region()))
            .map(bs -> {
                String analysis;
                if (bs.loadPercent() >= 85) {
                    analysis = "CRITICAL — severe congestion; network ops team notified automatically.";
                } else if (bs.loadPercent() >= 70) {
                    analysis = "HIGH — significant congestion causing speed degradation.";
                } else if (bs.loadPercent() >= 50) {
                    analysis = "MODERATE — some congestion; speeds may be slightly reduced.";
                } else {
                    analysis = "NORMAL — no congestion detected.";
                }
                return String.format(
                    "Station ID: %s | Region: %s | Load: %d%% | Status: %s | " +
                    "Connected Devices: %,d | Analysis: %s",
                    bs.stationId(), bs.region(), bs.loadPercent(),
                    bs.status(), bs.connectedDevices(), analysis
                );
            })
            .orElse("Base station data unavailable for this customer's region.");
    }
}
