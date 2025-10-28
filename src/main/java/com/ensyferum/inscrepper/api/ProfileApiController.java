package com.ensyferum.inscrepper.api;

import com.ensyferum.inscrepper.model.Profile;
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

    public ProfileApiController(ProfileService profileService) {
        this.profileService = profileService;
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
}
