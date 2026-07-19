package com.smart.transformer.dto.request;

import com.smart.transformer.entity.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Provisions/updates the local `app_users` mirror of a Supabase Auth user.
 * The id MUST match the Supabase auth.users UUID (the JWT "sub" claim) so RBAC
 * lookups line up between Supabase Auth and this table.
 */
@Getter
@Setter
public class UserRequest {

    @NotNull(message = "User id (Supabase auth UUID) is required")
    private UUID id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String fullName;

    @NotNull(message = "Role is required")
    private RoleName role;
}
