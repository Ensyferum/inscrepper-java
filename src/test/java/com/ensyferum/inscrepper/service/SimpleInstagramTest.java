package com.ensyferum.inscrepper.service;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;

public class SimpleInstagramTest {

    @Test
    public void simpleInstagramAccess() {
        System.out.println("=== TESTE SIMPLES DE ACESSO AO INSTAGRAM ===");
        
        WebDriver driver = null;
        
        try {
            // Setup básico do WebDriver
            WebDriverManager.chromedriver().setup();
            
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1366,768");
            
            // User agent mais básico
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            
            String url = "https://www.instagram.com/oncallpeds/";
            System.out.println("1. Acessando: " + url);
            
            driver.get(url);
            
            System.out.println("2. Página carregada. URL atual: " + driver.getCurrentUrl());
            System.out.println("3. Título da página: " + driver.getTitle());
            
            // Aguardar um pouco
            Thread.sleep(3000);
            
            // Verificar o tamanho da página
            String pageSource = driver.getPageSource();
            System.out.println("4. Tamanho do HTML: " + pageSource.length() + " caracteres");
            
            // Verificar se há conteúdo básico do Instagram
            if (pageSource.contains("Instagram") || pageSource.contains("instagram")) {
                System.out.println("✅ Conteúdo do Instagram detectado na página");
            } else {
                System.out.println("❌ Conteúdo do Instagram NÃO detectado");
            }
            
            // Procurar por elementos básicos
            checkBasicElements(driver);
            
            // Tentar scroll para carregar mais conteúdo
            System.out.println("5. Tentando scroll para carregar conteúdo dinâmico...");
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(2000);
            
            // Verificar novamente após scroll
            checkForPosts(driver);
            
            System.out.println("6. Teste concluído com sucesso!");
            
        } catch (Exception e) {
            System.err.println("❌ Erro durante o teste: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Erro ao fechar driver: " + e.getMessage());
                }
            }
        }
    }
    
    private void checkBasicElements(WebDriver driver) {
        System.out.println("\n--- Verificando elementos básicos ---");
        
        try {
            // Verificar se há H1 ou H2
            List<WebElement> headings = driver.findElements(By.cssSelector("h1, h2"));
            System.out.println("Cabeçalhos encontrados: " + headings.size());
            
            for (int i = 0; i < Math.min(headings.size(), 2); i++) {
                WebElement heading = headings.get(i);
                String text = heading.getText().trim();
                if (!text.isEmpty()) {
                    System.out.println("  - " + heading.getTagName().toUpperCase() + ": " + text);
                }
            }
            
            // Verificar links
            List<WebElement> links = driver.findElements(By.tagName("a"));
            System.out.println("Total de links: " + links.size());
            
            // Verificar imagens
            List<WebElement> images = driver.findElements(By.tagName("img"));
            System.out.println("Total de imagens: " + images.size());
            
            // Verificar se há elementos com classes do Instagram
            List<WebElement> instagramElements = driver.findElements(By.cssSelector("[class*='x'], [class*='_a'], [data-testid]"));
            System.out.println("Elementos com classes típicas do Instagram: " + instagramElements.size());
            
        } catch (Exception e) {
            System.err.println("Erro ao verificar elementos básicos: " + e.getMessage());
        }
    }
    
    private void checkForPosts(WebDriver driver) {
        System.out.println("\n--- Procurando por posts ---");
        
        String[] postSelectors = {
            "a[href*='/p/']",
            "a[href*='/reel/']", 
            "[href*='/p/']",
            "[href*='/reel/']"
        };
        
        for (String selector : postSelectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                System.out.println(selector + " -> " + elements.size() + " elementos");
                
                if (!elements.isEmpty()) {
                    for (int i = 0; i < Math.min(elements.size(), 3); i++) {
                        WebElement element = elements.get(i);
                        String href = element.getAttribute("href");
                        if (href != null) {
                            System.out.println("  [" + (i+1) + "] " + href);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(selector + " -> ERRO: " + e.getMessage());
            }
        }
        
        // Verificar se há JavaScript executando
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript("return document.readyState;");
            System.out.println("Estado do documento: " + result);
            
            Object linksCount = js.executeScript("return document.querySelectorAll('a[href*=\"/p/\"]').length;");
            System.out.println("Links para posts via JavaScript: " + linksCount);
        } catch (Exception e) {
            System.out.println("Erro ao executar JavaScript: " + e.getMessage());
        }
    }
}