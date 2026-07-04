package com.piagroup.agent.tool;

import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

// This tool has been superseded by the Telecom AI Operations Agent tools.
// @Component intentionally removed — class retained for reference only.
public class DateTimeTool implements Tool {

    @Override
    public String getName() {
        return "get_current_datetime";
    }

    @Override
    public String getDescription() {
        return "Returns the current date and time. Use this tool when the user asks 'what day is it', 'what time is it', or similar questions.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "timezone", Map.of(
                                "type", "string",
                                "description", "IANA timezone, e.g. 'Europe/Istanbul'. Defaults to Europe/Istanbul if not specified."
                        )
                ),
                "required", new String[]{}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String tz = args.get("timezone") != null ? String.valueOf(args.get("timezone")) : "Europe/Istanbul";
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(tz));
            return now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss (EEEE)"));
        } catch (Exception e) {
            return "Error: invalid timezone -> " + tz;
        }
    }
}
