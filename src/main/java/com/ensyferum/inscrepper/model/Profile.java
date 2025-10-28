package com.ensyferum.inscrepper.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_profiles_username", columnNames = {"username"})
})
public class Profile {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 150)
    private String displayName;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
