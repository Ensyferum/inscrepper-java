package com.ensyferum.inscrepper.repository;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.ContentType;
import com.ensyferum.inscrepper.model.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {
    List<Content> findByProfile(Profile profile);
    List<Content> findByProfileOrderByCollectedAtDesc(Profile profile);
    Optional<Content> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);
    
    // Métodos com paginação
    Page<Content> findByProfile(Profile profile, Pageable pageable);
    Page<Content> findByType(ContentType type, Pageable pageable);
    Page<Content> findByProfileAndType(Profile profile, ContentType type, Pageable pageable);
}
