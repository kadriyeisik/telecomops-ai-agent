package com.piagroup.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        Long conversationId,   // null starts a new conversation
        @NotBlank String message
) {}
