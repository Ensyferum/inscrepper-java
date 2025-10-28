package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.ContentType;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedInstagramScraper {

    private final ContentRepository contentRepository;
    
    private static final String INSTAGRAM_BASE_URL = "https://www.instagram.com/";
    private static final int MAX_POSTS_TO_SCRAPE = 6;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Pool de User-Agents realísticos
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/121.0"
    };
    
    // Diferentes estratégias de viewport
    private static final String[][] VIEWPORTS = {
        {"1920", "1080"},
        {"1366", "768"},
        {"1440", "900"},
        {"1600", "900"},
        {"1280", "720"}
    };

    public List<Content> scrapeProfile(Profile profile) {
        List<Content> results = new ArrayList<>();
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            log.info("🔄 Tentativa {} de {} para @{}", attempt, MAX_RETRY_ATTEMPTS, profile.getUsername());
            
            WebDriver driver = null;
            try {
                driver = createEnhancedWebDriver(attempt);
                results = performScraping(driver, profile, attempt);
                
                if (!results.isEmpty()) {
                    log.info("✅ Sucesso na tentativa {} - {} posts encontrados", attempt, results.size());
                    break;
                } else {
                    log.warn("⚠️ Tentativa {} falhou - nenhum post encontrado", attempt);
                }
                
            } catch (Exception e) {
                log.error("❌ Erro na tentativa {}: {}", attempt, e.getMessage());
                
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error("💥 Todas as tentativas falharam para @{}", profile.getUsername());
                }
            } finally {
                if (driver != null) {
                    try {
                        driver.quit();
                    } catch (Exception e) {
                        log.debug("Erro ao fechar driver: {}", e.getMessage());
                    }
                }
                
                // Delay entre tentativas
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    randomDelay(3000, 8000);
                }
            }
        }
        
        return results;
    }
    
    private WebDriver createEnhancedWebDriver(int attempt) {
        log.info("🚀 Criando WebDriver enhanced - tentativa {}", attempt);
        
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // User Agent aleatório
        String userAgent = USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
        options.addArguments("--user-agent=" + userAgent);
        log.debug("👤 User Agent: {}", userAgent);
        
        // Viewport aleatório
        String[] viewport = VIEWPORTS[ThreadLocalRandom.current().nextInt(VIEWPORTS.length)];
        options.addArguments("--window-size=" + viewport[0] + "," + viewport[1]);
        log.debug("📱 Viewport: {}x{}", viewport[0], viewport[1]);
        
        // Configurações anti-detecção
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images"); // Acelerar carregamento
        options.addArguments("--disable-javascript-harmony"); 
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        // Headers customizados
        options.addArguments("--disable-features=VizDisplayCompositor");
        
        // Configurações de preferências
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("profile.managed_default_content_settings.images", 2); // Bloquear imagens
        options.setExperimentalOption("prefs", prefs);
        
        // Remover automação detectável
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        
        // Modo headless apenas na segunda tentativa em diante
        if (attempt > 1) {
            options.addArguments("--headless");
            log.debug("🤖 Modo headless ativado");
        }
        
        ChromeDriver driver = new ChromeDriver(options);
        
        // Configurar script para mascarar automação
        driver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        
        // Timeouts otimizados
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        
        return driver;
    }
    
    private List<Content> performScraping(WebDriver driver, Profile profile, int attempt) {
        List<Content> contents = new ArrayList<>();
        
        String profileUrl = INSTAGRAM_BASE_URL + profile.getUsername() + "/";
        log.info("🌐 Acessando: {}", profileUrl);
        
        // Navegar com delay
        driver.get(profileUrl);
        randomDelay(2000, 5000);
        
        // Verificar se a página carregou corretamente
        if (!validatePageLoad(driver, profile)) {
            throw new RuntimeException("Falha ao carregar página do perfil");
        }
        
        // Aceitar cookies se aparecer
        handleCookieConsent(driver);
        
        // Fazer scroll para carregar posts
        performIntelligentScrolling(driver);
        
        // Extrair posts com múltiplas estratégias
        Set<String> postUrls = extractPostUrls(driver, attempt);
        log.info("📊 URLs encontradas: {}", postUrls.size());
        
        // Processar posts encontrados
        int processedCount = 0;
        for (String postUrl : postUrls) {
            if (processedCount >= MAX_POSTS_TO_SCRAPE) {
                break;
            }
            
            try {
                Content content = createContentFromUrl(postUrl, profile);
                if (content != null && !contentRepository.existsByExternalId(content.getExternalId())) {
                    contents.add(content);
                    processedCount++;
                    log.info("✅ Post {}: {}", processedCount, content.getExternalId());
                }
                
                // Small delay entre processamento de posts
                randomDelay(500, 1500);
                
            } catch (Exception e) {
                log.error("❌ Erro ao processar post {}: {}", postUrl, e.getMessage());
            }
        }
        
        return contents;
    }
    
    private boolean validatePageLoad(WebDriver driver, Profile profile) {
        try {
            // Verificar se estamos na página correta
            String currentUrl = driver.getCurrentUrl();
            if (!currentUrl.contains(profile.getUsername())) {
                log.error("❌ URL incorreta: {}", currentUrl);
                return false;
            }
            
            // Verificar se não há erro 404
            String pageSource = driver.getPageSource();
            if (pageSource.contains("Esta página não está disponível") || 
                pageSource.contains("Page not found") ||
                pageSource.contains("User not found")) {
                log.error("❌ Perfil não encontrado");
                return false;
            }
            
            // Verificar se não estamos bloqueados
            if (pageSource.contains("Please wait a few minutes") ||
                pageSource.contains("Try again later") ||
                pageSource.contains("Aguarde alguns minutos")) {
                log.error("❌ Rate limit detectado");
                return false;
            }
            
            // Aguardar elementos essenciais
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                wait.until(d -> d.getPageSource().contains("instagram") || 
                          d.getPageSource().contains("Instagram"));
            } catch (TimeoutException e) {
                log.warn("⚠️ Timeout aguardando elementos do Instagram");
                return false;
            }
            
            log.info("✅ Página carregada corretamente");
            return true;
            
        } catch (Exception e) {
            log.error("❌ Erro na validação da página: {}", e.getMessage());
            return false;
        }
    }
    
    private void handleCookieConsent(WebDriver driver) {
        try {
            // Possíveis seletores para botões de cookies
            String[] cookieSelectors = {
                "button[data-cookiebanner='accept_button']",
                "button:contains('Accept')",
                "button:contains('Aceitar')",
                "[data-testid='cookie-banner'] button",
                ".cookie-banner button"
            };
            
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            
            for (String selector : cookieSelectors) {
                try {
                    WebElement cookieButton = shortWait.until(d -> {
                        try {
                            return d.findElement(By.cssSelector(selector));
                        } catch (org.openqa.selenium.NoSuchElementException e) {
                            return null;
                        }
                    });
                    
                    if (cookieButton != null && cookieButton.isDisplayed()) {
                        cookieButton.click();
                        log.info("🍪 Cookies aceitos");
                        randomDelay(1000, 2000);
                        return;
                    }
                } catch (TimeoutException e) {
                    // Continuar tentando outros seletores
                }
            }
            
        } catch (Exception e) {
            log.debug("Nenhum banner de cookies encontrado");
        }
    }
    
    private void performIntelligentScrolling(WebDriver driver) {
        log.info("📜 Iniciando scroll inteligente");
        
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        // Scroll inicial
        js.executeScript("window.scrollTo(0, 500);");
        randomDelay(1000, 2000);
        
        // Múltiplos scrolls com delays
        for (int i = 0; i < 3; i++) {
            long scrollHeight = (Long) js.executeScript("return document.body.scrollHeight");
            long currentScroll = (Long) js.executeScript("return window.pageYOffset");
            
            // Scroll parcial
            long newScroll = currentScroll + (scrollHeight / 4);
            js.executeScript("window.scrollTo(0, " + newScroll + ");");
            
            randomDelay(2000, 4000);
            
            // Verificar se carregou novo conteúdo
            long newScrollHeight = (Long) js.executeScript("return document.body.scrollHeight");
            if (newScrollHeight > scrollHeight) {
                log.debug("📈 Novo conteúdo carregado");
            }
        }
        
        // Scroll de volta para o topo
        js.executeScript("window.scrollTo(0, 0);");
        randomDelay(1000, 2000);
    }
    
    private Set<String> extractPostUrls(WebDriver driver, int attempt) {
        Set<String> urls = new HashSet<>();
        
        // Estratégia 1: CSS Selectors
        urls.addAll(extractWithCssSelectors(driver));
        
        // Estratégia 2: JavaScript execution
        if (urls.isEmpty() || attempt > 1) {
            urls.addAll(extractWithJavaScript(driver));
        }
        
        // Estratégia 3: Regex no PageSource
        if (urls.isEmpty() || attempt > 2) {
            urls.addAll(extractWithRegex(driver));
        }
        
        return urls;
    }
    
    private Set<String> extractWithCssSelectors(WebDriver driver) {
        Set<String> urls = new HashSet<>();
        
        String[] selectors = {
            "a[href*='/p/']",
            "a[href*='/reel/']", 
            "[data-testid='post'] a",
            "article a[href*='/p/']",
            "div[style*='post'] a"
        };
        
        for (String selector : selectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                
                for (WebElement element : elements) {
                    String href = element.getAttribute("href");
                    if (href != null && (href.contains("/p/") || href.contains("/reel/"))) {
                        urls.add(normalizeUrl(href));
                        if (urls.size() >= MAX_POSTS_TO_SCRAPE * 2) {
                            break;
                        }
                    }
                }
                
            } catch (Exception e) {
                log.debug("Erro com seletor {}: {}", selector, e.getMessage());
            }
        }
        
        log.info("🎯 CSS Selectors encontraram {} URLs", urls.size());
        return urls;
    }
    
    private Set<String> extractWithJavaScript(WebDriver driver) {
        Set<String> urls = new HashSet<>();
        
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            String script = """
                var links = [];
                var anchors = document.querySelectorAll('a[href*="/p/"], a[href*="/reel/"]');
                for (var i = 0; i < anchors.length && i < 20; i++) {
                    if (anchors[i].href) {
                        links.push(anchors[i].href);
                    }
                }
                return links;
                """;
                
            @SuppressWarnings("unchecked")
            List<String> jsResults = (List<String>) js.executeScript(script);
            
            if (jsResults != null) {
                for (String url : jsResults) {
                    urls.add(normalizeUrl(url));
                }
            }
            
        } catch (Exception e) {
            log.debug("Erro na extração JavaScript: {}", e.getMessage());
        }
        
        log.info("🚀 JavaScript encontrou {} URLs", urls.size());
        return urls;
    }
    
    private Set<String> extractWithRegex(WebDriver driver) {
        Set<String> urls = new HashSet<>();
        
        try {
            String pageSource = driver.getPageSource();
            
            String[] patterns = {
                "https://www\\.instagram\\.com/p/([a-zA-Z0-9_-]+)/",
                "https://www\\.instagram\\.com/reel/([a-zA-Z0-9_-]+)/",
                "\"/p/([a-zA-Z0-9_-]+)/\"",
                "\"/reel/([a-zA-Z0-9_-]+)/\""
            };
            
            for (String patternStr : patterns) {
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(pageSource);
                
                while (matcher.find() && urls.size() < MAX_POSTS_TO_SCRAPE * 2) {
                    String fullMatch = matcher.group(0);
                    
                    if (!fullMatch.startsWith("http")) {
                        fullMatch = "https://www.instagram.com" + fullMatch.replace("\"", "");
                    }
                    
                    urls.add(normalizeUrl(fullMatch));
                }
            }
            
        } catch (Exception e) {
            log.debug("Erro na extração Regex: {}", e.getMessage());
        }
        
        log.info("🔍 Regex encontrou {} URLs", urls.size());
        return urls;
    }
    
    private String normalizeUrl(String url) {
        if (url == null) return null;
        
        // Remover parâmetros de query
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        
        // Garantir que termina sem barra
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        return url;
    }
    
    private Content createContentFromUrl(String postUrl, Profile profile) {
        try {
            String shortcode = extractShortcode(postUrl);
            
            ContentType type = postUrl.contains("/reel/") ? ContentType.REEL : ContentType.POST;
            
            return Content.builder()
                    .profile(profile)
                    .externalId(shortcode)
                    .url(postUrl)
                    .caption("Coletado via Enhanced Selenium Scraper")
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
        
        return String.valueOf(Math.abs(url.hashCode()));
    }
    
    private void randomDelay(int minMs, int maxMs) {
        try {
            int delay = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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