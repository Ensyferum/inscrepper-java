package com.ensyferum.inscrepper.web;

import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
public class AdminController {

    private final ProfileService profileService;

    public AdminController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        List<Profile> profiles = profileService.listAll();
        model.addAttribute("profiles", profiles);
        return "admin";
    }

    @PostMapping("/admin/profiles")
    public String addProfile(@RequestParam("username") String username,
                             @RequestParam(value = "displayName", required = false) String displayName,
                             Model model) {
        try {
            profileService.create(username, displayName);
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("profiles", profileService.listAll());
            return "admin";
        }
    }

    @PostMapping("/admin/profiles/{id}/delete")
    public String deleteProfile(@PathVariable("id") UUID id) {
        profileService.delete(id);
        return "redirect:/admin";
    }
}
