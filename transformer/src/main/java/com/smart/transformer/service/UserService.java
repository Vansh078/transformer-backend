package com.smart.transformer.service;

import com.smart.transformer.dto.request.UserRequest;
import com.smart.transformer.dto.request.UserRoleUpdateRequest;
import com.smart.transformer.dto.response.UserResponse;
import com.smart.transformer.entity.Role;
import com.smart.transformer.entity.User;
import com.smart.transformer.entity.enums.AuthProvider;
import com.smart.transformer.exception.DuplicateResourceException;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.RoleRepository;
import com.smart.transformer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Phase 1 "User & Role Management". The Supabase Auth project owns credentials/sessions;
 * this table mirrors the resulting users so the backend can enforce RBAC (roles: ADMIN,
 * ENGINEER, VIEWER) without round-tripping to Supabase on every request.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public UserResponse provision(UserRequest request) {
        if (userRepository.existsById(request.getId())) {
            throw new DuplicateResourceException("A user with id '" + request.getId() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user with email '" + request.getEmail() + "' already exists");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        User user = new User();
        user.setId(request.getId());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setOrganization(request.getOrganization());
        user.setAuthProvider(request.getAuthProvider() != null ? request.getAuthProvider() : AuthProvider.EMAIL);
        user.setRole(role);
        user.setActive(true);

        UserResponse response = toResponse(userRepository.save(user));
        activityLogService.record("USER_PROVISIONED", "User", user.getId(), request.getEmail());
        return response;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse updateRole(UUID id, UserRoleUpdateRequest request) {
        User user = getEntity(id);
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));
        user.setRole(role);
        UserResponse response = toResponse(userRepository.save(user));
        activityLogService.record("USER_ROLE_CHANGED", "User", id, "New role: " + request.getRole());
        return response;
    }

    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        User user = getEntity(id);
        user.setActive(active);
        UserResponse response = toResponse(userRepository.save(user));
        activityLogService.record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User", id, null);
        return response;
    }

    private User getEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getFullName(),
                u.getRole() != null ? u.getRole().getName() : null,
                u.isActive(), u.getCreatedAt(),
                u.getAuthProvider(), u.getOrganization());
    }
}