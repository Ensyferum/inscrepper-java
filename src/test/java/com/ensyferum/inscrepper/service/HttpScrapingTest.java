package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class HttpScrapingTest {

    @Autowired
    private HttpInstagramScraper httpScraper;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Test
    public void testHttpScrapingOncallpeds() {
        System.out.println("🚀 INICIANDO TESTE HTTP SCRAPING");
        
        // Criar perfil de teste
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Peds")
                .active(true)
                .build();

        profile = profileRepository.save(profile);
        System.out.println("👤 Perfil criado: @" + profile.getUsername());
        
        // Fazer análise HTML primeiro
        System.out.println("🔍 FAZENDO ANÁLISE HTML...");
        httpScraper.analyzeProfileHtml(profile);
        
        // Tentar scraping
        System.out.println("\n🎯 INICIANDO SCRAPING...");
        List<Content> contents = httpScraper.scrapeProfile(profile);
        
        System.out.println("✅ RESULTADO:");
        System.out.println("   Posts encontrados: " + contents.size());
        
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            System.out.println("   " + (i + 1) + ". " + content.getExternalId() + 
                             " | " + content.getType() + 
                             " | " + content.getUrl());
        }
        
        // Salvar no banco
        if (!contents.isEmpty()) {
            contentRepository.saveAll(contents);
            System.out.println("💾 " + contents.size() + " posts salvos no banco");
        }
        
        System.out.println("🏁 TESTE CONCLUÍDO");
    }
    
    @Test
    public void testSimpleHttpRequest() {
        System.out.println("🌐 TESTE SIMPLES DE HTTP REQUEST");
        
        Profile testProfile = Profile.builder()
                .username("oncallpeds")
                .displayName("Test")
                .build();
                
        httpScraper.analyzeProfileHtml(testProfile);
    }
}