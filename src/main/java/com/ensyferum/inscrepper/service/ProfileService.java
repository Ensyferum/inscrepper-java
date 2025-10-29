package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EnhancedInstagramScraper enhancedScraper;

    public List<Profile> listAll() {
        return profileRepository.findAll();
    }

    @Transactional
    public Profile create(String username, String displayName) {
        String normalized = username == null ? null : username.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("username is required");
        }
        if (profileRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new IllegalArgumentException("username already exists");
        }
        Profile p = Profile.builder()
                .username(normalized)
                .displayName(displayName)
                .active(true)
                .build();
        return profileRepository.save(p);
    }

    @Transactional
    public void delete(UUID id) {
        if (id == null) return;
        profileRepository.deleteById(id);
    }
    
    public Page<Profile> findAll(Pageable pageable) {
        return profileRepository.findAll(pageable);
    }
    
    public Optional<Profile> findById(UUID id) {
        return profileRepository.findById(id);
    }
    
    public Optional<Profile> findByUsername(String username) {
        return profileRepository.findByUsername(username);
    }
    
    public List<Content> scrapeProfile(UUID profileId) {
        return scrapeProfile(profileId, false);
    }
    
    public List<Content> scrapeProfile(UUID profileId, boolean forceUpdate) {
        log.info("🚀 Iniciando scraping enhanced para perfil ID: {} (forceUpdate: {})", profileId, forceUpdate);
        
        Optional<Profile> profileOpt = findById(profileId);
        if (profileOpt.isEmpty()) {
            throw new IllegalArgumentException("Profile not found: " + profileId);
        }
        
        Profile profile = profileOpt.get();
        
        try {
            List<Content> results = enhancedScraper.scrapeAndSaveProfile(profile, forceUpdate);
            log.info("✅ Scraping concluído para @{}: {} posts", profile.getUsername(), results.size());
            return results;
        } catch (Exception e) {
            log.error("❌ Erro no scraping para @{}: {}", profile.getUsername(), e.getMessage());
            throw new RuntimeException("Falha no scraping: " + e.getMessage(), e);
        }
    }
}
