package com.piagroup.agent.data;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory mock data store simulating a telecom operator's back-end systems
 * (BSS/OSS): customer profiles, active packages, network signal readings, and
 * base station load metrics. In a production environment these would be calls to
 * real CRM, billing, and network-management APIs.
 */
@Component
public class TelecomDataStore {

    // ── Domain records ────────────────────────────────────────────────────────

    public record Customer(String phoneNumber, String name, String accountStatus,
                           String packageId, String region, String customerId) {}

    public record Package(String id, String name, int dataLimitGB,
                          double usedDataGB, String technology,
                          int voiceMinutes, int usedVoiceMinutes, String billingStatus) {}

    public record SignalStatus(String region, int signalDbm, String quality,
                               String technology, double coveragePercent) {}

    public record BaseStation(String stationId, String region,
                              int loadPercent, String status, int connectedDevices) {}

    // ── Static mock data ──────────────────────────────────────────────────────

    private static final Map<String, Customer> CUSTOMERS = new LinkedHashMap<>();
    private static final Map<String, Package>  PACKAGES  = new LinkedHashMap<>();
    private static final Map<String, SignalStatus> SIGNALS  = new LinkedHashMap<>();
    private static final Map<String, BaseStation>  STATIONS = new LinkedHashMap<>();

    static {
        // Customers  (phone → Customer)
        CUSTOMERS.put("05321234567", new Customer("05321234567", "Alice Johnson",  "ACTIVE",    "PKG_5G_PRO",      "ISTANBUL_EUROPEAN", "C-1001"));
        CUSTOMERS.put("05329876543", new Customer("05329876543", "Bob Smith",       "ACTIVE",    "PKG_4G_BASIC",    "ANKARA_CENTRAL",    "C-1002"));
        CUSTOMERS.put("05321111111", new Customer("05321111111", "Carol Davis",     "SUSPENDED", "PKG_4G_STANDARD", "IZMIR_COASTAL",     "C-1003"));
        CUSTOMERS.put("05325551234", new Customer("05325551234", "David Wilson",    "ACTIVE",    "PKG_5G_PRO",      "ISTANBUL_ASIAN",    "C-1004"));
        CUSTOMERS.put("05330001122", new Customer("05330001122", "Emma Brown",      "ACTIVE",    "PKG_4G_STANDARD", "BURSA_CENTRAL",     "C-1005"));

        // Packages  (id → Package)
        // dataLimitGB = -1 means unlimited
        PACKAGES.put("PKG_5G_PRO",      new Package("PKG_5G_PRO",      "5G Pro Unlimited",   -1, 45.2, "5G",  -1,   0,    "PAID"));
        PACKAGES.put("PKG_4G_BASIC",    new Package("PKG_4G_BASIC",    "4G Basic 10 GB",     10,  9.8, "4G",  500,  312,  "PAID"));
        PACKAGES.put("PKG_4G_STANDARD", new Package("PKG_4G_STANDARD", "4G Standard 30 GB",  30, 12.3, "4G+", 1000, 204,  "PAID"));

        // Signal readings  (region → SignalStatus)
        SIGNALS.put("ISTANBUL_EUROPEAN", new SignalStatus("ISTANBUL_EUROPEAN", -72, "GOOD",      "5G",  98.5));
        SIGNALS.put("ISTANBUL_ASIAN",    new SignalStatus("ISTANBUL_ASIAN",    -88, "FAIR",      "5G",  94.2));
        SIGNALS.put("ANKARA_CENTRAL",    new SignalStatus("ANKARA_CENTRAL",    -68, "EXCELLENT", "4G+", 99.1));
        SIGNALS.put("IZMIR_COASTAL",     new SignalStatus("IZMIR_COASTAL",     -97, "POOR",      "4G",  81.0));
        SIGNALS.put("BURSA_CENTRAL",     new SignalStatus("BURSA_CENTRAL",     -78, "GOOD",      "4G+", 96.3));

        // Base stations  (region → BaseStation)
        STATIONS.put("ISTANBUL_EUROPEAN", new BaseStation("IST-EUR-4521", "ISTANBUL_EUROPEAN", 41, "NORMAL",    1240));
        STATIONS.put("ISTANBUL_ASIAN",    new BaseStation("IST-ASI-3318", "ISTANBUL_ASIAN",    89, "HIGH_LOAD", 3180));
        STATIONS.put("ANKARA_CENTRAL",    new BaseStation("ANK-CEN-2201", "ANKARA_CENTRAL",    33, "NORMAL",    890));
        STATIONS.put("IZMIR_COASTAL",     new BaseStation("IZM-CST-1104", "IZMIR_COASTAL",     76, "HIGH_LOAD", 2410));
        STATIONS.put("BURSA_CENTRAL",     new BaseStation("BRS-CEN-0981", "BURSA_CENTRAL",     55, "MODERATE",  1650));
    }

    // ── Lookup helpers ────────────────────────────────────────────────────────

    /** Normalises various phone formats (e.g. +905321234567, 05321234567, 5321234567) */
    public Optional<Customer> getCustomer(String rawPhone) {
        String n = rawPhone.replaceAll("[^0-9]", "");
        if (n.startsWith("90") && n.length() == 12) n = "0" + n.substring(2);
        if (!n.startsWith("0") && n.length() == 10)  n = "0" + n;
        return Optional.ofNullable(CUSTOMERS.get(n));
    }

    public Optional<Package>     getPackage(String packageId) { return Optional.ofNullable(PACKAGES.get(packageId)); }
    public Optional<SignalStatus> getSignal(String region)    { return Optional.ofNullable(SIGNALS.get(region)); }
    public Optional<BaseStation>  getStation(String region)   { return Optional.ofNullable(STATIONS.get(region)); }
}
