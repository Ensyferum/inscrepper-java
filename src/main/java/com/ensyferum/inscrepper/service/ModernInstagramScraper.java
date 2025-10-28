package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.ContentType;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
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
public class ModernInstagramScraper {

    private final ContentRepository contentRepository;
    
    private static final String INSTAGRAM_BASE_URL = "https://www.instagram.com/";
    private static final int MAX_POSTS_TO_SCRAPE = 6;
    private static final int MAX_SCROLL_ATTEMPTS = 3;

    public List<Content> scrapeProfile(Profile profile) {
        List<Content> scrapedContents = new ArrayList<>();
        WebDriver driver = null;
        
        try {
            driver = createOptimizedWebDriver();
            String profileUrl = INSTAGRAM_BASE_URL + profile.getUsername() + "/";
            
            log.info("🔍 Iniciando scraping do perfil: @{}", profile.getUsername());
            log.info("🌐 Acessando URL: {}", profileUrl);
            
            driver.get(profileUrl);
            
            // Aguardar página carregar
            Thread.sleep(4000);
            
            // Verificar se página carregou corretamente
            String title = driver.getTitle();
            log.info("📄 Título da página: {}", title);
            
            if (title.contains("Page Not Found") || title.contains("Página não encontrada")) {
                log.error("❌ Perfil @{} não encontrado", profile.getUsername());
                return scrapedContents;
            }
            
            // Tentar diferentes estratégias para encontrar posts
            Set<String> foundUrls = new HashSet<>();
            
            // Estratégia 1: JavaScript direto
            foundUrls.addAll(findPostsWithJavaScript(driver));
            
            // Estratégia 2: Seletores CSS após scroll
            foundUrls.addAll(findPostsWithScrollAndSelectors(driver));
            
            // Estratégia 3: Análise do HTML por regex
            foundUrls.addAll(findPostsWithRegex(driver));
            
            log.info("📊 Total de URLs únicas encontradas: {}", foundUrls.size());
            
            // Processar as URLs encontradas
            int processedCount = 0;
            for (String postUrl : foundUrls) {
                if (processedCount >= MAX_POSTS_TO_SCRAPE) {
                    break;
                }
                
                try {
                    Content content = processPostUrl(postUrl, profile, driver);
                    if (content != null) {
                        scrapedContents.add(content);
                        processedCount++;
                        log.info("✅ Post {} processado: {}", processedCount, extractShortcode(postUrl));
                    }
                } catch (Exception e) {
                    log.error("❌ Erro ao processar post {}: {}", postUrl, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("💥 Erro durante o scraping: {}", e.getMessage(), e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.debug("Erro ao fechar driver: {}", e.getMessage());
                }
            }
        }
        
        log.info("🎯 Scraping concluído: {} posts capturados para @{}", scrapedContents.size(), profile.getUsername());
        return scrapedContents;
    }
    
    private WebDriver createOptimizedWebDriver() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // Configurações para melhor compatibilidade
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1366,768");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        // User agent realista
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        // Evitar detecção de automação
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        WebDriver driver = new ChromeDriver(options);
        
        // Script para mascarar webdriver
        ((JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        
        return driver;
    }
    
    private Set<String> findPostsWithJavaScript(WebDriver driver) {
        Set<String> urls = new HashSet<>();
        
        try {
            log.info("🔧 Tentando encontrar posts via JavaScript...");
            
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Script para encontrar todos os links de posts
            String script = """
                var links = [];
                var anchors = document.querySelectorAll('a[href*="/p/"], a[href*="/reel/"]');
                for (var i = 0; i < anchors.length; i++) {
                    var href = anchors[i].href;
                    if (href && (href.includes('/p/') || href.includes('/reel/'))) {
                        links.push(href);
                    }
                }
                return links;
            """;
            
            @SuppressWarnings("unchecked")
            List<String> jsUrls = (List<String>) js.executeScript(script);
            
            if (jsUrls != null) {
                urls.addAll(jsUrls);
                log.info("📱 JavaScript encontrou {} URLs", jsUrls.size());
            }
            
        } catch (Exception e) {
            log.debug("Erro no JavaScript: {}", e.getMessage());
        }
        
        return urls;
    }
    
    private Set<String> findPostsWithScrollAndSelectors(WebDriver driver) {
        Set<String> urls = new HashSet<>();
        
        try {
            log.info("📜 Tentando scroll e seletores CSS...");
            
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Tentar scroll para carregar mais conteúdo
            for (int i = 0; i < MAX_SCROLL_ATTEMPTS; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000);
                
                // Tentar diferentes seletores
                String[] selectors = {
                    "a[href*='/p/']",
                    "a[href*='/reel/']",
                    "[href*='/p/']",
                    "[href*='/reel/']",
                    "div a[href*='/p/']",
                    "article a[href*='/p/']"
                };
                
                for (String selector : selectors) {
                    try {
                        List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                        for (WebElement element : elements) {
                            String href = element.getAttribute("href");
                            if (href != null && (href.contains("/p/") || href.contains("/reel/"))) {
                                urls.add(href);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Seletor {} falhou: {}", selector, e.getMessage());
                    }
                }
            }
            
            log.info("🎯 Scroll e seletores encontraram {} URLs", urls.size());
            
        } catch (Exception e) {
            log.debug("Erro no scroll: {}", e.getMessage());
        }
        
        return urls;
    }
    
    private Set<String> findPostsWithRegex(WebDriver driver) {
        Set<String> urls = new HashSet<>();
        
        try {
            log.info("🔍 Analisando HTML com regex...");
            
            String pageSource = driver.getPageSource();
            
            // Pattern para encontrar URLs de posts no HTML
            Pattern pattern = Pattern.compile("https://www\\.instagram\\.com/(?:p|reel)/([a-zA-Z0-9_-]+)/?");
            Matcher matcher = pattern.matcher(pageSource);
            
            while (matcher.find()) {
                urls.add(matcher.group());
            }
            
            log.info("🧩 Regex encontrou {} URLs", urls.size());
            
        } catch (Exception e) {
            log.debug("Erro no regex: {}", e.getMessage());
        }
        
        return urls;
    }
    
    private Content processPostUrl(String postUrl, Profile profile, WebDriver driver) {
        try {
            String shortcode = extractShortcode(postUrl);
            
            // Verificar se já existe
            if (contentRepository.existsByExternalId(shortcode)) {
                log.debug("Post {} já existe no banco", shortcode);
                return null;
            }
            
            // Tentar extrair informações da imagem
            String imageUrl = findImageUrl(postUrl, driver);
            
            Content content = Content.builder()
                    .profile(profile)
                    .externalId(shortcode)
                    .url(postUrl)
                    .mediaUrl(imageUrl)
                    .caption("") // Por enquanto vazio, pode ser melhorado
                    .type(postUrl.contains("/reel/") ? ContentType.REEL : ContentType.POST)
                    .collectedAt(Instant.now())
                    .build();
            
            // Tentar baixar imagem se encontrada
            if (imageUrl != null && !imageUrl.isEmpty()) {
                downloadAndSaveImage(content, imageUrl);
            }
            
            return content;
            
        } catch (Exception e) {
            log.error("Erro ao processar URL {}: {}", postUrl, e.getMessage());
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
            log.debug("Erro ao extrair shortcode de {}: {}", url, e.getMessage());
        }
        
        // Fallback
        return String.valueOf(url.hashCode());
    }
    
    private String findImageUrl(String postUrl, WebDriver driver) {
        // Por enquanto, retornar null - pode ser implementado navegando para o post
        // ou tentando encontrar a imagem na página atual
        return null;
    }
    
    private void downloadAndSaveImage(Content content, String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                content.setImageBlob(outputStream.toByteArray());
                content.setImageMimeType(connection.getContentType());
                
                log.debug("Imagem baixada: {} bytes", outputStream.size());
                
            }
        } catch (IOException e) {
            log.debug("Erro ao baixar imagem: {}", e.getMessage());
        }
    }
    
    public List<Content> scrapeAndSaveProfile(Profile profile) {
        List<Content> contents = scrapeProfile(profile);
        if (!contents.isEmpty()) {
            return contentRepository.saveAll(contents);
        }
        return contents;
    }
}