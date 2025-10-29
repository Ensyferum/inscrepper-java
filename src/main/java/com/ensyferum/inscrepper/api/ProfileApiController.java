package com.ensyferum.inscrepper.api;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.service.ProfileService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileApiController {

    private final ProfileService profileService;
    private final ContentRepository contentRepository;

    public ProfileApiController(ProfileService profileService, ContentRepository contentRepository) {
        this.profileService = profileService;
        this.contentRepository = contentRepository;
    }

    @GetMapping
    public List<Profile> list() {
        return profileService.listAll();
    }

    public record CreateRequest(@NotBlank String username, String displayName) {}

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRequest req) {
        try {
            Profile p = profileService.create(req.username(), req.displayName());
            return ResponseEntity.ok(p);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/contents")
    public ResponseEntity<?> getContents(@PathVariable UUID id) {
        Profile profile = profileService.findById(id).orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<Content> contents = contentRepository.findByProfileOrderByCollectedAtDesc(profile);
        
        return ResponseEntity.ok(Map.of(
            "profile", Map.of(
                "id", profile.getId(),
                "username", profile.getUsername(),
                "displayName", profile.getDisplayName()
            ),
            "totalContents", contents.size(),
            "contents", contents.stream().map(c -> Map.of(
                "id", c.getId(),
                "externalId", c.getExternalId(),
                "type", c.getType().toString(),
                "url", c.getUrl() != null ? c.getUrl() : "",
                "mediaUrl", c.getMediaUrl() != null ? c.getMediaUrl() : "",
                "caption", c.getCaption() != null ? c.getCaption() : "",
                "publishedAt", c.getPublishedAt() != null ? c.getPublishedAt().toString() : "",
                "collectedAt", c.getCollectedAt().toString(),
                "hasImage", c.getImageBlob() != null,
                "imageSizeKB", c.getImageBlob() != null ? c.getImageBlob().length / 1024 : 0
            )).toList()
        ));
    }
    
    @GetMapping("/username/{username}/contents")
    public ResponseEntity<?> getContentsByUsername(@PathVariable String username) {
        Profile profile = profileService.findByUsername(username).orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        
        return getContents(profile.getId());
    }
}
