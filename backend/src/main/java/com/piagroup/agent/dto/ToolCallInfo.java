package com.piagroup.agent.dto;

public record ToolCallInfo(
        String toolName,
        String arguments,
        String result,
        int step
) {
}