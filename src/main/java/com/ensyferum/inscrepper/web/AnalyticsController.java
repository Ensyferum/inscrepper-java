package com.ensyferum.inscrepper.web;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ProfileRepository profileRepository;
    private final ContentRepository contentRepository;

    @GetMapping
    public String analyticsHome(Model model) {
        List<Profile> profiles = profileRepository.findAll();
        model.addAttribute("profiles", profiles);
        return "analytics";
    }

    @GetMapping("/profile/{username}")
    public String profileAnalytics(@PathVariable String username, Model model) {
        Optional<Profile> profileOpt = profileRepository.findByUsername(username);
        
        if (profileOpt.isEmpty()) {
            model.addAttribute("error", "Perfil não encontrado: " + username);
            return "analytics";
        }
        
        Profile profile = profileOpt.get();
        List<Content> contents = contentRepository.findByProfileOrderByCollectedAtDesc(profile);
        
        model.addAttribute("profile", profile);
        model.addAttribute("contents", contents);
        model.addAttribute("totalContents", contents.size());
        
        // Calcular estatísticas
        long totalLikes = contents.stream()
                .mapToLong(c -> c.getLikesCount() != null ? c.getLikesCount() : 0)
                .sum();
        
        long totalComments = contents.stream()
                .mapToLong(c -> c.getCommentsCount() != null ? c.getCommentsCount() : 0)
                .sum();
        
        long totalViews = contents.stream()
                .mapToLong(c -> c.getViewsCount() != null ? c.getViewsCount() : 0)
                .sum();
        
        double avgLikes = contents.isEmpty() ? 0 : (double) totalLikes / contents.size();
        double avgComments = contents.isEmpty() ? 0 : (double) totalComments / contents.size();
        double avgViews = contents.isEmpty() ? 0 : (double) totalViews / contents.size();
        
        model.addAttribute("totalLikes", totalLikes);
        model.addAttribute("totalComments", totalComments);
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("avgLikes", avgLikes);
        model.addAttribute("avgComments", avgComments);
        model.addAttribute("avgViews", avgViews);
        
        return "profile-analytics";
    }
}
