package com.piagroup.agent.service;

import com.piagroup.agent.tool.Tool;

import java.util.List;
import java.util.Map;

/**
 * Contract for LLM providers (OpenAI, Ollama, …).
 * Implementations are selected at runtime via @ConditionalOnProperty on llm.provider.
 */
public interface LlmClient {

    /**
     * Sends a chat-completion request to the underlying LLM provider.
     *
     * @param messages OpenAI-formatted message list (role / content / tool_calls / tool_call_id …)
     * @param tools    all tools currently available to the agent
     * @return the assistant "message" object (role, content, tool_calls) as a Map
     */
    Map<String, Object> chat(List<Map<String, Object>> messages, List<Tool> tools);
}
