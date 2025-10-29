package com.ensyferum.inscrepper.service;

import com.ensyferum.inscrepper.config.CookieManager;
import com.ensyferum.inscrepper.config.ProxyRotationManager;
import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.ContentType;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.model.ScrapingExecution;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.repository.ScrapingExecutionRepository;
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
    private final ScrapingExecutionRepository executionRepository;
    private final ProxyRotationManager proxyManager;
    private final CookieManager cookieManager;
    
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
        return scrapeProfile(profile, false);
    }
    
    public List<Content> scrapeProfile(Profile profile, boolean forceUpdate) {
        // Criar registro de execução
        ScrapingExecution execution = ScrapingExecution.builder()
                .profile(profile)
                .startedAt(Instant.now())
                .status(ScrapingExecution.ExecutionStatus.STARTED)
                .forceUpdate(forceUpdate)
                .build();
        execution = executionRepository.save(execution);
        
        List<Content> results = new ArrayList<>();
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            log.info("🔄 Tentativa {} de {} para @{} (execução: {})", 
                    attempt, MAX_RETRY_ATTEMPTS, profile.getUsername(), execution.getId());
            
            // Atualizar tentativa na execução
            execution.setAttemptNumber(attempt);
            executionRepository.save(execution);
            
            WebDriver driver = null;
            try {
                driver = createEnhancedWebDriver(attempt);
                results = performScraping(driver, profile, attempt, forceUpdate, execution);
                
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
        
        // Finalizar execução
        finalizeScraping(execution, results);
        
        // Limpeza periódica de cookies antigos (apenas ocasionalmente)
        if (ThreadLocalRandom.current().nextInt(10) == 0) { // 10% de chance
            cookieManager.cleanupOldCookies();
        }
        
        return results;
    }
    
    private void finalizeScraping(ScrapingExecution execution, List<Content> results) {
        try {
            execution.setFinishedAt(Instant.now());
            execution.setPostsFound(results.size() + (execution.getPostsSkipped() != null ? execution.getPostsSkipped() : 0));
            execution.setPostsProcessed(results.size());
            
            // Determinar status final
            if (results.isEmpty()) {
                if (execution.getPostsSkipped() != null && execution.getPostsSkipped() > 0) {
                    execution.setStatus(ScrapingExecution.ExecutionStatus.PARTIAL_SUCCESS);
                } else {
                    execution.setStatus(ScrapingExecution.ExecutionStatus.FAILED);
                    execution.setErrorMessage("Nenhum post foi processado");
                }
            } else if (results.size() >= MAX_POSTS_TO_SCRAPE / 2) {
                execution.setStatus(ScrapingExecution.ExecutionStatus.SUCCESS);
            } else {
                execution.setStatus(ScrapingExecution.ExecutionStatus.PARTIAL_SUCCESS);
            }
            
            executionRepository.save(execution);
            
            log.info("📊 Execução finalizada: {}", execution.getSummary());
            
        } catch (Exception e) {
            log.error("❌ Erro ao finalizar execução: {}", e.getMessage());
        }
    }
    
    /**
     * Detecta e tenta lidar com captchas
     */
    private boolean detectAndHandleCaptcha(WebDriver driver) {
        try {
            log.debug("🔍 Verificando presença de captcha...");
            
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            String pageSource = driver.getPageSource().toLowerCase();
            String currentUrl = driver.getCurrentUrl().toLowerCase();
            
            // Detectar diferentes tipos de captcha/challenge
            boolean hasCaptcha = pageSource.contains("captcha") || 
                               pageSource.contains("challenge") ||
                               pageSource.contains("security check") ||
                               pageSource.contains("verificação") ||
                               currentUrl.contains("challenge") ||
                               currentUrl.contains("captcha");
            
            if (!hasCaptcha) {
                // Verificar elementos visuais de captcha
                String[] captchaSelectors = {
                    "[data-testid*='captcha']",
                    ".captcha",
                    "#captcha",
                    "[aria-label*='captcha']",
                    "[aria-label*='security']",
                    "iframe[src*='captcha']",
                    "iframe[src*='recaptcha']"
                };
                
                for (String selector : captchaSelectors) {
                    try {
                        WebElement captchaElement = shortWait.until(webDriver -> {
                            try {
                                WebElement el = webDriver.findElement(By.cssSelector(selector));
                                return el.isDisplayed() ? el : null;
                            } catch (Exception e) {
                                return null;
                            }
                        });
                        
                        if (captchaElement != null) {
                            hasCaptcha = true;
                            log.debug("✅ Captcha detectado via seletor: {}", selector);
                            break;
                        }
                    } catch (TimeoutException e) {
                        // Seletor não encontrado, continuar
                    }
                }
            }
            
            if (hasCaptcha) {
                log.warn("🤖 CAPTCHA/Challenge detectado!");
                return handleCaptchaChallenge(driver);
            }
            
            return false;
            
        } catch (Exception e) {
            log.debug("Erro na detecção de captcha: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Tenta lidar com desafios de captcha
     */
    private boolean handleCaptchaChallenge(WebDriver driver) {
        try {
            log.info("🔧 Tentando lidar com captcha/challenge...");
            
            // Estratégia 1: Aguardar e tentar continuar
            humanDelay(5000, 10000);
            
            // Estratégia 2: Procurar botões para pular ou continuar
            String[] skipSelectors = {
                "button:contains('Skip')",
                "button:contains('Pular')", 
                "button:contains('Continue')",
                "button:contains('Continuar')",
                "button[data-testid*='skip']",
                "button[data-testid*='continue']",
                "[role='button']:contains('Not now')",
                "[role='button']:contains('Agora não')"
            };
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            for (String selector : skipSelectors) {
                try {
                    WebElement skipButton;
                    if (selector.contains(":contains(")) {
                        String text = selector.replaceAll(".*:contains\\('([^']+)'\\).*", "$1");
                        skipButton = wait.until(webDriver -> {
                            try {
                                return webDriver.findElement(By.xpath("//button[contains(text(), '" + text + "')] | //*[@role='button'][contains(text(), '" + text + "')]"));
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    } else {
                        skipButton = wait.until(webDriver -> {
                            try {
                                WebElement el = webDriver.findElement(By.cssSelector(selector));
                                return el.isDisplayed() ? el : null;
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    }
                    
                    if (skipButton != null) {
                        log.info("✅ Encontrado botão para pular captcha: {}", selector);
                        humanClick(skipButton, driver);
                        humanDelay(2000, 4000);
                        return true;
                    }
                    
                } catch (TimeoutException e) {
                    // Botão não encontrado, tentar próximo
                }
            }
            
            // Estratégia 3: Aguardar mais tempo e esperar o captcha resolver automaticamente
            log.info("⏳ Aguardando resolução automática do captcha...");
            humanDelay(15000, 25000);
            
            // Verificar se ainda está na página de captcha
            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            
            boolean stillInChallenge = currentUrl.contains("challenge") || 
                                     currentUrl.contains("captcha") ||
                                     pageSource.contains("captcha") ||
                                     pageSource.contains("challenge");
            
            if (!stillInChallenge) {
                log.info("✅ Captcha resolvido automaticamente");
                return true;
            }
            
            log.warn("⚠️ Captcha ainda presente após tentativas");
            return false;
            
        } catch (Exception e) {
            log.error("❌ Erro ao lidar com captcha: {}", e.getMessage());
            return false;
        }
    }
    
    private WebDriver createEnhancedWebDriver(int attempt) {
        log.info("🚀 Criando WebDriver enhanced - tentativa {}", attempt);
        
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // Configurar proxy rotativo se disponível (temporariamente desabilitado para testes)
        ProxyRotationManager.ProxyConfig proxyConfig = null;
        if (false && proxyManager.hasProxies()) { // Desabilitado temporariamente
            proxyConfig = proxyManager.getRandomProxy();
            setupProxy(options, proxyConfig);
            log.info("🌐 Proxy configurado: {}", proxyConfig.toString());
        } else {
            log.info("🔄 Executando sem proxy (modo de teste)");
        }
        
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
        
        // SEMPRE executar em modo headless para não interferir no teste
        options.addArguments("--headless=new");
        log.info("👻 Executando em modo headless (background)");
        
        ChromeDriver driver = new ChromeDriver(options);
        
        // Configurar script para mascarar automação
        driver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        
        // Timeouts otimizados
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        
        return driver;
    }
    
    /**
     * Configura proxy no Chrome Options
     */
    private void setupProxy(ChromeOptions options, ProxyRotationManager.ProxyConfig proxyConfig) {
        if (proxyConfig == null) return;
        
        try {
            log.info("🌐 Configurando proxy: {}", proxyConfig.toString());
            
            // Configurar proxy
            org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
            proxy.setHttpProxy(proxyConfig.toChromeProxyString());
            proxy.setSslProxy(proxyConfig.toChromeProxyString());
            
            options.setProxy(proxy);
            
            // Se o proxy tem autenticação, configurar via Chrome args
            if (proxyConfig.hasAuth()) {
                log.debug("🔐 Proxy com autenticação configurado");
                
                // Nota: Para autenticação de proxy mais robusta, seria necessário
                // usar extensões Chrome ou outras técnicas mais avançadas
                options.addArguments("--proxy-auth=" + proxyConfig.getUsername() + ":" + proxyConfig.getPassword());
            }
            
            // Configurações adicionais para proxy
            options.addArguments("--proxy-bypass-list=<-loopback>");
            options.addArguments("--proxy-server=" + proxyConfig.toChromeProxyString());
            
            log.info("✅ Proxy configurado com sucesso: {}", proxyConfig.toString());
            
        } catch (Exception e) {
            log.warn("⚠️ Erro ao configurar proxy {}: {}", proxyConfig.toString(), e.getMessage());
            // Continuar sem proxy em caso de erro
        }
    }
    
    private List<Content> performScraping(WebDriver driver, Profile profile, int attempt, boolean forceUpdate, ScrapingExecution execution) {
        List<Content> contents = new ArrayList<>();
        
        String profileUrl = INSTAGRAM_BASE_URL + profile.getUsername() + "/";
        log.info("🌐 Acessando: {}", profileUrl);
        
        // Tentar carregar cookies salvos antes de navegar
        String sessionId = cookieManager.getSessionId(profile.getUsername());
        boolean cookiesLoaded = false;
        
        // Primeiro, navegar para Instagram para estabelecer contexto
        driver.get("https://www.instagram.com/");
        humanDelay(1000, 2000);
        
        // Tentar carregar cookies existentes
        if (cookieManager.hasCookies(sessionId)) {
            log.info("🍪 Tentando carregar cookies salvos para {}", profile.getUsername());
            cookiesLoaded = cookieManager.loadCookies(driver, sessionId);
        }
        
        // Navegar para o perfil
        log.info("🌐 Navegando para perfil: {}", profileUrl);
        driver.get(profileUrl);
        humanDelay(2000, 4000);
        
        // Verificar se a página carregou corretamente
        if (!validatePageLoad(driver, profile)) {
            throw new RuntimeException("Falha ao carregar página do perfil");
        }
        
        // Se carregamos cookies com sucesso e não precisamos de login, pular validação de login
        if (cookiesLoaded) {
            log.info("✅ Cookies carregados, verificando se login ainda é necessário...");
            
            // Verificar se ainda estamos logados
            String currentPageSource = driver.getPageSource();
            boolean stillNeedsLogin = needsLogin(driver.getCurrentUrl(), currentPageSource);
            
            if (!stillNeedsLogin) {
                log.info("🎉 Login automático via cookies bem-sucedido!");
            } else {
                log.warn("⚠️ Cookies não foram suficientes, login manual necessário");
                // Os cookies não funcionaram, limpar e tentar login normal
                cookieManager.clearCookies(sessionId);
            }
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
                Content content = createContentFromUrl(postUrl, profile, driver, forceUpdate, execution);
                if (content != null && !contentRepository.existsByExternalId(content.getExternalId())) {
                    contents.add(content);
                    processedCount++;
                    
                    String captionPreview = content.getCaption();
                    if (captionPreview != null && captionPreview.length() > 50) {
                        captionPreview = captionPreview.substring(0, 50) + "...";
                    }
                    
                    log.info("✅ Post {}: {} - Caption: {} | ❤️ {} likes, 💬 {} comments, 👁️ {} views", 
                            processedCount, content.getExternalId(), captionPreview,
                            content.getLikesCount() != null ? content.getLikesCount() : 0,
                            content.getCommentsCount() != null ? content.getCommentsCount() : 0,
                            content.getViewsCount() != null ? content.getViewsCount() : 0);
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
            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            
            // Verificar se precisa fazer login
            if (needsLogin(currentUrl, pageSource)) {
                log.info("🔐 Detectada necessidade de login, executando...");
                if (!performLogin(driver)) {
                    log.error("❌ Falha no login automático");
                    return false;
                }
                
                // Após login, tentar navegar novamente para o perfil
                String profileUrl = INSTAGRAM_BASE_URL + profile.getUsername() + "/";
                driver.get(profileUrl);
                randomDelay(3000, 5000);
                
                currentUrl = driver.getCurrentUrl();
                pageSource = driver.getPageSource();
            }
            
            // Verificar se estamos na página correta
            if (!currentUrl.contains(profile.getUsername()) && !currentUrl.contains("instagram.com")) {
                log.error("❌ URL incorreta após validação: {}", currentUrl);
                return false;
            }
            
            // Verificar se não há erro 404
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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            try {
                wait.until(d -> d.getPageSource().contains("instagram") || 
                          d.getPageSource().contains("Instagram"));
            } catch (TimeoutException e) {
                log.warn("⚠️ Timeout aguardando elementos do Instagram");
                return false;
            }
            
            log.info("✅ Página carregada e validada corretamente");
            return true;
            
        } catch (Exception e) {
            log.error("❌ Erro na validação da página: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean needsLogin(String currentUrl, String pageSource) {
        return currentUrl.contains("/accounts/login/") || 
               currentUrl.contains("login") ||
               pageSource.contains("loginForm") ||
               pageSource.contains("Log in to Instagram") ||
               pageSource.contains("Entrar no Instagram") ||
               pageSource.contains("username") && pageSource.contains("password");
    }
    
    private boolean performLogin(WebDriver driver) {
        try {
            log.info("🚀 Iniciando processo de login automático...");
            
            // Navegar para página de login se não estivermos lá
            if (!driver.getCurrentUrl().contains("/accounts/login/")) {
                driver.get("https://www.instagram.com/accounts/login/");
                randomDelay(3000, 5000);
            }
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            // Aguardar elementos de login aparecerem
            log.info("⏳ Aguardando formulário de login...");
            
            // Múltiplos seletores para o campo de username
            String[] usernameSelectors = {
                "input[name='username']",
                "input[type='text'][autocomplete='username']",
                "input[aria-label*='usuário']",
                "input[aria-label*='username']",
                "input[placeholder*='usuário']",
                "input[placeholder*='username']"
            };
            
            WebElement usernameField = null;
            for (String selector : usernameSelectors) {
                try {
                    usernameField = wait.until(webDriver -> webDriver.findElement(By.cssSelector(selector)));
                    if (usernameField.isDisplayed()) {
                        log.debug("✅ Campo username encontrado com seletor: {}", selector);
                        break;
                    }
                } catch (Exception e) {
                    log.debug("❌ Seletor {} falhou: {}", selector, e.getMessage());
                }
            }
            
            if (usernameField == null) {
                log.error("❌ Campo de username não encontrado");
                return false;
            }
            
            // Múltiplos seletores para o campo de senha
            String[] passwordSelectors = {
                "input[name='password']",
                "input[type='password']",
                "input[aria-label*='senha']",
                "input[aria-label*='password']"
            };
            
            WebElement passwordField = null;
            for (String selector : passwordSelectors) {
                try {
                    passwordField = driver.findElement(By.cssSelector(selector));
                    if (passwordField.isDisplayed()) {
                        log.debug("✅ Campo password encontrado com seletor: {}", selector);
                        break;
                    }
                } catch (Exception e) {
                    log.debug("❌ Seletor {} falhou: {}", selector, e.getMessage());
                }
            }
            
            if (passwordField == null) {
                log.error("❌ Campo de senha não encontrado");
                return false;
            }
            
            // Preencher credenciais com delays humanos
            log.info("📝 Preenchendo credenciais com simulação humana...");
            
            // Limpar campo username com delay humano
            usernameField.clear();
            humanDelay(300, 800);
            
            // Digitar username com velocidade humana
            typeWithHumanSpeed(usernameField, "rorato.rafa");
            humanDelay(800, 1500);
            
            // Limpar campo password com delay humano  
            passwordField.clear();
            humanDelay(400, 900);
            
            // Digitar senha com velocidade humana
            typeWithHumanSpeed(passwordField, "R@f@4636");
            humanDelay(1200, 2500);
            
            // Encontrar e clicar no botão de login
            String[] loginButtonSelectors = {
                "button[type='submit']",
                "button:contains('Entrar')",
                "button:contains('Log in')",
                "div[role='button']:contains('Entrar')",
                "div[role='button']:contains('Log in')"
            };
            
            WebElement loginButton = null;
            for (String selector : loginButtonSelectors) {
                try {
                    if (selector.contains(":contains(")) {
                        // Para seletores com :contains, usar XPath
                        String text = selector.contains("Entrar") ? "Entrar" : "Log in";
                        loginButton = driver.findElement(By.xpath("//button[contains(text(), '" + text + "')] | //div[@role='button'][contains(text(), '" + text + "')]"));
                    } else {
                        loginButton = driver.findElement(By.cssSelector(selector));
                    }
                    
                    if (loginButton.isDisplayed() && loginButton.isEnabled()) {
                        log.debug("✅ Botão login encontrado: {}", selector);
                        break;
                    }
                } catch (Exception e) {
                    log.debug("❌ Botão {} não encontrado: {}", selector, e.getMessage());
                }
            }
            
            if (loginButton == null) {
                // Fallback: tentar Enter no campo de senha com delay humano
                log.info("🔄 Botão não encontrado, tentando Enter com delay humano...");
                humanDelay(300, 700);
                passwordField.sendKeys(Keys.ENTER);
            } else {
                log.info("🔐 Clicando no botão de login com movimento humano...");
                humanClick(loginButton, driver);
            }
            
            // Aguardar e verificar captcha/redirecionamento
            log.info("⏳ Aguardando redirecionamento pós-login...");
            humanDelay(3000, 5000);
            
            // Verificar se apareceu captcha
            if (detectAndHandleCaptcha(driver)) {
                log.info("🤖 Captcha detectado e tratado, continuando...");
                humanDelay(2000, 4000);
            }
            
            // Aguardar mais um pouco após possível captcha
            humanDelay(2000, 4000);
            
            // Verificar se login foi bem-sucedido
            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            
            boolean loginSuccess = !currentUrl.contains("/accounts/login/") &&
                                 !pageSource.contains("loginForm") &&
                                 !pageSource.contains("incorrect") &&
                                 !pageSource.contains("erro") &&
                                 !pageSource.contains("challenge") &&
                                 !pageSource.contains("captcha");
                                 
            if (loginSuccess) {
                log.info("✅ Login realizado com sucesso!");
                
                // Salvar cookies para próximas execuções
                String sessionId = cookieManager.getSessionId("rorato.rafa");
                cookieManager.saveCookies(driver, sessionId);
                
                // Lidar com possíveis pop-ups pós-login
                handlePostLoginPopups(driver);
                
                return true;
            } else {
                log.error("❌ Login falhou - ainda na página de login ou erro detectado");
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Erro durante login automático: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private void handlePostLoginPopups(WebDriver driver) {
        try {
            log.info("🚫 Verificando pop-ups pós-login...");
            
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            
            // Possíveis pop-ups para dispensar
            String[] dismissSelectors = {
                "button:contains('Agora não')",
                "button:contains('Not now')",
                "button:contains('Não')",
                "button:contains('No')",
                "button[role='button']:contains('Dismiss')",
                "[data-testid='turnOnNotifications'] button",
                "div[role='dialog'] button"
            };
            
            for (String selector : dismissSelectors) {
                try {
                    WebElement popup;
                    if (selector.contains(":contains(")) {
                        String text = selector.replaceAll(".*:contains\\('([^']+)'\\).*", "$1");
                        popup = shortWait.until(webDriver -> {
                            try {
                                return webDriver.findElement(By.xpath("//button[contains(text(), '" + text + "')]"));
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    } else {
                        popup = shortWait.until(webDriver -> {
                            try {
                                WebElement el = webDriver.findElement(By.cssSelector(selector));
                                return el.isDisplayed() ? el : null;
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    }
                    
                    if (popup != null) {
                        popup.click();
                        log.info("✅ Pop-up dispensado: {}", selector);
                        randomDelay(1000, 2000);
                    }
                    
                } catch (TimeoutException e) {
                    // Pop-up não encontrado, continuar
                }
            }
            
            log.info("✅ Verificação de pop-ups concluída");
            
        } catch (Exception e) {
            log.debug("Pop-ups pós-login: {}", e.getMessage());
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
    
    private Content createContentFromUrl(String postUrl, Profile profile, WebDriver driver, boolean forceUpdate, ScrapingExecution execution) {
        try {
            String shortcode = extractShortcode(postUrl);
            
            // Verificar se o post já existe (a menos que force update esteja ativado)
            if (!forceUpdate) {
                Optional<Content> existingContent = contentRepository.findByExternalId(shortcode);
                if (existingContent.isPresent()) {
                    log.debug("⏭️ Post {} já existe, pulando...", shortcode);
                    
                    // Atualizar contadores da execução
                    execution.setPostsSkipped((execution.getPostsSkipped() != null ? execution.getPostsSkipped() : 0) + 1);
                    executionRepository.save(execution);
                    
                    return null; // Não processar duplicata
                }
            }
            
            ContentType type = postUrl.contains("/reel/") ? ContentType.REEL : ContentType.POST;
            
            // Extrair caption do post
            String caption = extractPostCaption(driver, postUrl);
            if (caption == null || caption.trim().isEmpty()) {
                caption = "Sem descrição disponível";
            }
            
            log.debug("✅ Caption extraída para {}: {}", shortcode, 
                     caption.length() > 50 ? caption.substring(0, 50) + "..." : caption);
            
            // Extrair métricas de engajamento (likes, views, comentários)
            EngagementMetrics metrics = extractEngagementMetrics(driver, postUrl);
            
            Content content = Content.builder()
                    .profile(profile)
                    .externalId(shortcode)
                    .url(postUrl)
                    .caption(caption)
                    .type(type)
                    .likesCount(metrics.likes)
                    .commentsCount(metrics.comments)
                    .viewsCount(metrics.views)
                    .collectedAt(Instant.now())
                    .build();
            
            // Atualizar contadores da execução
            execution.setCaptionsExtracted((execution.getCaptionsExtracted() != null ? execution.getCaptionsExtracted() : 0) + 1);
            
            if (forceUpdate) {
                execution.setPostsUpdated((execution.getPostsUpdated() != null ? execution.getPostsUpdated() : 0) + 1);
            } else {
                execution.setPostsNew((execution.getPostsNew() != null ? execution.getPostsNew() : 0) + 1);
            }
            
            executionRepository.save(execution);
            
            return content;
                    
        } catch (Exception e) {
            log.error("❌ Erro ao criar content: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractPostCaption(WebDriver driver, String postUrl) {
        log.debug("🔍 Extraindo caption para: {}", postUrl);
        
        try {
            // CORREÇÃO: SEMPRE navegar para o post individual primeiro
            // A grid do perfil não contém as captions completas
            String caption = extractCaptionByNavigatingToPost(driver, postUrl);
            if (caption != null && !caption.trim().isEmpty()) {
                log.debug("✅ Caption encontrado navegando para o post");
                return cleanCaption(caption);
            }
            
            // Estratégia 2: Buscar no JSON embutido (backup)
            caption = extractCaptionFromJSON(driver);
            if (caption != null && !caption.trim().isEmpty()) {
                log.debug("✅ Caption encontrado no JSON");
                return cleanCaption(caption);
            }
            
            // Estratégia 3: Buscar no HTML atual (última tentativa)
            caption = extractCaptionFromCurrentPage(driver, postUrl);
            if (caption != null && !caption.trim().isEmpty()) {
                log.debug("✅ Caption encontrado na página atual");
                return cleanCaption(caption);
            }
            
            log.debug("⚠️ Nenhum caption encontrado");
            return null;
            
        } catch (Exception e) {
            log.debug("❌ Erro ao extrair caption: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractCaptionFromCurrentPage(WebDriver driver, String postUrl) {
        try {
            // Seletores possíveis para caption no Instagram
            String[] captionSelectors = {
                "article img[alt]", // Alt text das imagens
                "[data-testid='post-caption']",
                "article h1",
                "span[dir='auto']", // Texto com direção automática
                ".C4VMK span", // Classe específica do Instagram
                "article span:not([class*='icon'])", // Spans sem classe de ícone
                "figure + div span", // Span após figure
                "h2 + div span" // Span após h2
            };
            
            for (String selector : captionSelectors) {
                try {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    
                    for (WebElement element : elements) {
                        String text = element.getAttribute("alt");
                        if (text == null) {
                            text = element.getText();
                        }
                        
                        if (text != null && text.trim().length() > 10 && 
                            !text.toLowerCase().contains("instagram") &&
                            !text.toLowerCase().contains("photo") &&
                            !text.toLowerCase().contains("image")) {
                            
                            return text.trim();
                        }
                    }
                } catch (Exception e) {
                    log.debug("Erro com seletor {}: {}", selector, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.debug("Erro na extração da página atual: {}", e.getMessage());
        }
        
        return null;
    }
    
    private String extractCaptionByNavigatingToPost(WebDriver driver, String postUrl) {
        String currentUrl = driver.getCurrentUrl();
        
        try {
            log.debug("📍 Navegando para post individual: {}", postUrl);
            
            // Navegar para o post específico
            driver.get(postUrl);
            randomDelay(3000, 5000); // Delay maior para garantir carregamento completo
            
            // Aguardar carregamento
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(d -> d.getPageSource().length() > 1000);
            
            // ESTRATÉGIA 1: Meta tags (mais confiável)
            try {
                WebElement metaDesc = driver.findElement(By.cssSelector("meta[property='og:description']"));
                String content = metaDesc.getAttribute("content");
                if (content != null && content.length() > 5) {
                    log.debug("✅ Caption encontrado em meta tag");
                    return content;
                }
            } catch (Exception e) {
                log.debug("Meta tag não encontrada");
            }
            
            // ESTRATÉGIA 2: Atributo alt da imagem/vídeo principal
            try {
                WebElement img = driver.findElement(By.cssSelector("article img[alt]"));
                String alt = img.getAttribute("alt");
                if (alt != null && alt.length() > 5 && 
                    !alt.toLowerCase().contains("photo by") &&
                    !alt.toLowerCase().contains("image may contain")) {
                    log.debug("✅ Caption encontrado em alt da imagem");
                    return alt;
                }
            } catch (Exception e) {
                log.debug("Alt da imagem não encontrado");
            }
            
            // ESTRATÉGIA 3: H1 do artigo (caption principal)
            try {
                WebElement h1 = driver.findElement(By.cssSelector("article h1"));
                String text = h1.getText();
                if (text != null && text.length() > 5) {
                    log.debug("✅ Caption encontrado em H1");
                    return text;
                }
            } catch (Exception e) {
                log.debug("H1 não encontrado");
            }
            
            // ESTRATÉGIA 4: Span com dir='auto' dentro do article (texto da caption)
            try {
                List<WebElement> spans = driver.findElements(By.cssSelector("article span[dir='auto']"));
                for (WebElement span : spans) {
                    String text = span.getText();
                    if (text != null && text.length() > 10 &&
                        !text.matches("\\d+")) { // Não é apenas números (likes, etc)
                        
                        log.debug("✅ Caption encontrado em span[dir='auto']");
                        return text;
                    }
                }
            } catch (Exception e) {
                log.debug("Spans com dir='auto' não encontrados");
            }
            
            // ESTRATÉGIA 5: Busca mais ampla em spans dentro do article
            try {
                List<WebElement> allSpans = driver.findElements(By.cssSelector("article span"));
                
                // Procurar o span com maior texto (provavelmente a caption)
                String longestText = "";
                for (WebElement span : allSpans) {
                    String text = span.getText();
                    if (text != null && text.length() > longestText.length() &&
                        text.length() > 10 &&
                        !text.toLowerCase().contains("curtir") &&
                        !text.toLowerCase().contains("comment") &&
                        !text.toLowerCase().contains("compartilhar") &&
                        !text.toLowerCase().contains("like") &&
                        !text.toLowerCase().contains("share")) {
                        
                        longestText = text;
                    }
                }
                
                if (longestText.length() > 10) {
                    log.debug("✅ Caption encontrado procurando pelo maior texto");
                    return longestText;
                }
            } catch (Exception e) {
                log.debug("Busca ampla em spans falhou");
            }
            
            log.debug("⚠️ Nenhuma caption encontrada na página do post");
            
        } catch (Exception e) {
            log.debug("❌ Erro ao navegar para o post: {}", e.getMessage());
        } finally {
            // Voltar para a página original
            try {
                if (!currentUrl.equals(driver.getCurrentUrl())) {
                    log.debug("🔙 Voltando para página original: {}", currentUrl);
                    driver.get(currentUrl);
                    randomDelay(2000, 3000);
                }
            } catch (Exception e) {
                log.debug("Erro ao voltar para página original: {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    private String extractCaptionFromJSON(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Script para buscar dados de posts no JavaScript
            String script = """
                var captions = [];
                
                // Buscar em possíveis estruturas de dados do Instagram
                if (window.__additionalDataLoaded) {
                    try {
                        var data = JSON.stringify(window.__additionalDataLoaded);
                        var matches = data.match(/"caption"\\s*:\\s*"([^"]+)"/g);
                        if (matches) {
                            matches.forEach(function(match) {
                                var caption = match.match(/"caption"\\s*:\\s*"([^"]+)"/)[1];
                                if (caption && caption.length > 10) {
                                    captions.push(caption);
                                }
                            });
                        }
                    } catch(e) {}
                }
                
                // Buscar em elementos com dados
                document.querySelectorAll('[data-testid]').forEach(function(el) {
                    if (el.innerText && el.innerText.length > 10) {
                        captions.push(el.innerText);
                    }
                });
                
                return captions.length > 0 ? captions[0] : null;
                """;
                
            Object result = js.executeScript(script);
            if (result instanceof String && ((String) result).length() > 10) {
                return (String) result;
            }
            
        } catch (Exception e) {
            log.debug("Erro na extração JSON: {}", e.getMessage());
        }
        
        return null;
    }
    
    private String cleanCaption(String caption) {
        if (caption == null) return null;
        
        // Limpar o caption
        caption = caption.trim();
        
        // Remover caracteres de escape
        caption = caption.replace("\\n", "\n")
                        .replace("\\t", " ")
                        .replace("\\\"", "\"");
        
        // Limitar tamanho (database constraint é text, mas vamos ser conservadores)
        if (caption.length() > 2000) {
            caption = caption.substring(0, 1997) + "...";
        }
        
        return caption;
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
    
    /**
     * Classe interna para armazenar métricas de engajamento
     */
    private static class EngagementMetrics {
        Long likes = 0L;
        Long comments = 0L;
        Long views = 0L;
    }
    
    /**
     * Extrai métricas de engajamento (likes, views, comentários) do post
     */
    private EngagementMetrics extractEngagementMetrics(WebDriver driver, String postUrl) {
        EngagementMetrics metrics = new EngagementMetrics();
        String currentUrl = driver.getCurrentUrl();
        
        try {
            // Se não estiver na página do post, navegar
            if (!currentUrl.equals(postUrl)) {
                log.debug("📊 Navegando para extrair métricas: {}", postUrl);
                driver.get(postUrl);
                randomDelay(2000, 3000);
            }
            
            // Aguardar carregamento
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(d -> d.getPageSource().length() > 1000);
            
            // ESTRATÉGIA 1: Buscar em spans e buttons com aria-label
            try {
                List<WebElement> buttons = driver.findElements(By.cssSelector("section button[aria-label], section span[aria-label]"));
                
                for (WebElement button : buttons) {
                    String ariaLabel = button.getAttribute("aria-label");
                    if (ariaLabel != null) {
                        ariaLabel = ariaLabel.toLowerCase();
                        
                        // Likes (curtidas)
                        if (ariaLabel.contains("like") || ariaLabel.contains("curtir")) {
                            metrics.likes = extractNumberFromText(ariaLabel);
                            log.debug("❤️ Likes encontrados: {}", metrics.likes);
                        }
                        
                        // Comments (comentários)
                        if (ariaLabel.contains("comment") || ariaLabel.contains("comentário")) {
                            metrics.comments = extractNumberFromText(ariaLabel);
                            log.debug("💬 Comentários encontrados: {}", metrics.comments);
                        }
                        
                        // Views (visualizações) - apenas para REELs
                        if (ariaLabel.contains("view") || ariaLabel.contains("visualiza")) {
                            metrics.views = extractNumberFromText(ariaLabel);
                            log.debug("👁️ Views encontrados: {}", metrics.views);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Erro ao buscar métricas em aria-labels: {}", e.getMessage());
            }
            
            // ESTRATÉGIA 2: Buscar em spans com texto visível
            if (metrics.likes == 0 || metrics.comments == 0) {
                try {
                    List<WebElement> spans = driver.findElements(By.cssSelector("section span, section a"));
                    
                    for (WebElement span : spans) {
                        String text = span.getText().toLowerCase();
                        
                        if (text.contains("curtida") || text.contains("like")) {
                            Long count = extractNumberFromText(text);
                            if (count > 0) {
                                metrics.likes = count;
                                log.debug("❤️ Likes (texto): {}", metrics.likes);
                            }
                        }
                        
                        if (text.contains("comentário") || text.contains("comment")) {
                            Long count = extractNumberFromText(text);
                            if (count > 0) {
                                metrics.comments = count;
                                log.debug("💬 Comentários (texto): {}", metrics.comments);
                            }
                        }
                        
                        if (text.contains("visualiza") || text.contains("view")) {
                            Long count = extractNumberFromText(text);
                            if (count > 0) {
                                metrics.views = count;
                                log.debug("👁️ Views (texto): {}", metrics.views);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Erro ao buscar métricas em texto: {}", e.getMessage());
                }
            }
            
            // ESTRATÉGIA 3: Buscar em meta tags
            if (metrics.views == 0) {
                try {
                    WebElement metaViews = driver.findElement(By.cssSelector("meta[property='video:views']"));
                    String content = metaViews.getAttribute("content");
                    if (content != null) {
                        metrics.views = Long.parseLong(content.replaceAll("[^0-9]", ""));
                        log.debug("👁️ Views (meta): {}", metrics.views);
                    }
                } catch (Exception e) {
                    // Não há meta tag de views
                }
            }
            
            log.debug("📊 Métricas extraídas - Likes: {}, Comentários: {}, Views: {}", 
                     metrics.likes, metrics.comments, metrics.views);
            
        } catch (Exception e) {
            log.debug("❌ Erro ao extrair métricas: {}", e.getMessage());
        } finally {
            // Voltar para a página original se necessário
            if (!currentUrl.equals(postUrl) && !currentUrl.isEmpty()) {
                try {
                    driver.get(currentUrl);
                    randomDelay(1000, 2000);
                } catch (Exception e) {
                    log.debug("Erro ao voltar para página original: {}", e.getMessage());
                }
            }
        }
        
        return metrics;
    }
    
    /**
     * Extrai números de um texto (ex: "1,234 likes" -> 1234)
     */
    private Long extractNumberFromText(String text) {
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        
        try {
            // Remover tudo exceto dígitos e vírgulas/pontos
            String cleaned = text.replaceAll("[^0-9,.]", "");
            
            // Remover vírgulas e pontos de milhar
            cleaned = cleaned.replaceAll("[,.](?=\\d{3})", "");
            
            // Converter para número
            if (!cleaned.isEmpty()) {
                return Long.parseLong(cleaned.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair número de '{}': {}", text, e.getMessage());
        }
        
        return 0L;
    }
    
    /**
     * Delay mais humanizado com distribuição gaussiana
     */
    private void humanDelay(int minMs, int maxMs) {
        try {
            // Usar distribuição gaussiana para delays mais naturais
            double mean = (minMs + maxMs) / 2.0;
            double stdDev = (maxMs - minMs) / 6.0; // 99.7% dentro do range
            
            Random random = ThreadLocalRandom.current();
            double gaussianDelay = random.nextGaussian() * stdDev + mean;
            
            // Garantir que está dentro do range
            int delay = (int) Math.max(minMs, Math.min(maxMs, gaussianDelay));
            
            Thread.sleep(delay);
            log.debug("⏱️ Human delay: {}ms", delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Digitação com velocidade humana - simula digitação character by character
     */
    private void typeWithHumanSpeed(WebElement element, String text) {
        try {
            for (char c : text.toCharArray()) {
                element.sendKeys(String.valueOf(c));
                
                // Delay entre caracteres - humanos não digitam uniformemente
                int charDelay = ThreadLocalRandom.current().nextInt(80, 200);
                
                // Alguns caracteres demoram mais (símbolos, maiúsculas)
                if (Character.isUpperCase(c) || "!@#$%^&*()".indexOf(c) >= 0) {
                    charDelay += ThreadLocalRandom.current().nextInt(50, 150);
                }
                
                Thread.sleep(charDelay);
            }
            
            log.debug("⌨️ Typed '{}' with human speed", text.replaceAll(".", "*"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Simula movimento de mouse antes de clicar
     */
    private void humanClick(WebElement element, WebDriver driver) {
        try {
            // Delay antes do clique
            humanDelay(200, 600);
            
            // Simular hover usando JavaScript
            try {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].dispatchEvent(new MouseEvent('mouseover', {bubbles: true}));", element);
                humanDelay(100, 300);
            } catch (Exception e) {
                // Ignore hover errors
                log.debug("Hover simulation failed: {}", e.getMessage());
            }
            
            element.click();
            log.debug("🖱️ Human click executed");
            
        } catch (Exception e) {
            log.debug("Click fallback: {}", e.getMessage());
            element.click(); // Fallback to regular click
        }
    }
    
    public List<Content> scrapeAndSaveProfile(Profile profile) {
        return scrapeAndSaveProfile(profile, false);
    }
    
    public List<Content> scrapeAndSaveProfile(Profile profile, boolean forceUpdate) {
        List<Content> contents = scrapeProfile(profile, forceUpdate);
        if (!contents.isEmpty()) {
            return contentRepository.saveAll(contents);
        }
        return contents;
    }
}