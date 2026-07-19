package com.smart.transformer.repository;

import com.smart.transformer.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Page<ChatConversation> findByTransformerIdOrderByUpdatedAtDesc(Long transformerId, Pageable pageable);
    Page<ChatConversation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);
}
