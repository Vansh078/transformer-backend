package com.smart.transformer.dto.request;

import com.smart.transformer.entity.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRoleUpdateRequest {

    @NotNull(message = "Role is required")
    private RoleName role;
}
