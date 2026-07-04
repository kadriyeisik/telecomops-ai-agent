package com.piagroup.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Uses the free Open-Meteo API (no API key required).
 * 1) Converts the city name into coordinates (geocoding)
 * 2) Fetches the current weather for those coordinates
 */
// This tool has been superseded by the Telecom AI Operations Agent tools.
// @Component intentionally removed — class retained for reference only.
public class WeatherTool implements Tool {

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "Returns the current weather (temperature, wind) for a given city.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of(
                                "type", "string",
                                "description", "Name of the city to get weather for, e.g. 'Istanbul'"
                        )
                ),
                "required", new String[]{"city"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String city = String.valueOf(args.get("city"));
        try {
            String geoJson = webClient.get()
                    .uri("https://geocoding-api.open-meteo.com/v1/search?name=" + city + "&count=1&language=en")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode geoRoot = mapper.readTree(geoJson);
            JsonNode results = geoRoot.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return "Error: location not found for '" + city + "'.";
            }
            JsonNode first = results.get(0);
            double lat = first.get("latitude").asDouble();
            double lon = first.get("longitude").asDouble();
            String resolvedName = first.get("name").asText();

            String weatherJson = webClient.get()
                    .uri("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                            + "&current=temperature_2m,wind_speed_10m,relative_humidity_2m")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode weatherRoot = mapper.readTree(weatherJson);
            JsonNode current = weatherRoot.get("current");
            double temp = current.get("temperature_2m").asDouble();
            double wind = current.get("wind_speed_10m").asDouble();
            double humidity = current.get("relative_humidity_2m").asDouble();

            return String.format("%s: %.1f°C, wind %.1f km/h, humidity %.0f%%",
                    resolvedName, temp, wind, humidity);
        } catch (Exception e) {
            return "Error: could not fetch weather -> " + e.getMessage();
        }
    }
}
