package com.piagroup.agent.controller;

import com.piagroup.agent.dto.ChatRequest;
import com.piagroup.agent.dto.ChatResponse;
import com.piagroup.agent.model.Conversation;
import com.piagroup.agent.repository.ConversationRepository;
import com.piagroup.agent.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentService agentService;
    private final ConversationRepository conversationRepository;

    public ChatController(AgentService agentService, ConversationRepository conversationRepository) {
        this.agentService = agentService;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        try {
            ChatResponse response = agentService.handleUserMessage(request.conversationId(), request.message());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // e.g. the API key is not set
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations")
    public List<Conversation> listConversations() {
        return conversationRepository.findAll();
    }
}
