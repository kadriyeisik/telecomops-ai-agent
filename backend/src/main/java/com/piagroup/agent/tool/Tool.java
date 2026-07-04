package com.piagroup.agent.tool;

import java.util.Map;

/**
 * Every tool the agent can call implements this interface.
 * Matches the OpenAI function-calling format:
 *  - name: the function name
 *  - description: explains to the LLM what it's for
 *  - parametersSchema: JSON Schema (the format OpenAI expects)
 *  - execute: the actual execution logic
 */
public interface Tool {

    String getName();

    String getDescription();

    /** JSON Schema for function calling (as a Map, auto-converted to JSON) */
    Map<String, Object> getParametersSchema();

    /** The tool's actual execution logic. args -> the parameters sent by the LLM */
    String execute(Map<String, Object> args);
}
