package com.piagroup.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple tool that lets the agent save important information to "memory"
 * during the conversation and list it later.
 * (For learning purposes: kept in memory only, not persistent.)
 */
// This tool has been superseded by the Telecom AI Operations Agent tools.
// @Component intentionally removed — class retained for reference only.
public class NoteTool implements Tool {

    private final List<String> notes = new CopyOnWriteArrayList<>();

    @Override
    public String getName() {
        return "manage_notes";
    }

    @Override
    public String getDescription() {
        return "Saves information the user wants to remember (action=save) " +
                "or lists all saved notes (action=list).";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of(
                                "type", "string",
                                "enum", new String[]{"save", "list"},
                                "description", "'save': saves a new note, 'list': lists all saved notes"
                        ),
                        "note", Map.of(
                                "type", "string",
                                "description", "The note text to save when action=save"
                        )
                ),
                "required", new String[]{"action"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String action = String.valueOf(args.get("action"));
        if ("save".equals(action)) {
            String note = String.valueOf(args.getOrDefault("note", "")).trim();
            if (note.isEmpty()) return "Error: note text cannot be empty.";
            notes.add(note);
            return "Note saved: \"" + note + "\"";
        } else if ("list".equals(action)) {
            if (notes.isEmpty()) return "No notes saved yet.";
            List<String> copy = Collections.unmodifiableList(notes);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < copy.size(); i++) {
                sb.append(i + 1).append(". ").append(copy.get(i)).append("\n");
            }
            return sb.toString().trim();
        }
        return "Error: unknown action -> " + action;
    }
}
