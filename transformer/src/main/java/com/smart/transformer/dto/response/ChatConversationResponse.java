package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationResponse {
    private Long id;
    private Long transformerId;
    private String transformerName;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ChatMessageResponse> messages; // null in list views, populated in detail view
}
