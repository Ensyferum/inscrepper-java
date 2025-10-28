package com.ensyferum.inscrepper.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@SpringBootTest
@ActiveProfiles("test")
public class InstagramApiTest {

    @Test
    public void testInstagramApi() {
        System.out.println("🌐 TESTANDO API DIRETA DO INSTAGRAM");
        
        String[] apiUrls = {
            "https://www.instagram.com/api/v1/users/web_profile_info/?username=oncallpeds",
            "https://www.instagram.com/oncallpeds/?__a=1",
            "https://www.instagram.com/oncallpeds/?__a=1&__d=dis",
            "https://i.instagram.com/api/v1/users/web_profile_info/?username=oncallpeds"
        };
        
        for (String apiUrl : apiUrls) {
            System.out.println("\n📡 Testando: " + apiUrl);
            testApiEndpoint(apiUrl);
        }
    }
    
    private void testApiEndpoint(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Headers realísticos
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.8,en;q=0.6");
            connection.setRequestProperty("Referer", "https://www.instagram.com/oncallpeds/");
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            
            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            
            System.out.println("  Response Code: " + responseCode);
            System.out.println("  Content-Type: " + contentType);
            
            if (responseCode == 200) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null && lineCount < 20) {
                        content.append(line).append("\n");
                        lineCount++;
                    }
                }
                
                String response = content.toString();
                System.out.println("  Tamanho da resposta: " + response.length() + " caracteres");
                
                if (response.length() > 0) {
                    String preview = response.length() > 500 ? response.substring(0, 500) : response;
                    System.out.println("  Prévia:");
                    System.out.println("    " + preview.replace("\n", "\\n"));
                    
                    // Verificar se contém dados de posts
                    boolean hasJson = response.trim().startsWith("{");
                    boolean hasUser = response.contains("\"user\"");
                    boolean hasMedia = response.contains("edge_owner_to_timeline_media") || 
                                     response.contains("\"media\"");
                    boolean hasShortcode = response.contains("shortcode");
                    
                    System.out.println("  ✅ É JSON: " + hasJson);
                    System.out.println("  ✅ Contém user: " + hasUser);
                    System.out.println("  ✅ Contém media: " + hasMedia);
                    System.out.println("  ✅ Contém shortcode: " + hasShortcode);
                    
                    if (hasJson && (hasUser || hasMedia || hasShortcode)) {
                        System.out.println("  🎉 ENDPOINT PROMISSOR!");
                    }
                }
                
            } else {
                System.out.println("  ❌ Erro HTTP: " + responseCode);
                
                // Tentar ler erro
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                    String errorLine;
                    StringBuilder errorContent = new StringBuilder();
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorContent.append(errorLine).append("\n");
                        if (errorContent.length() > 500) break;
                    }
                    if (errorContent.length() > 0) {
                        System.out.println("  Erro: " + errorContent.toString());
                    }
                } catch (Exception e) {
                    // Ignorar erro ao ler erro
                }
            }
            
        } catch (Exception e) {
            System.out.println("  ❌ Exceção: " + e.getMessage());
        }
    }
}