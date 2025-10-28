package com.ensyferum.inscrepper.web;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.service.ProfileService;
import com.ensyferum.inscrepper.service.ScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ScrapingService scrapingService;
    private final ContentRepository contentRepository;

    @GetMapping
    public String listProfiles(Model model,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Profile> profilePage = profileService.findAll(pageable);
        
        model.addAttribute("profilePage", profilePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", profilePage.getTotalPages());
        
        return "profiles/list";
    }

    @GetMapping("/{id}")
    public String viewProfile(@PathVariable UUID id, Model model) {
        Optional<Profile> profileOpt = profileService.findById(id);
        
        if (profileOpt.isEmpty()) {
            return "redirect:/profiles?error=profile-not-found";
        }
        
        Profile profile = profileOpt.get();
        
        // Buscar conteúdos do perfil
        List<Content> contents = contentRepository.findByProfile(profile);
        
        // Estatísticas
        long totalPosts = contents.size();
        long postsWithImages = contents.stream()
                .mapToLong(c -> c.getImageBlob() != null ? 1 : 0)
                .sum();
        
        model.addAttribute("profile", profile);
        model.addAttribute("contents", contents);
        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("postsWithImages", postsWithImages);
        
        return "profiles/detail";
    }

    @PostMapping("/{id}/scrape")
    public String executeScraping(@PathVariable UUID id, 
                                 @RequestParam(defaultValue = "enhanced") String type,
                                 RedirectAttributes redirectAttributes) {
        try {
            Optional<Profile> profileOpt = profileService.findById(id);
            
            if (profileOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Perfil não encontrado");
                return "redirect:/profiles";
            }
            
            Profile profile = profileOpt.get();
            
            log.info("🚀 Iniciando scraping {} para o perfil: @{}", type, profile.getUsername());
            
            List<Content> scrapedContents;
            
            if ("enhanced".equals(type)) {
                scrapedContents = profileService.scrapeProfile(id);
                log.info("✅ Enhanced scraping executado para @{}", profile.getUsername());
            } else {
                scrapedContents = scrapingService.scrapeAndSaveProfile(profile);
                log.info("✅ Scraping básico executado para @{}", profile.getUsername());
            }
            
            String message = String.format("🎯 Scraping %s concluído! %d novos posts foram capturados para @%s", 
                    type, scrapedContents.size(), profile.getUsername());
            
            redirectAttributes.addFlashAttribute("success", message);
            
        } catch (Exception e) {
            log.error("❌ Erro durante o scraping {}: {}", type, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                String.format("Erro no scraping %s: %s", type, e.getMessage()));
        }
        
        return "redirect:/profiles/" + id;
    }
    
    @PostMapping("/{id}/scrape-enhanced")
    public String executeEnhancedScraping(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            log.info("🚀 Iniciando Enhanced Scraping para perfil ID: {}", id);
            
            List<Content> scrapedContents = profileService.scrapeProfile(id);
            
            String message = String.format("🎯 Enhanced Scraping concluído! %d novos posts foram capturados", 
                    scrapedContents.size());
            
            redirectAttributes.addFlashAttribute("success", message);
            log.info("✅ Enhanced scraping concluído com {} posts", scrapedContents.size());
            
        } catch (Exception e) {
            log.error("❌ Erro no Enhanced Scraping: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Erro no Enhanced Scraping: " + e.getMessage());
        }
        
        return "redirect:/profiles/" + id;
    }

    @GetMapping("/{id}/posts")
    public String viewPosts(@PathVariable UUID id, Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "12") int size) {
        
        Optional<Profile> profileOpt = profileService.findById(id);
        
        if (profileOpt.isEmpty()) {
            return "redirect:/profiles?error=profile-not-found";
        }
        
        Profile profile = profileOpt.get();
        List<Content> contents = contentRepository.findByProfile(profile);
        
        // Simular paginação (você pode implementar paginação no repository depois)
        int start = page * size;
        int end = Math.min(start + size, contents.size());
        List<Content> pagedContents = contents.subList(start, end);
        
        int totalPages = (int) Math.ceil((double) contents.size() / size);
        
        model.addAttribute("profile", profile);
        model.addAttribute("contents", pagedContents);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalPosts", contents.size());
        
        return "profiles/posts";
    }

    @GetMapping("/{id}/analytics")
    public String viewAnalytics(@PathVariable UUID id, Model model) {
        Optional<Profile> profileOpt = profileService.findById(id);
        
        if (profileOpt.isEmpty()) {
            return "redirect:/profiles?error=profile-not-found";
        }
        
        Profile profile = profileOpt.get();
        List<Content> contents = contentRepository.findByProfile(profile);
        
        // Estatísticas básicas
        long totalPosts = contents.size();
        long postsWithImages = contents.stream()
                .mapToLong(c -> c.getImageBlob() != null ? 1 : 0)
                .sum();
        
        double imagePercentage = totalPosts > 0 ? (double) postsWithImages / totalPosts * 100 : 0;
        
        // Análise de captions
        long postsWithCaption = contents.stream()
                .mapToLong(c -> c.getCaption() != null && !c.getCaption().trim().isEmpty() ? 1 : 0)
                .sum();
        
        double captionPercentage = totalPosts > 0 ? (double) postsWithCaption / totalPosts * 100 : 0;
        
        model.addAttribute("profile", profile);
        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("postsWithImages", postsWithImages);
        model.addAttribute("imagePercentage", String.format("%.1f", imagePercentage));
        model.addAttribute("postsWithCaption", postsWithCaption);
        model.addAttribute("captionPercentage", String.format("%.1f", captionPercentage));
        
        return "profiles/analytics";
    }

    @GetMapping("/image/{contentId}")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID contentId) {
        Optional<Content> contentOpt = contentRepository.findById(contentId);
        
        if (contentOpt.isEmpty() || contentOpt.get().getImageBlob() == null) {
            return ResponseEntity.notFound().build();
        }
        
        Content content = contentOpt.get();
        
        HttpHeaders headers = new HttpHeaders();
        
        if (content.getImageMimeType() != null) {
            headers.setContentType(MediaType.parseMediaType(content.getImageMimeType()));
        } else {
            headers.setContentType(MediaType.IMAGE_JPEG);
        }
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(content.getImageBlob());
    }
}