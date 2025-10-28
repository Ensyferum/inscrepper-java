package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class ScrapingDebugTest {

    @Autowired
    private ScrapingService scrapingService;
    
    @Autowired
    private ProfileRepository profileRepository;

    @Test
    public void testScrapeOncallpedsWithDebug() {
        // Criar perfil de teste
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Pediatrics")
                .active(true)
                .build();
        
        profile = profileRepository.save(profile);
        
        System.out.println("=== INICIANDO TESTE DE SCRAPING DEBUG ===");
        System.out.println("Perfil: @" + profile.getUsername());
        
        // Executar scraping
        List<Content> contents = scrapingService.scrapeProfile(profile);
        
        System.out.println("=== RESULTADOS ===");
        System.out.println("Posts encontrados: " + contents.size());
        
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            System.out.println(String.format("Post %d:", i + 1));
            System.out.println("  - Shortcode: " + content.getExternalId());
            System.out.println("  - URL: " + content.getUrl());
            System.out.println("  - Imagem URL: " + content.getMediaUrl());
            System.out.println("  - Caption: " + (content.getCaption() != null && content.getCaption().length() > 50 ? 
                content.getCaption().substring(0, 50) + "..." : content.getCaption()));
            System.out.println("  - Tem blob de imagem: " + (content.getImageBlob() != null ? 
                content.getImageBlob().length + " bytes" : "Não"));
            System.out.println();
        }
        
        System.out.println("=== FIM DO TESTE ===");
    }
}