package com.piagroup.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PIA Group AI Training - Homework Project
 * A custom-built, code-based AI Agent (not a visual/static workflow tool like n8n).
 * The agent decides for itself based on the user's message and calls a tool
 * when needed: calculator, date/time, weather, note-taking.
 */
@SpringBootApplication
public class AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
