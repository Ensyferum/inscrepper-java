package com.ensyferum.inscrepper.service;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;

public class InstagramStructureAnalysisTest {

    @Test
    public void analyzeInstagramStructure() {
        WebDriver driver = null;
        
        try {
            // Setup WebDriver
            WebDriverManager.chromedriver().setup();
            
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            String profileUrl = "https://www.instagram.com/oncallpeds/";
            System.out.println("Acessando: " + profileUrl);
            
            driver.get(profileUrl);
            Thread.sleep(5000); // Aguardar carregamento
            
            System.out.println("Página carregada. Tamanho do HTML: " + driver.getPageSource().length() + " caracteres");
            
            // Analisar diferentes elementos
            analyzeElements(driver, "article", "Artigos");
            analyzeElements(driver, "section", "Seções");  
            analyzeElements(driver, "main", "Main");
            analyzeElements(driver, "[role='main']", "Role Main");
            analyzeElements(driver, "a[href*='/p/']", "Links para Posts");
            analyzeElements(driver, "img", "Imagens");
            analyzeElements(driver, "div", "Divs (primeiros 5)");
            
            // Verificar se há mensagem de erro ou login
            checkForCommonIssues(driver);
            
            // Tentar encontrar posts com diferentes estratégias
            tryDifferentSelectors(driver);
            
        } catch (Exception e) {
            System.err.println("Erro no teste: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    private void analyzeElements(WebDriver driver, String selector, String description) {
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector(selector));
            System.out.println("\n" + description + " (" + selector + "): " + elements.size() + " encontrados");
            
            if (selector.equals("div") && elements.size() > 5) {
                // Limitar divs para não poluir o output
                elements = elements.subList(0, 5);
            }
            
            for (int i = 0; i < Math.min(elements.size(), 3); i++) {
                WebElement element = elements.get(i);
                try {
                    String tagName = element.getTagName();
                    String className = element.getAttribute("class");
                    String href = element.getAttribute("href");
                    String src = element.getAttribute("src");
                    String text = element.getText();
                    
                    System.out.println("  [" + (i+1) + "] Tag: " + tagName);
                    if (className != null && !className.isEmpty()) {
                        System.out.println("      Class: " + (className.length() > 50 ? className.substring(0, 50) + "..." : className));
                    }
                    if (href != null && !href.isEmpty()) {
                        System.out.println("      Href: " + href);
                    }
                    if (src != null && !src.isEmpty()) {
                        System.out.println("      Src: " + (src.length() > 80 ? src.substring(0, 80) + "..." : src));
                    }
                    if (text != null && !text.trim().isEmpty()) {
                        String cleanText = text.trim().replaceAll("\\s+", " ");
                        System.out.println("      Text: " + (cleanText.length() > 100 ? cleanText.substring(0, 100) + "..." : cleanText));
                    }
                } catch (Exception e) {
                    System.out.println("  [" + (i+1) + "] Erro ao analisar elemento: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar " + description + ": " + e.getMessage());
        }
    }
    
    private void checkForCommonIssues(WebDriver driver) {
        System.out.println("\n=== VERIFICANDO PROBLEMAS COMUNS ===");
        
        // Verificar título da página
        String title = driver.getTitle();
        System.out.println("Título da página: " + title);
        
        // Verificar se há mensagem de login
        try {
            List<WebElement> loginElements = driver.findElements(By.cssSelector("input[type='password'], [data-testid='royal_login_form']"));
            if (!loginElements.isEmpty()) {
                System.out.println("⚠️  Página de login detectada!");
            }
        } catch (Exception e) {
            System.out.println("Erro ao verificar login: " + e.getMessage());
        }
        
        // Verificar se perfil existe
        try {
            List<WebElement> notFoundElements = driver.findElements(By.cssSelector("h2, h1"));
            for (WebElement element : notFoundElements) {
                String text = element.getText().toLowerCase();
                if (text.contains("not found") || text.contains("não encontrado") || text.contains("sorry")) {
                    System.out.println("⚠️  Possível perfil não encontrado: " + element.getText());
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao verificar se perfil existe: " + e.getMessage());
        }
        
        // Verificar JavaScript
        try {
            Object jsResult = ((ChromeDriver) driver).executeScript("return typeof window.React !== 'undefined' || typeof window._sharedData !== 'undefined';");
            System.out.println("JavaScript React/Instagram detectado: " + jsResult);
        } catch (Exception e) {
            System.out.println("Erro ao verificar JavaScript: " + e.getMessage());
        }
    }
    
    private void tryDifferentSelectors(WebDriver driver) {
        System.out.println("\n=== TESTANDO SELETORES PARA POSTS ===");
        
        String[] selectors = {
            "a[href*='/p/']",
            "article a[href*='/p/']",
            "div a[href*='/p/']",
            "section a[href*='/p/']",
            "[role='main'] a[href*='/p/']",
            "main a[href*='/p/']",
            "a[href*='/p/'][role='link']",
            "a[href*='/p/'] img",
            "div[style*='display: grid'] a",
            "div[style*='flex'] a[href*='/p/']"
        };
        
        for (String selector : selectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                System.out.println(selector + " -> " + elements.size() + " elementos");
                
                if (!elements.isEmpty()) {
                    WebElement first = elements.get(0);
                    try {
                        String href = first.getAttribute("href");
                        if (href != null && href.contains("/p/")) {
                            System.out.println("    ✅ Primeiro link: " + href);
                        }
                    } catch (Exception e) {
                        System.out.println("    ❌ Erro ao obter href: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println(selector + " -> ERRO: " + e.getMessage());
            }
        }
    }
}