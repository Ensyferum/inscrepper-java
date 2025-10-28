package com.ensyferum.inscrepper.web;

import com.ensyferum.inscrepper.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProfileService profileService;

    public HomeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // Passar dados dos perfis para a página inicial
        model.addAttribute("profiles", profileService.listAll());
        return "home";
    }
}
