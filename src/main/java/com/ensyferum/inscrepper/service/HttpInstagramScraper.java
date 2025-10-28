package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.ContentType;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpInstagramScraper {

    private final ContentRepository contentRepository;
    
    private static final String INSTAGRAM_BASE_URL = "https://www.instagram.com/";
    private static final int MAX_POSTS_TO_SCRAPE = 6;

    public List<Content> scrapeProfile(Profile profile) {
        List<Content> scrapedContents = new ArrayList<>();
        
        try {
            String profileUrl = INSTAGRAM_BASE_URL + profile.getUsername() + "/";
            log.info("🔍 Scraping via HTTP: @{}", profile.getUsername());
            log.info("🌐 URL: {}", profileUrl);
            
            // Fazer requisição HTTP simples
            String html = fetchHtmlContent(profileUrl);
            
            if (html == null || html.isEmpty()) {
                log.error("❌ Não foi possível obter HTML do perfil");
                return scrapedContents;
            }
            
            log.info("📄 HTML obtido: {} caracteres", html.length());
            
            // Analisar HTML para encontrar posts
            Set<String> postUrls = extractPostUrls(html);
            log.info("📊 URLs encontradas: {}", postUrls.size());
            
            // Processar URLs encontradas
            int processedCount = 0;
            for (String postUrl : postUrls) {
                if (processedCount >= MAX_POSTS_TO_SCRAPE) {
                    break;
                }
                
                try {
                    Content content = createContentFromUrl(postUrl, profile);
                    if (content != null) {
                        scrapedContents.add(content);
                        processedCount++;
                        log.info("✅ Post {}: {}", processedCount, content.getExternalId());
                    }
                } catch (Exception e) {
                    log.error("❌ Erro ao processar post {}: {}", postUrl, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("💥 Erro durante scraping HTTP: {}", e.getMessage());
        }
        
        log.info("🎯 Scraping HTTP concluído: {} posts para @{}", scrapedContents.size(), profile.getUsername());
        return scrapedContents;
    }
    
    private String fetchHtmlContent(String url) {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            
            // Configurar headers para parecer um navegador real
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", 
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            connection.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.8,en;q=0.6");
            // Remover Accept-Encoding para evitar compressão
            // connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            
            int responseCode = connection.getResponseCode();
            log.info("📡 Response Code: {}", responseCode);
            log.info("📡 Content-Type: {}", connection.getContentType());
            log.info("📡 Content-Encoding: {}", connection.getContentEncoding());
            
            if (responseCode == 200) {
                StringBuilder content = new StringBuilder();
                
                // Verificar se o conteúdo está comprimido
                String encoding = connection.getContentEncoding();
                java.io.InputStream inputStream = connection.getInputStream();
                
                if ("gzip".equalsIgnoreCase(encoding)) {
                    inputStream = new java.util.zip.GZIPInputStream(inputStream);
                    log.info("🗜️ Decomprimindo GZIP");
                }
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                return content.toString();
            } else {
                log.error("❌ HTTP Error: {} {}", responseCode, connection.getResponseMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("💥 Erro ao fazer requisição HTTP: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private Set<String> extractPostUrls(String html) {
        Set<String> urls = new HashSet<>();
        
        try {
            // Padrões regex para encontrar URLs de posts
            String[] patterns = {
                "https://www\\.instagram\\.com/p/([a-zA-Z0-9_-]+)/",
                "https://www\\.instagram\\.com/reel/([a-zA-Z0-9_-]+)/",
                "/p/([a-zA-Z0-9_-]+)/",
                "/reel/([a-zA-Z0-9_-]+)/"
            };
            
            for (String patternStr : patterns) {
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(html);
                
                while (matcher.find()) {
                    String fullMatch = matcher.group(0);
                    
                    // Normalizar URL
                    if (!fullMatch.startsWith("http")) {
                        fullMatch = "https://www.instagram.com" + fullMatch;
                    }
                    
                    // Remover trailing slash se houver
                    if (fullMatch.endsWith("/")) {
                        fullMatch = fullMatch.substring(0, fullMatch.length() - 1);
                    }
                    
                    urls.add(fullMatch);
                    
                    if (urls.size() >= MAX_POSTS_TO_SCRAPE * 2) {
                        break; // Limitar para não processar muitos
                    }
                }
            }
            
            // Tentar encontrar JSON com dados do Instagram - vários padrões
            String[] jsonPatterns = {
                "window\\._sharedData\\s*=\\s*(\\{.*?\\});",
                "window\\.__additionalDataLoaded\\([^,]+,(\\{.*?\\})\\);",
                "\"ProfilePage\"\\s*:\\s*(\\{.*?\\})\\s*\\}\\s*\\}",
                "\"user\"\\s*:\\s*(\\{.*?\"edge_owner_to_timeline_media\".*?\\})"
            };
            
            for (String patternStr : jsonPatterns) {
                Pattern jsonPattern = Pattern.compile(patternStr, Pattern.DOTALL);
                Matcher jsonMatcher = jsonPattern.matcher(html);
                
                if (jsonMatcher.find()) {
                    log.info("📱 Dados JSON do Instagram encontrados! Padrão: {}", patternStr.substring(0, 20));
                    String jsonData = jsonMatcher.group(1);
                    
                    // Extrair shortcodes do JSON com vários padrões
                    String[] shortcodePatterns = {
                        "\"shortcode\"\\s*:\\s*\"([a-zA-Z0-9_-]+)\"",
                        "\"code\"\\s*:\\s*\"([a-zA-Z0-9_-]+)\"",
                        "/p/([a-zA-Z0-9_-]+)/",
                        "/reel/([a-zA-Z0-9_-]+)/"
                    };
                    
                    for (String scPattern : shortcodePatterns) {
                        Pattern urlInJsonPattern = Pattern.compile(scPattern);
                        Matcher urlMatcher = urlInJsonPattern.matcher(jsonData);
                        
                        while (urlMatcher.find()) {
                            String shortcode = urlMatcher.group(1);
                            String postUrl = "https://www.instagram.com/p/" + shortcode;
                            urls.add(postUrl);
                            
                            if (urls.size() >= MAX_POSTS_TO_SCRAPE * 3) {
                                break;
                            }
                        }
                    }
                    
                    // Se encontrou dados, parar de procurar outros padrões
                    if (urls.size() > 0) {
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao extrair URLs: {}", e.getMessage());
        }
        
        return urls;
    }
    
    private Content createContentFromUrl(String postUrl, Profile profile) {
        try {
            String shortcode = extractShortcode(postUrl);
            
            // Verificar se já existe
            if (contentRepository.existsByExternalId(shortcode)) {
                log.debug("Post {} já existe", shortcode);
                return null;
            }
            
            ContentType type = postUrl.contains("/reel/") ? ContentType.REEL : ContentType.POST;
            
            return Content.builder()
                    .profile(profile)
                    .externalId(shortcode)
                    .url(postUrl)
                    .mediaUrl(null) // Por enquanto null, pode ser melhorado
                    .caption("Capturado via HTTP scraping")
                    .type(type)
                    .collectedAt(Instant.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Erro ao criar content: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractShortcode(String url) {
        try {
            Pattern pattern = Pattern.compile("/(?:p|reel)/([a-zA-Z0-9_-]+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair shortcode: {}", e.getMessage());
        }
        
        return String.valueOf(url.hashCode());
    }
    
    public List<Content> scrapeAndSaveProfile(Profile profile) {
        List<Content> contents = scrapeProfile(profile);
        if (!contents.isEmpty()) {
            return contentRepository.saveAll(contents);
        }
        return contents;
    }
    
    // Método para análise de debug
    public void analyzeProfileHtml(Profile profile) {
        try {
            String profileUrl = INSTAGRAM_BASE_URL + profile.getUsername() + "/";
            String html = fetchHtmlContent(profileUrl);
            
            if (html != null) {
                System.out.println("=== ANÁLISE HTML DO PERFIL @" + profile.getUsername() + " ===");
                System.out.println("Tamanho do HTML: " + html.length() + " caracteres");
                
                // Verificar se há indicadores do Instagram
                boolean hasInstagram = html.toLowerCase().contains("instagram");
                boolean hasReact = html.contains("React") || html.contains("react");
                boolean hasJson = html.contains("window._sharedData");
                boolean hasProfile = html.contains(profile.getUsername());
                boolean hasAdditionalData = html.contains("window.__additionalDataLoaded");
                boolean hasProfilePage = html.contains("ProfilePage");
                
                System.out.println("Contém 'Instagram': " + hasInstagram);
                System.out.println("Contém React: " + hasReact);
                System.out.println("Contém JSON _sharedData: " + hasJson);
                System.out.println("Contém __additionalDataLoaded: " + hasAdditionalData);
                System.out.println("Contém ProfilePage: " + hasProfilePage);
                System.out.println("Contém username: " + hasProfile);
                
                // Procurar por padrões específicos de posts
                int countPostPattern = countMatches(html, "/p/[a-zA-Z0-9_-]+/");
                int countReelPattern = countMatches(html, "/reel/[a-zA-Z0-9_-]+/");
                int countShortcode = countMatches(html, "\"shortcode\"");
                
                System.out.println("\nContadores de padrões:");
                System.out.println("  Posts (/p/): " + countPostPattern);
                System.out.println("  Reels (/reel/): " + countReelPattern);
                System.out.println("  Shortcodes JSON: " + countShortcode);
                
                // Mostrar início do HTML
                String preview = html.length() > 1000 ? html.substring(0, 1000) : html;
                System.out.println("\nPrévia do HTML:");
                System.out.println(preview);
                System.out.println("...");
                
                // Procurar por trechos específicos do Instagram
                if (hasAdditionalData) {
                    int startIdx = html.indexOf("window.__additionalDataLoaded");
                    if (startIdx > 0) {
                        int endIdx = Math.min(startIdx + 500, html.length());
                        System.out.println("\nTrecho __additionalDataLoaded:");
                        System.out.println(html.substring(startIdx, endIdx));
                        System.out.println("...");
                    }
                }
                
                // Encontrar posts
                Set<String> urls = extractPostUrls(html);
                System.out.println("\nPosts encontrados: " + urls.size());
                int count = 0;
                for (String url : urls) {
                    if (count++ < 10) { // Limitar para não poluir
                        System.out.println("  - " + url);
                    }
                }
                if (urls.size() > 10) {
                    System.out.println("  ... e mais " + (urls.size() - 10) + " posts");
                }
                
                System.out.println("=== FIM DA ANÁLISE ===");
            }
        } catch (Exception e) {
            System.err.println("Erro na análise: " + e.getMessage());
        }
    }
    
    private int countMatches(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            int count = 0;
            while (m.find()) {
                count++;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}