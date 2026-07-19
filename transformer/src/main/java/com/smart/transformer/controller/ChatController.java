package com.smart.transformer.controller;

import com.smart.transformer.dto.request.ChatRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.ChatConversationResponse;
import com.smart.transformer.dto.response.ChatResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.service.ChatService;
import com.smart.transformer.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** "Ask Your Transformer" — Phase 2 LLM chat feature, powered by OpenAI. */
    @PostMapping("/ask")
    public ApiResponse<ChatResponse> ask(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.ask(request));
    }

    /** Lists chat conversations for a transformer (most recently updated first). */
    @GetMapping("/transformer/{transformerId}/conversations")
    public ApiResponse<PagedResponse<ChatConversationResponse>> getConversations(
            @PathVariable Long transformerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageUtil.of(page, size, "updatedAt", "desc");
        return ApiResponse.success(chatService.getConversations(transformerId, pageable));
    }

    /** Fetches a single conversation with its full message history. */
    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<ChatConversationResponse> getConversation(@PathVariable Long conversationId) {
        return ApiResponse.success(chatService.getConversation(conversationId));
    }
}
