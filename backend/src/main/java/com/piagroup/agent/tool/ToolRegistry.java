package com.piagroup.agent.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically collects every Tool implementation registered in the Spring context.
 * To add a new tool, just implement the Tool interface and annotate it with
 * @Component; ToolRegistry picks it up automatically.
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> toolsByName = new HashMap<>();

    public ToolRegistry(List<Tool> tools) {
        for (Tool tool : tools) {
            toolsByName.put(tool.getName(), tool);
        }
    }

    public List<Tool> getAllTools() {
        return List.copyOf(toolsByName.values());
    }

    public Tool get(String name) {
        return toolsByName.get(name);
    }
}
