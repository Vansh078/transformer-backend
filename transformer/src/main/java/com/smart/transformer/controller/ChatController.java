package com.smart.transformer.controller;

import com.smart.transformer.dto.request.ChatRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.ChatResponse;
import com.smart.transformer.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** "Ask Your Transformer" — Phase 2 LLM chat feature. */
    @PostMapping("/ask")
    public ApiResponse<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.ask(request));
    }
}
