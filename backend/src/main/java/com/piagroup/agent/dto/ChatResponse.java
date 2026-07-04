package com.piagroup.agent.dto;

import java.util.List;

public record ChatResponse(
        Long conversationId,
        String reply,
        List<ToolCallInfo> trace
) {}
