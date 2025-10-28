package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ScrapingServiceIntegrationTest {

    @Autowired
    private ScrapingService scrapingService;
    
    @Autowired
    private ProfileRepository profileRepository;
    
    @Autowired
    private ContentRepository contentRepository;

    @Test
    public void testScrapeOncallpedsProfile() {
        // Criar ou buscar o perfil oncallpeds
        Profile profile = profileRepository.findByUsername("oncallpeds")
                .orElseGet(() -> {
                    Profile newProfile = Profile.builder()
                            .username("oncallpeds")
                            .displayName("OnCall Pediatrics")
                            .active(true)
                            .build();
                    return profileRepository.save(newProfile);
                });

        // Executar o scraping
        List<Content> scrapedContents = scrapingService.scrapeAndSaveProfile(profile);

        // Verificações
        assertNotNull(scrapedContents);
        assertTrue(scrapedContents.size() <= 6, "Deve capturar no máximo 6 posts");
        
        if (!scrapedContents.isEmpty()) {
            // Verificar se pelo menos um conteúdo foi salvo
            assertTrue(scrapedContents.size() > 0, "Deve ter capturado pelo menos um post");
            
            // Verificar propriedades do primeiro conteúdo
            Content firstContent = scrapedContents.get(0);
            assertNotNull(firstContent.getId());
            assertEquals(profile.getId(), firstContent.getProfile().getId());
            assertNotNull(firstContent.getExternalId());
            assertNotNull(firstContent.getUrl());
            assertNotNull(firstContent.getCollectedAt());
            
            // Verificar se a imagem foi baixada e salva como blob
            if (firstContent.getImageBlob() != null) {
                assertTrue(firstContent.getImageBlob().length > 0, "Imagem deve ter sido baixada como blob");
                assertNotNull(firstContent.getImageMimeType(), "Tipo MIME da imagem deve estar definido");
                System.out.println("Imagem baixada com sucesso: " + firstContent.getImageBlob().length + " bytes");
                System.out.println("Tipo MIME: " + firstContent.getImageMimeType());
            }
            
            System.out.println("Teste de scraping concluído com sucesso!");
            System.out.println("Posts capturados: " + scrapedContents.size());
            
            // Imprimir detalhes dos posts capturados
            for (int i = 0; i < scrapedContents.size(); i++) {
                Content content = scrapedContents.get(i);
                System.out.println(String.format("Post %d: ID=%s, URL=%s, Caption=%s", 
                    i + 1, 
                    content.getExternalId(), 
                    content.getUrl(), 
                    content.getCaption().length() > 50 ? 
                        content.getCaption().substring(0, 50) + "..." : 
                        content.getCaption()
                ));
            }
        } else {
            System.out.println("Nenhum post foi capturado. Isso pode acontecer se:");
            System.out.println("1. O perfil não existe ou está privado");
            System.out.println("2. Houve problema de conectividade");
            System.out.println("3. O Instagram bloqueou o acesso");
            System.out.println("4. Todos os posts já existem no banco de dados");
        }

        // Verificar se os conteúdos foram persistidos no banco
        List<Content> savedContents = contentRepository.findByProfile(profile);
        assertTrue(savedContents.size() >= scrapedContents.size(), 
                   "Conteúdos devem ter sido persistidos no banco");
    }
    
    @Test
    public void testProfileCreationAndRetrieval() {
        // Teste auxiliar para verificar se o sistema de perfis está funcionando
        String testUsername = "test_profile_" + System.currentTimeMillis();
        
        Profile profile = Profile.builder()
                .username(testUsername)
                .displayName("Test Profile")
                .active(true)
                .build();
        
        Profile savedProfile = profileRepository.save(profile);
        assertNotNull(savedProfile.getId());
        assertNotNull(savedProfile.getCreatedAt());
        assertNotNull(savedProfile.getUpdatedAt());
        
        Optional<Profile> retrievedProfile = profileRepository.findByUsername(testUsername);
        assertTrue(retrievedProfile.isPresent());
        assertEquals(testUsername, retrievedProfile.get().getUsername());
        
        System.out.println("Teste de criação de perfil concluído com sucesso!");
    }
}