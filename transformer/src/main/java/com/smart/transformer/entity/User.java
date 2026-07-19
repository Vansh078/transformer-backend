package com.smart.transformer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Mirrors a user provisioned via Supabase Auth.
 * The primary key is the Supabase auth.users UUID ("sub" claim in the JWT),
 * NOT an auto-generated local id — this keeps the two systems in sync.
 */
@Getter
@Setter
@Entity
@Table(name = "app_users")
public class User extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
