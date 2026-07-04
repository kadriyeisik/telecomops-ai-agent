package com.piagroup.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piagroup.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sends chat completion requests (with function/tool calling) to the OpenAI API.
 * Active when llm.provider=openai (this is also the default, see matchIfMissing below).
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiService implements LlmClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public OpenAiService() {
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    /**
     * Performs a single "chat completion" call to OpenAI.
     * @param messages OpenAI-formatted message list (role/content/tool_calls/tool_call_id...)
     * @param tools    every tool currently available to the agent (converted into function definitions)
     * @return the returned "message" object (role, content, tool_calls) as a Map
     */
    @Override
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Tool> tools) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not set. Export it as an environment variable: export OPENAI_API_KEY=sk-...");
        }

        List<Map<String, Object>> toolDefs = new ArrayList<>();
        for (Tool tool : tools) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());
            function.put("parameters", tool.getParametersSchema());

            Map<String, Object> toolDef = new LinkedHashMap<>();
            toolDef.put("type", "function");
            toolDef.put("function", function);
            toolDefs.add(toolDef);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", toolDefs);
        body.put("tool_choice", "auto");
        body.put("temperature", 0.4);

        String responseJson;
        try {
            responseJson = webClient.post()
                    .uri(Objects.requireNonNull(apiUrl, "openai.api.url must be configured"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }

        try {
            JsonNode root = mapper.readTree(responseJson);
            if (root.has("error")) {
                throw new RuntimeException("OpenAI returned an error: " + root.get("error").toString());
            }
            JsonNode messageNode = root.get("choices").get(0).get("message");
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = mapper.convertValue(messageNode, Map.class);
            return messageMap;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Could not parse OpenAI response: " + e.getMessage(), e);
        }
    }
}
