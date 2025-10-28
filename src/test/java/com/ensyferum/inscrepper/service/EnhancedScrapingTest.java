package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.model.Content;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class EnhancedScrapingTest {

    @Autowired
    private EnhancedInstagramScraper enhancedScraper;

    @Test
    public void testEnhancedScrapingOncallpeds() {
        System.out.println("🚀 TESTE DO ENHANCED INSTAGRAM SCRAPER");
        
        // Criar perfil de teste
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Peds Enhanced")
                .active(true)
                .build();
                
        System.out.println("👤 Perfil: @" + profile.getUsername());
        
        try {
            // Executar scraping enhanced
            List<Content> contents = enhancedScraper.scrapeProfile(profile);
            
            System.out.println("📊 RESULTADOS:");
            System.out.println("  Posts encontrados: " + contents.size());
            
            for (int i = 0; i < contents.size(); i++) {
                Content content = contents.get(i);
                System.out.println("  " + (i + 1) + ". " + content.getExternalId() + 
                                 " (" + content.getType() + ")");
                System.out.println("     URL: " + content.getUrl());
                System.out.println("     Caption: " + content.getCaption());
            }
            
            if (contents.isEmpty()) {
                System.out.println("⚠️  Nenhum post foi encontrado");
                System.out.println("💡 Isso pode indicar:");
                System.out.println("   - Proteções anti-bot do Instagram");
                System.out.println("   - Rate limiting");
                System.out.println("   - Mudanças na estrutura da página");
                System.out.println("   - Necessidade de aguardar mais tempo");
            } else {
                System.out.println("✅ Scraping enhanced executado com sucesso!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro durante scraping: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testEnhancedScrapingWithSave() {
        System.out.println("💾 TESTE COM SALVAMENTO NO BANCO");
        
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Peds Save Test")
                .active(true)
                .build();
                
        try {
            List<Content> savedContents = enhancedScraper.scrapeAndSaveProfile(profile);
            
            System.out.println("📊 RESULTADOS SALVOS:");
            System.out.println("  Posts salvos: " + savedContents.size());
            
            for (Content content : savedContents) {
                System.out.println("  ✓ " + content.getExternalId() + 
                                 " (ID: " + content.getId() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro durante scraping com save: " + e.getMessage());
            e.printStackTrace();
        }
    }
}