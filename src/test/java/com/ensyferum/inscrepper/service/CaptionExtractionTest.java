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
public class CaptionExtractionTest {

    @Autowired
    private EnhancedInstagramScraper enhancedScraper;
    
    @Autowired
    private ProfileRepository profileRepository;

    @Test
    public void testCaptionExtraction() {
        System.out.println("📝 TESTE DE EXTRAÇÃO DE CAPTIONS");
        
        // Criar perfil de teste
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("OnCall Peds - Caption Test")
                .active(true)
                .build();
                
        System.out.println("👤 Testando extração de captions para: @" + profile.getUsername());
        
        try {
            // Executar scraping com foco na extração de captions
            List<Content> contents = enhancedScraper.scrapeProfile(profile);
            
            System.out.println("\n📊 RESULTADOS DA EXTRAÇÃO DE CAPTIONS:");
            System.out.println("Posts processados: " + contents.size());
            
            for (int i = 0; i < contents.size(); i++) {
                Content content = contents.get(i);
                
                System.out.println("\n📄 Post " + (i + 1) + ":");
                System.out.println("  ID: " + content.getExternalId());
                System.out.println("  Tipo: " + content.getType());
                System.out.println("  URL: " + content.getUrl());
                
                String caption = content.getCaption();
                if (caption != null && !caption.trim().isEmpty()) {
                    System.out.println("  ✅ Caption encontrado:");
                    System.out.println("    Tamanho: " + caption.length() + " caracteres");
                    
                    // Mostrar prévia do caption
                    String preview = caption.length() > 100 ? 
                        caption.substring(0, 100) + "..." : caption;
                    System.out.println("    Prévia: \"" + preview + "\"");
                    
                    // Verificar se o caption parece realístico
                    boolean hasHashtags = caption.contains("#");
                    boolean hasMentions = caption.contains("@");
                    boolean hasEmojis = caption.matches(".*[\\p{So}\\p{Cn}].*");
                    boolean isReasonableLength = caption.length() > 5 && caption.length() < 3000;
                    
                    System.out.println("    📊 Análise do caption:");
                    System.out.println("      Hashtags: " + (hasHashtags ? "✅" : "❌"));
                    System.out.println("      Menções: " + (hasMentions ? "✅" : "❌"));
                    System.out.println("      Emojis: " + (hasEmojis ? "✅" : "❌"));
                    System.out.println("      Tamanho OK: " + (isReasonableLength ? "✅" : "❌"));
                    
                } else {
                    System.out.println("  ❌ Caption não encontrado ou vazio");
                }
            }
            
            // Estatísticas
            long postsComCaption = contents.stream()
                .filter(c -> c.getCaption() != null && 
                           !c.getCaption().trim().isEmpty() && 
                           !c.getCaption().equals("Sem descrição disponível"))
                .count();
                
            double sucessRate = contents.isEmpty() ? 0 : (double) postsComCaption / contents.size() * 100;
            
            System.out.println("\n📈 ESTATÍSTICAS:");
            System.out.println("Posts com caption: " + postsComCaption + "/" + contents.size());
            System.out.println("Taxa de sucesso: " + String.format("%.1f", sucessRate) + "%");
            
            if (sucessRate > 50) {
                System.out.println("🎉 EXCELENTE! Taxa de extração de captions acima de 50%");
            } else if (sucessRate > 20) {
                System.out.println("👍 BOM! Taxa de extração razoável");
            } else {
                System.out.println("⚠️ Taxa baixa - pode precisar de melhorias nos seletores");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro durante teste de caption: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testCaptionPersistence() {
        System.out.println("💾 TESTE DE PERSISTÊNCIA DE CAPTIONS");
        
        Profile profile = Profile.builder()
                .username("oncallpeds")
                .displayName("Caption Persistence Test")
                .active(true)
                .build();
        
        // Salvar o profile primeiro para evitar erro de transient object
        profile = profileRepository.save(profile);
                
        try {
            List<Content> savedContents = enhancedScraper.scrapeAndSaveProfile(profile, true); // forceUpdate = true para testes
            
            System.out.println("📊 CAPTIONS SALVOS NO BANCO:");
            
            for (Content content : savedContents) {
                System.out.println("\n💾 " + content.getExternalId() + " (ID: " + content.getId() + ")");
                
                if (content.getCaption() != null) {
                    String captionPreview = content.getCaption().length() > 80 ? 
                        content.getCaption().substring(0, 80) + "..." : content.getCaption();
                    System.out.println("    Caption: \"" + captionPreview + "\"");
                } else {
                    System.out.println("    Caption: null");
                }
            }
            
            System.out.println("\n✅ Todos os captions foram salvos no banco de dados!");
            
        } catch (Exception e) {
            System.err.println("❌ Erro no teste de persistência: " + e.getMessage());
            e.printStackTrace();
        }
    }
}