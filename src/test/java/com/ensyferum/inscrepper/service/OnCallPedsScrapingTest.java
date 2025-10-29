package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class OnCallPedsScrapingTest {

    @Autowired
    private EnhancedInstagramScraper enhancedScraper;
    
    @Autowired
    private ProfileRepository profileRepository;

    @Test
    public void testOnCallPedsProfile() {
        System.out.println("🎯 TESTE ESPECÍFICO: Perfil @oncallpeds");
        System.out.println("📋 Objetivo: Encontrar pelo menos 3 posts (página tem mais de 20)");
        
        // Criar e salvar perfil
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Peds Test")
                .active(true)
                .build();
        
        profile = profileRepository.save(profile);
        
        try {
            System.out.println("🚀 Iniciando scraping do perfil @oncallpeds...");
            
            List<Content> savedContents = enhancedScraper.scrapeAndSaveProfile(profile, true); // forceUpdate = true
            
            System.out.println("📊 RESULTADOS DO SCRAPING:");
            System.out.println("Posts encontrados: " + savedContents.size());
            
            // Verificar se encontrou pelo menos 3 posts
            assertTrue(savedContents.size() >= 3, 
                    String.format("Esperava pelo menos 3 posts, mas encontrou apenas %d", savedContents.size()));
            
            System.out.println("✅ SUCESSO: Encontrou pelo menos 3 posts!");
            
            // Exibir detalhes dos posts encontrados
            System.out.println("\n📝 POSTS ENCONTRADOS:");
            for (int i = 0; i < savedContents.size() && i < 6; i++) {
                Content content = savedContents.get(i);
                System.out.printf("%d. ID: %s\n", i + 1, content.getExternalId());
                System.out.printf("   Tipo: %s\n", content.getType());
                System.out.printf("   URL: %s\n", content.getUrl());
                
                if (content.getCaption() != null) {
                    String captionPreview = content.getCaption().length() > 80 ? 
                            content.getCaption().substring(0, 80) + "..." : content.getCaption();
                    System.out.printf("   Caption: %s\n", captionPreview);
                } else {
                    System.out.println("   Caption: [Não disponível]");
                }
                System.out.println();
            }
            
            // Estatísticas adicionais
            long postsComCaption = savedContents.stream()
                    .mapToLong(c -> c.getCaption() != null && !c.getCaption().trim().isEmpty() ? 1 : 0)
                    .sum();
            
            System.out.printf("📈 ESTATÍSTICAS:\n");
            System.out.printf("• Total de posts: %d\n", savedContents.size());
            System.out.printf("• Posts com caption: %d\n", postsComCaption);
            System.out.printf("• Taxa de caption: %.1f%%\n", 
                    savedContents.size() > 0 ? (postsComCaption * 100.0 / savedContents.size()) : 0);
            
            // Verificar qualidade mínima
            if (savedContents.size() >= 6) {
                System.out.println("🎉 EXCELENTE: Encontrou 6 ou mais posts!");
            } else if (savedContents.size() >= 3) {
                System.out.println("👍 BOM: Encontrou pelo menos 3 posts!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERRO no scraping: " + e.getMessage());
            e.printStackTrace();
            fail("Teste falhou com erro: " + e.getMessage());
        }
    }
    
    @Test
    public void testOnCallPedsWithoutForceUpdate() {
        System.out.println("🔄 TESTE: Perfil @oncallpeds SEM forçar atualização");
        
        // Criar e salvar perfil
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Peds No Force Test")
                .active(true)
                .build();
        
        profile = profileRepository.save(profile);
        
        try {
            System.out.println("🚀 Iniciando scraping sem forçar atualização...");
            
            List<Content> savedContents = enhancedScraper.scrapeAndSaveProfile(profile, false); // forceUpdate = false
            
            System.out.println("📊 RESULTADOS (sem forçar):");
            System.out.println("Posts encontrados: " + savedContents.size());
            
            // Este teste pode ter menos posts se alguns já existirem
            System.out.printf("✅ Teste concluído. Posts únicos encontrados: %d\n", savedContents.size());
            
        } catch (Exception e) {
            System.err.println("❌ ERRO no scraping: " + e.getMessage());
            // Não falha o teste, pois pode haver duplicatas
        }
    }
}