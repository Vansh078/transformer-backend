package com.smart.transformer.dto.response;

import com.smart.transformer.entity.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private ChatRole role;
    private String content;
    private Instant createdAt;
}
