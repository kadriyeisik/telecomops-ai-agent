package com.piagroup.agent.tool;

import com.piagroup.agent.data.TelecomDataStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Step 2 of the diagnostic flow.
 * Verifies customer identity and returns account status, region, and plan.
 * The agent must ask for the phone number if the customer hasn't provided it.
 */
@Component
public class CustomerVerificationTool implements Tool {

    private final TelecomDataStore store;

    public CustomerVerificationTool(TelecomDataStore store) {
        this.store = store;
    }

    @Override
    public String getName() { return "customer_verification"; }

    @Override
    public String getDescription() {
        return "Looks up and verifies a customer account by their phone number. " +
               "Returns the customer's name, account status (ACTIVE/SUSPENDED), region, " +
               "active plan ID, and customer ID. Ask the customer for their phone number " +
               "if it has not already been provided.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "phone_number", Map.of(
                    "type", "string",
                    "description", "Customer's mobile phone number (e.g. 05321234567 or +905321234567)"
                )
            ),
            "required", new String[]{"phone_number"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String phone = String.valueOf(args.getOrDefault("phone_number", ""));
        return store.getCustomer(phone)
            .map(c -> String.format(
                "Customer ID: %s | Name: %s | Phone: %s | Account Status: %s | " +
                "Active Plan: %s | Region: %s",
                c.customerId(), c.name(), c.phoneNumber(),
                c.accountStatus(), c.packageId(), c.region()
            ))
            .orElse("Account not found for phone number: " + phone +
                    ". Please verify the number and try again.");
    }
}
