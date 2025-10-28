package com.ensyferum.inscrepper.repository;

import com.ensyferum.inscrepper.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUsername(String username);
    Optional<Profile> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
