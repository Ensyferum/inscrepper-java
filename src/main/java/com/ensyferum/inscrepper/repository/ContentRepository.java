package com.ensyferum.inscrepper.repository;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {
    List<Content> findByProfile(Profile profile);
    Optional<Content> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);
}
