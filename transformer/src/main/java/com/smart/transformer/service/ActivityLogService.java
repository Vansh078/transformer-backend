package com.smart.transformer.service;

import com.smart.transformer.dto.response.ActivityLogResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.entity.ActivityLog;
import com.smart.transformer.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Phase 4 "Activity Logs" — a lightweight, append-only audit trail.
 * Call {@link #record} from services after a significant write action succeeds.
 */
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void record(String action, String entityType, Object entityId, String details) {
        ActivityLog log = new ActivityLog();
        log.setUserId(currentUserId());
        log.setActorLabel(currentActorLabel());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId != null ? entityId.toString() : null);
        log.setDetails(details);
        activityLogRepository.save(log);
    }

    public PagedResponse<ActivityLogResponse> getAll(Pageable pageable) {
        return PagedResponse.from(activityLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse));
    }

    public PagedResponse<ActivityLogResponse> getByEntity(String entityType, String entityId, Pageable pageable) {
        var page = entityId != null
                ? activityLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable)
                : activityLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    private ActivityLogResponse toResponse(ActivityLog a) {
        return new ActivityLogResponse(
                a.getId(), a.getUserId(), a.getActorLabel(), a.getAction(),
                a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt());
    }

    private UUID currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                return UUID.fromString(jwt.getSubject());
            }
        } catch (Exception ignored) {
            // JWT subject isn't a UUID (e.g. system/device call) — fall back to null
        }
        return null;
    }

    private String currentActorLabel() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                Object email = jwt.getClaims().get("email");
                return email != null ? email.toString() : jwt.getSubject();
            }
        } catch (Exception ignored) {
        }
        return "system";
    }
}
