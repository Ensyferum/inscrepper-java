package com.ensyferum.inscrepper.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@ActiveProfiles("test")
public class InstagramJsonAnalysisTest {

    @Test
    public void analyzeInstagramJsonStructure() {
        try {
            System.out.println("🔍 ANÁLISE DETALHADA DO JSON DO INSTAGRAM");
            
            String url = "https://www.instagram.com/oncallpeds/";
            String html = fetchHtml(url);
            
            if (html == null) {
                System.err.println("❌ Não foi possível obter HTML");
                return;
            }
            
            System.out.println("📄 HTML obtido: " + html.length() + " caracteres");
            
            // Procurar todos os scripts com dados JSON
            System.out.println("\n🔍 PROCURANDO SCRIPTS COM JSON...");
            
            Pattern scriptPattern = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);
            Matcher scriptMatcher = scriptPattern.matcher(html);
            
            int scriptCount = 0;
            int jsonScriptCount = 0;
            
            while (scriptMatcher.find()) {
                scriptCount++;
                String scriptContent = scriptMatcher.group(1).trim();
                
                if (scriptContent.length() > 50 && 
                    (scriptContent.contains("{") || scriptContent.contains("window"))) {
                    
                    jsonScriptCount++;
                    System.out.println("\n📜 SCRIPT " + jsonScriptCount + " (tamanho: " + scriptContent.length() + "):");
                    
                    // Mostrar início do script
                    String preview = scriptContent.length() > 200 ? 
                        scriptContent.substring(0, 200) : scriptContent;
                    System.out.println("Início: " + preview.replace("\n", "\\n"));
                    
                    // Verificar padrões específicos
                    boolean hasWindow = scriptContent.contains("window");
                    boolean hasProfileData = scriptContent.toLowerCase().contains("profile");
                    boolean hasUserData = scriptContent.contains("user");
                    boolean hasPostData = scriptContent.contains("edge_owner_to_timeline_media") || 
                                         scriptContent.contains("edge_felix_video_timeline");
                    boolean hasShortcode = scriptContent.contains("shortcode");
                    
                    System.out.println("  - Contém window: " + hasWindow);
                    System.out.println("  - Contém profile: " + hasProfileData);
                    System.out.println("  - Contém user: " + hasUserData);
                    System.out.println("  - Contém posts data: " + hasPostData);
                    System.out.println("  - Contém shortcode: " + hasShortcode);
                    
                    if (hasPostData || hasShortcode) {
                        System.out.println("  ✅ SCRIPT PROMISSOR! Analisando mais...");
                        analyzePromissingScript(scriptContent);
                    }
                    
                    if (jsonScriptCount >= 10) {
                        System.out.println("  ... limitando análise a 10 scripts");
                        break;
                    }
                }
            }
            
            System.out.println("\n📊 RESUMO:");
            System.out.println("Total de scripts: " + scriptCount);
            System.out.println("Scripts com JSON: " + jsonScriptCount);
            
            // Procurar outros padrões no HTML todo
            System.out.println("\n🔍 PROCURANDO OUTROS PADRÕES...");
            
            String[] searchPatterns = {
                "oncallpeds",
                "\"username\"",
                "\"full_name\"",
                "\"biography\"",
                "\"profile_pic_url\"",
                "\"edge_owner_to_timeline_media\"",
                "\"shortcode\":",
                "\"display_url\"",
                "\"thumbnail_src\"",
                "__a=1",
                "graphql"
            };
            
            for (String pattern : searchPatterns) {
                int count = countOccurrences(html, pattern);
                System.out.println("'" + pattern + "': " + count + " ocorrências");
                
                if (count > 0) {
                    int index = html.indexOf(pattern);
                    if (index > 0) {
                        int start = Math.max(0, index - 100);
                        int end = Math.min(html.length(), index + 200);
                        String context = html.substring(start, end);
                        System.out.println("  Contexto: ..." + context.replace("\n", "\\n") + "...");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("💥 Erro na análise: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void analyzePromissingScript(String script) {
        try {
            // Procurar por shortcodes
            Pattern shortcodePattern = Pattern.compile("\"shortcode\"\\s*:\\s*\"([a-zA-Z0-9_-]+)\"");
            Matcher matcher = shortcodePattern.matcher(script);
            
            System.out.println("    🔍 Shortcodes encontrados:");
            int count = 0;
            while (matcher.find() && count < 5) {
                String shortcode = matcher.group(1);
                System.out.println("      - " + shortcode);
                count++;
            }
            
            if (count == 0) {
                System.out.println("      (nenhum shortcode encontrado)");
            }
            
        } catch (Exception e) {
            System.out.println("    ❌ Erro ao analisar script: " + e.getMessage());
        }
    }
    
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        
        return count;
    }
    
    private String fetchHtml(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("HTTP Error: " + responseCode);
                return null;
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            return content.toString();
            
        } catch (Exception e) {
            System.err.println("Erro ao buscar HTML: " + e.getMessage());
            return null;
        }
    }
}