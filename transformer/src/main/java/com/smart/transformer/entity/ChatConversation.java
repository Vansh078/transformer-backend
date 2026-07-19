package com.smart.transformer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A single "Ask Your Transformer" conversation thread. Scoping messages to a
 * conversation (rather than one-shot Q&A) lets the LLM keep prior turns in
 * context, which is what makes the chat feel conversational.
 */
@Getter
@Setter
@Entity
@Table(name = "chat_conversations")
public class ChatConversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id")
    private Transformer transformer;

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 200)
    private String title;
}
