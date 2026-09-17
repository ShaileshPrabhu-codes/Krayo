package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "auth_provider", nullable = false)
    private String authProvider = "LOCAL"; // LOCAL | GOOGLE

    @Column(name = "country_code", nullable = false)
    private String countryCode = "IN";

    @Column(nullable = false)
    private String role = "CUSTOMER"; // CUSTOMER | ADMIN

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
