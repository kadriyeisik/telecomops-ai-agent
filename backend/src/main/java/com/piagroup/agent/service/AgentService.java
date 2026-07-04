package com.piagroup.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piagroup.agent.dto.ChatResponse;
import com.piagroup.agent.dto.ToolCallInfo;
import com.piagroup.agent.model.Conversation;
import com.piagroup.agent.model.Message;
import com.piagroup.agent.repository.ConversationRepository;
import com.piagroup.agent.repository.MessageRepository;
import com.piagroup.agent.tool.Tool;
import com.piagroup.agent.tool.ToolRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the heart of the project: instead of a fixed flow the user draws
 * beforehand (like n8n), it runs a loop (the "agent loop") where the LLM itself
 * decides "which tool to call, and when."
 *
 * Flow:
 *   1) User message + history -> sent to the LLM
 *   2) If the LLM says "I want to call a tool" -> the tool runs, the result goes back to the LLM
 *   3) Once the LLM no longer needs a tool, it produces the final answer
 *   4) MAX_ITERATIONS caps the loop to prevent an infinite cycle
 */
@Service
public class AgentService {

    private static final int MAX_ITERATIONS = 5;

    private static final String SYSTEM_PROMPT = """
            You are TelecomOps AI — an advanced AI operations assistant for a telecommunications company.
            Your role is to diagnose and resolve customer telecom issues through a systematic, professional diagnostic workflow.

            MANDATORY WORKFLOW — follow this exact order for every customer issue:
            1. INTENT ANALYSIS      : Call intent_analysis with the customer's raw message to classify the issue.
            2. CUSTOMER VERIFICATION: Call customer_verification with their phone number.
                                      If the phone number has not been provided, politely ask for it first.
            3. DIAGNOSTICS          : Run tools based on the classified intent:
                                      - SLOW_DATA / NO_SIGNAL / CALL_DROPS: signal_check → base_station_load → active_package_check
                                      - BILLING_QUERY / PLAN_CHANGE_REQUEST: active_package_check
                                      - ROAMING_ISSUE: active_package_check
            4. CASE HISTORY         : Always call case_history_check after diagnostics to retrieve previous cases
                                      and identify trends. Pass the phone_number and current issue_type.
            5. RESOLUTION           : Call suggest_resolution last with the issue_type and phone_number.

            RESPONSE FORMAT — after all tool calls complete, present a clean structured summary:
            **Issue Identified** — [intent type + severity]
            **Customer** — [name, account status]
            **Diagnostic Findings** — one bullet per tool result (signal, station load, package status)
            **Case History & Trends** — key trend findings from case_history_check (escalation if recurring)
            **Root Cause** — the primary cause identified
            **Resolution Steps** — numbered action items from the resolution report
            **Estimated Resolution** — time frame

            Be professional, empathetic, and precise. Always cite the metrics returned by the tools.
            If case_history_check flags a recurring issue, emphasise the escalation recommendation prominently.
            """;



    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public AgentService(LlmClient llmClient,
                         ToolRegistry toolRegistry,
                         ConversationRepository conversationRepository,
                         MessageRepository messageRepository) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public ChatResponse handleUserMessage(Long conversationId, String userMessage) {
        Conversation conversation = loadOrCreateConversation(conversationId, userMessage);

        // Persist the user message
        saveMessage(conversation, Message.Role.USER, userMessage, null);

        // Build the message history in the LLM provider's expected format
        List<Map<String, Object>> apiMessages = buildApiMessageHistory(conversation);

        List<Tool> tools = toolRegistry.getAllTools();
        List<ToolCallInfo> trace = new ArrayList<>();

        String finalReply = null;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            Map<String, Object> assistantMessage = llmClient.chat(apiMessages, tools);
            apiMessages.add(assistantMessage);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) assistantMessage.get("tool_calls");

            if (toolCalls == null || toolCalls.isEmpty()) {
                // The LLM gave its final answer, the loop ends
                finalReply = String.valueOf(assistantMessage.getOrDefault("content", ""));
                break;
            }

            // The LLM wants to call one or more tools
            for (Map<String, Object> toolCall : toolCalls) {
                String toolCallId = String.valueOf(toolCall.get("id"));
                @SuppressWarnings("unchecked")
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String toolName = String.valueOf(function.get("name"));
                String argsJson = String.valueOf(function.get("arguments"));

                Map<String, Object> args;
                try {
                    args = mapper.readValue(argsJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    args = Map.of();
                }

                Tool tool = toolRegistry.get(toolName);
                String result = (tool != null)
                        ? safeExecute(tool, args)
                        : "Error: unknown tool -> " + toolName;

                trace.add(new ToolCallInfo(toolName, argsJson, result, trace.size() + 1));
                saveMessage(conversation, Message.Role.TOOL, result, toolName);

                Map<String, Object> toolResultMessage = new LinkedHashMap<>();
                toolResultMessage.put("role", "tool");
                toolResultMessage.put("tool_call_id", toolCallId);
                toolResultMessage.put("content", result);
                apiMessages.add(toolResultMessage);
            }
            // the loop continues: the LLM sees the tool results and either calls another tool or gives the final answer
        }

        if (finalReply == null) {
            finalReply = "Sorry, I could not complete your request within the step limit. Please simplify your question.";
        }

        saveMessage(conversation, Message.Role.ASSISTANT, finalReply, null);

        return new ChatResponse(conversation.getId(), finalReply, trace);
    }

    private String safeExecute(Tool tool, Map<String, Object> args) {
        try {
            return tool.execute(args);
        } catch (Exception e) {
            return "Error: " + tool.getName() + " failed -> " + e.getMessage();
        }
    }

    private Conversation loadOrCreateConversation(Long conversationId, String firstMessage) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                    .orElseGet(() -> createConversation(firstMessage));
        }
        return createConversation(firstMessage);
    }

    private Conversation createConversation(String firstMessage) {
        Conversation c = new Conversation();
        String title = firstMessage.length() > 40 ? firstMessage.substring(0, 40) + "..." : firstMessage;
        c.setTitle(title);
        return conversationRepository.save(c);
    }

    private void saveMessage(Conversation conversation, Message.Role role, String content, String toolName) {
        Message m = new Message();
        m.setConversation(conversation);
        m.setRole(role);
        m.setContent(content);
        m.setToolName(toolName);
        messageRepository.save(m);
    }

    /** Converts the DB message history into the role/content format the LLM provider expects. */
    private List<Map<String, Object>> buildApiMessageHistory(Conversation conversation) {
        List<Map<String, Object>> result = new ArrayList<>();

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);
        result.add(system);

        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        for (Message m : history) {
            // Note: we skip historical TOOL-role messages for simplicity;
            // each request manages its own tool-call loop separately inside the apiMessages list.
            if (m.getRole() == Message.Role.TOOL) continue;

            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", m.getRole() == Message.Role.USER ? "user" : "assistant");
            msg.put("content", m.getContent());
            result.add(msg);
        }
        return result;
    }
}
