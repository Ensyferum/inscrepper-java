package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.ContentType;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingService {

    private final ContentRepository contentRepository;
    
    private static final String INSTAGRAM_BASE_URL = "https://www.instagram.com/";
    private static final int MAX_POSTS_TO_SCRAPE = 6;

    public List<Content> scrapeProfile(Profile profile) {
        List<Content> scrapedContents = new ArrayList<>();
        WebDriver driver = null;
        
        try {
            driver = createWebDriver();
            String profileUrl = INSTAGRAM_BASE_URL + profile.getUsername() + "/";
            log.info("Acessando perfil: {}", profileUrl);
            
            driver.get(profileUrl);
            
            // Aguardar a página carregar completamente
            new WebDriverWait(driver, Duration.ofSeconds(15));
            
            // Aguardar um pouco para o JavaScript carregar o conteúdo
            Thread.sleep(3000);
            
            // Imprimir HTML da página para análise (apenas uma parte)
            String pageSource = driver.getPageSource();
            log.info("Tamanho da página: {} caracteres", pageSource.length());
            
            // Analisar a estrutura da página para debugging
            analyzePageStructure(driver);
            
            // Tentar diferentes seletores para encontrar posts
            List<WebElement> postElements = findPostElements(driver);
            
            log.info("Encontrados {} posts para o perfil {}", postElements.size(), profile.getUsername());
            
            int postsProcessed = 0;
            for (WebElement postElement : postElements) {
                if (postsProcessed >= MAX_POSTS_TO_SCRAPE) {
                    break;
                }
                
                try {
                    // Extrair informações do post
                    PostInfo postInfo = extractPostInfo(postElement, driver);
                    
                    if (postInfo.getUrl() == null || postInfo.getShortcode() == null) {
                        log.warn("Post sem URL ou shortcode válido, pulando...");
                        continue;
                    }
                    
                    // Verificar se já existe no banco
                    if (contentRepository.existsByExternalId(postInfo.getShortcode())) {
                        log.info("Post {} já existe no banco, pulando...", postInfo.getShortcode());
                        continue;
                    }
                    
                    Content content = Content.builder()
                            .profile(profile)
                            .externalId(postInfo.getShortcode())
                            .url(postInfo.getUrl())
                            .mediaUrl(postInfo.getImageUrl())
                            .caption(postInfo.getAltText() != null ? postInfo.getAltText() : "")
                            .type(ContentType.POST)
                            .collectedAt(Instant.now())
                            .build();
                    
                    // Baixar e salvar a imagem como blob se houver URL da imagem
                    if (postInfo.getImageUrl() != null && !postInfo.getImageUrl().isEmpty()) {
                        downloadAndSaveImage(content, postInfo.getImageUrl());
                    }
                    
                    scrapedContents.add(content);
                    postsProcessed++;
                    
                    log.info("Post {} capturado com sucesso", postInfo.getShortcode());
                    
                } catch (Exception e) {
                    log.error("Erro ao processar post: {}", e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Erro durante o scraping do perfil {}: {}", profile.getUsername(), e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        
        return scrapedContents;
    }
    
    private WebDriver createWebDriver() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // Executar em modo headless para evitar problemas de UI
        options.addArguments("--headless");
        
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        
        // User agent mais recente
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
        
        // Desabilitar imagens para carregar mais rápido (pode ser removido se necessário)
        // options.addArguments("--blink-settings=imagesEnabled=false");
        
        // Configurações adicionais para evitar detecção
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        WebDriver driver = new ChromeDriver(options);
        
        // Executar script para remover propriedades do webdriver
        ((ChromeDriver) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        
        return driver;
    }
    
    private String extractShortcodeFromUrl(String url) {
        // URL format: https://www.instagram.com/p/SHORTCODE/
        String[] parts = url.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("p".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return url.hashCode() + ""; // Fallback
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
                
                log.info("Imagem baixada com sucesso: {} bytes", outputStream.size());
                
            }
        } catch (IOException e) {
            log.error("Erro ao baixar imagem: {}", e.getMessage());
        }
    }
    
    public List<Content> scrapeAndSaveProfile(Profile profile) {
        List<Content> contents = scrapeProfile(profile);
        return contentRepository.saveAll(contents);
    }
    
    private List<WebElement> findPostElements(WebDriver driver) {
        List<WebElement> postElements = new ArrayList<>();
        
        // Lista de seletores para tentar (Instagram muda frequentemente)
        String[] selectors = {
            "article a[href*='/p/']",                    // Seletor genérico
            "div[style*='flex-direction'] a[href*='/p/']", // Posts em grid
            "a[href*='/p/'][role='link']",               // Links com role
            "a[href*='/p/'] img",                        // Links que contêm imagens
            "div[data-testid] a[href*='/p/']",          // Elementos com data-testid
            "[role='main'] a[href*='/p/']",             // Dentro do main content
            "section a[href*='/p/']"                     // Dentro de section
        };
        
        for (String selector : selectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    log.info("Seletor '{}' encontrou {} elementos", selector, elements.size());
                    postElements.addAll(elements);
                }
            } catch (Exception e) {
                log.debug("Seletor '{}' falhou: {}", selector, e.getMessage());
            }
        }
        
        // Remover duplicatas baseado no href
        Map<String, WebElement> uniquePosts = new HashMap<>();
        for (WebElement element : postElements) {
            try {
                String href = element.getAttribute("href");
                if (href != null && href.contains("/p/")) {
                    uniquePosts.put(href, element);
                }
            } catch (Exception e) {
                log.debug("Erro ao processar elemento: {}", e.getMessage());
            }
        }
        
        List<WebElement> result = new ArrayList<>(uniquePosts.values());
        log.info("Total de posts únicos encontrados: {}", result.size());
        
        return result;
    }
    
    private void analyzePageStructure(WebDriver driver) {
        log.info("=== ANÁLISE DA ESTRUTURA DA PÁGINA ===");
        
        try {
            // Analisar elementos principais
            List<WebElement> articles = driver.findElements(By.tagName("article"));
            log.info("Artigos encontrados: {}", articles.size());
            
            List<WebElement> sections = driver.findElements(By.tagName("section"));
            log.info("Seções encontradas: {}", sections.size());
            
            List<WebElement> allLinks = driver.findElements(By.cssSelector("a[href*='/p/']"));
            log.info("Links para posts (/p/): {}", allLinks.size());
            
            // Analisar estrutura de dados
            List<WebElement> dataElements = driver.findElements(By.cssSelector("[data-*]"));
            log.info("Elementos com data-*: {}", dataElements.size());
            
            // Procurar por elementos com roles específicos
            List<WebElement> roleElements = driver.findElements(By.cssSelector("[role]"));
            log.info("Elementos com role: {}", roleElements.size());
            
            // Tentar encontrar o container principal dos posts
            List<WebElement> mainContainers = driver.findElements(By.cssSelector("main, [role='main']"));
            log.info("Containers principais: {}", mainContainers.size());
            
            if (!mainContainers.isEmpty()) {
                WebElement main = mainContainers.get(0);
                List<WebElement> postsInMain = main.findElements(By.cssSelector("a[href*='/p/']"));
                log.info("Posts dentro do container principal: {}", postsInMain.size());
            }
            
        } catch (Exception e) {
            log.error("Erro na análise da estrutura: {}", e.getMessage());
        }
        
        log.info("=== FIM DA ANÁLISE ===");
    }
    
    private PostInfo extractPostInfo(WebElement postElement, WebDriver driver) {
        PostInfo postInfo = new PostInfo();
        
        try {
            // URL do post
            String postUrl = postElement.getAttribute("href");
            postInfo.setUrl(postUrl);
            postInfo.setShortcode(extractShortcodeFromUrl(postUrl));
            
            // Tentar encontrar imagem
            List<WebElement> images = postElement.findElements(By.tagName("img"));
            if (!images.isEmpty()) {
                WebElement img = images.get(0);
                postInfo.setImageUrl(img.getAttribute("src"));
                postInfo.setAltText(img.getAttribute("alt"));
            }
            
            // Tentar extrair mais informações navegando para o post (opcional)
            // Comentado por enquanto para não ser muito invasivo
            
        } catch (Exception e) {
            log.error("Erro ao extrair informações do post: {}", e.getMessage());
        }
        
        return postInfo;
    }
    
    // Classe auxiliar para armazenar informações do post
    private static class PostInfo {
        private String url;
        private String shortcode;
        private String imageUrl;
        private String altText;
        private String caption;
        
        // Getters e Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getShortcode() { return shortcode; }
        public void setShortcode(String shortcode) { this.shortcode = shortcode; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public String getAltText() { return altText; }
        public void setAltText(String altText) { this.altText = altText; }
        
        public String getCaption() { return caption; }
        public void setCaption(String caption) { this.caption = caption; }
    }
}