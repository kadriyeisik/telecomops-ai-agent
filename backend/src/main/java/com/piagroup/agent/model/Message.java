package com.piagroup.agent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Role role; // USER, ASSISTANT, TOOL

    @Column(length = 8000)
    private String content;

    /** If this message is a tool call/result, which tool it belongs to */
    private String toolName;

    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    public enum Role { USER, ASSISTANT, TOOL, SYSTEM }
}
