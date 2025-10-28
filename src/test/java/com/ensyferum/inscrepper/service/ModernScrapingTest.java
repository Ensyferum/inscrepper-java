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
public class ModernScrapingTest {

    @Autowired
    private ModernInstagramScraper modernScraper;
    
    @Autowired
    private ProfileRepository profileRepository;

    @Test
    public void testModernScrapingOncallpeds() {
        System.out.println("=== TESTE DO NOVO SCRAPER MODERNO ===");
        
        // Criar perfil de teste
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Pediatrics")
                .active(true)
                .build();
        
        profile = profileRepository.save(profile);
        
        System.out.println("Perfil criado: @" + profile.getUsername());
        
        // Executar scraping com o novo scraper
        List<Content> contents = modernScraper.scrapeAndSaveProfile(profile);
        
        System.out.println("\n=== RESULTADOS ===");
        System.out.println("Posts encontrados: " + contents.size());
        
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            System.out.println(String.format("\nPost %d:", i + 1));
            System.out.println("  - Shortcode: " + content.getExternalId());
            System.out.println("  - URL: " + content.getUrl());
            System.out.println("  - Tipo: " + content.getType());
            System.out.println("  - Coletado em: " + content.getCollectedAt());
            System.out.println("  - Imagem URL: " + (content.getMediaUrl() != null ? content.getMediaUrl() : "N/A"));
            System.out.println("  - Tem blob: " + (content.getImageBlob() != null ? 
                content.getImageBlob().length + " bytes" : "Não"));
        }
        
        if (contents.isEmpty()) {
            System.out.println("\n⚠️ NENHUM POST ENCONTRADO");
            System.out.println("Isso pode indicar que:");
            System.out.println("1. O Instagram mudou sua estrutura HTML");
            System.out.println("2. O perfil está privado");
            System.out.println("3. O Instagram está bloqueando nossa automação");
            System.out.println("4. Não há posts públicos no perfil");
        } else {
            System.out.println("\n✅ SUCESSO! Posts capturados com êxito.");
        }
        
        System.out.println("\n=== FIM DO TESTE ===");
    }
}