package com.smart.transformer.controller;

import com.smart.transformer.dto.request.UserRequest;
import com.smart.transformer.dto.request.UserRoleUpdateRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.UserResponse;
import com.smart.transformer.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Phase 1 "User & Role Management" — mirrors Supabase Auth users locally for RBAC. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
  //  @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> provision(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success("User provisioned", userService.provision(request));
    }

    @GetMapping
    //@PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAll() {
        return ApiResponse.success(userService.getAll());
    }

    @GetMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PatchMapping("/{id}/role")
    //@PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateRole(@PathVariable UUID id, @Valid @RequestBody UserRoleUpdateRequest request) {
        return ApiResponse.success("Role updated", userService.updateRole(id, request));
    }

    @PatchMapping("/{id}/activate")
    //@PreAuthorize("hasRole('ADMIN')")
    
    public ApiResponse<UserResponse> activate(@PathVariable UUID id) {
        return ApiResponse.success("User activated", userService.setActive(id, true));
    }

    @PatchMapping("/{id}/deactivate")
   // @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.success("User deactivated", userService.setActive(id, false));
    }
}
