package com.ensyferum.inscrepper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gerenciador de cookies persistentes para manter sessões entre execuções
 */
@Slf4j
@Component
public class CookieManager {
    
    private static final String COOKIES_DIR = ".cookies";
    private static final String COOKIE_FILE_EXTENSION = ".json";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Salva os cookies atuais do WebDriver
     */
    public void saveCookies(WebDriver driver, String sessionId) {
        try {
            createCookiesDirectory();
            
            Set<Cookie> cookies = driver.manage().getCookies();
            List<SerializableCookie> serializableCookies = cookies.stream()
                    .map(SerializableCookie::from)
                    .collect(Collectors.toList());
            
            File cookieFile = new File(COOKIES_DIR, sessionId + COOKIE_FILE_EXTENSION);
            objectMapper.writeValue(cookieFile, serializableCookies);
            
            log.info("🍪 Salvos {} cookies para sessão: {}", cookies.size(), sessionId);
            log.debug("📁 Arquivo de cookies: {}", cookieFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("❌ Erro ao salvar cookies para {}: {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * Carrega cookies salvos para o WebDriver
     */
    public boolean loadCookies(WebDriver driver, String sessionId) {
        try {
            File cookieFile = new File(COOKIES_DIR, sessionId + COOKIE_FILE_EXTENSION);
            
            if (!cookieFile.exists()) {
                log.debug("📂 Nenhum arquivo de cookie encontrado para: {}", sessionId);
                return false;
            }
            
            // Verificar se o arquivo não é muito antigo (ex: 24 horas)
            long fileAge = System.currentTimeMillis() - cookieFile.lastModified();
            long maxAge = 24 * 60 * 60 * 1000; // 24 horas em ms
            
            if (fileAge > maxAge) {
                log.info("⏰ Cookies expirados para {}, removendo arquivo", sessionId);
                cookieFile.delete();
                return false;
            }
            
            List<SerializableCookie> serializableCookies = objectMapper.readValue(
                cookieFile, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, SerializableCookie.class)
            );
            
            int loadedCount = 0;
            for (SerializableCookie serializableCookie : serializableCookies) {
                try {
                    Cookie cookie = serializableCookie.toCookie();
                    
                    // Verificar se o cookie não expirou
                    if (cookie.getExpiry() != null && cookie.getExpiry().before(new Date())) {
                        log.debug("⏰ Cookie expirado ignorado: {}", cookie.getName());
                        continue;
                    }
                    
                    driver.manage().addCookie(cookie);
                    loadedCount++;
                    
                } catch (Exception e) {
                    log.debug("⚠️ Erro ao carregar cookie {}: {}", serializableCookie.getName(), e.getMessage());
                }
            }
            
            log.info("🍪 Carregados {} cookies para sessão: {}", loadedCount, sessionId);
            return loadedCount > 0;
            
        } catch (Exception e) {
            log.error("❌ Erro ao carregar cookies para {}: {}", sessionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove cookies salvos para uma sessão
     */
    public void clearCookies(String sessionId) {
        try {
            File cookieFile = new File(COOKIES_DIR, sessionId + COOKIE_FILE_EXTENSION);
            
            if (cookieFile.exists()) {
                boolean deleted = cookieFile.delete();
                if (deleted) {
                    log.info("🗑️ Cookies removidos para sessão: {}", sessionId);
                } else {
                    log.warn("⚠️ Falha ao remover cookies para: {}", sessionId);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erro ao remover cookies para {}: {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * Limpa cookies antigos (mais de 7 dias)
     */
    public void cleanupOldCookies() {
        try {
            File cookiesDir = new File(COOKIES_DIR);
            
            if (!cookiesDir.exists()) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // 7 dias
            File[] cookieFiles = cookiesDir.listFiles((dir, name) -> name.endsWith(COOKIE_FILE_EXTENSION));
            
            if (cookieFiles != null) {
                int cleanedCount = 0;
                for (File file : cookieFiles) {
                    if (file.lastModified() < cutoffTime) {
                        if (file.delete()) {
                            cleanedCount++;
                        }
                    }
                }
                
                if (cleanedCount > 0) {
                    log.info("🧹 Limpeza automática: {} arquivos de cookie antigos removidos", cleanedCount);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erro na limpeza automática de cookies: {}", e.getMessage());
        }
    }
    
    /**
     * Verifica se existem cookies salvos para uma sessão
     */
    public boolean hasCookies(String sessionId) {
        File cookieFile = new File(COOKIES_DIR, sessionId + COOKIE_FILE_EXTENSION);
        return cookieFile.exists() && cookieFile.length() > 0;
    }
    
    /**
     * Gera ID de sessão baseado no perfil
     */
    public String getSessionId(String username) {
        return "instagram_" + username.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
    
    private void createCookiesDirectory() throws IOException {
        File cookiesDir = new File(COOKIES_DIR);
        if (!cookiesDir.exists()) {
            boolean created = cookiesDir.mkdirs();
            if (!created) {
                throw new IOException("Falha ao criar diretório de cookies: " + COOKIES_DIR);
            }
        }
    }
    
    /**
     * Classe para serializar cookies
     */
    @Data
    public static class SerializableCookie {
        private String name;
        private String value;
        private String domain;
        private String path;
        private Date expiry;
        private boolean isSecure;
        private boolean isHttpOnly;
        private String sameSite;
        private long createdAt;
        
        public SerializableCookie() {
            this.createdAt = System.currentTimeMillis();
        }
        
        public static SerializableCookie from(Cookie cookie) {
            SerializableCookie sc = new SerializableCookie();
            sc.name = cookie.getName();
            sc.value = cookie.getValue();
            sc.domain = cookie.getDomain();
            sc.path = cookie.getPath();
            sc.expiry = cookie.getExpiry();
            sc.isSecure = cookie.isSecure();
            sc.isHttpOnly = cookie.isHttpOnly();
            sc.sameSite = cookie.getSameSite();
            return sc;
        }
        
        public Cookie toCookie() {
            Cookie.Builder builder = new Cookie.Builder(name, value);
            
            if (domain != null) {
                builder.domain(domain);
            }
            
            if (path != null) {
                builder.path(path);
            }
            
            if (expiry != null) {
                builder.expiresOn(expiry);
            }
            
            if (isSecure) {
                builder.isSecure(true);
            }
            
            if (isHttpOnly) {
                builder.isHttpOnly(true);
            }
            
            if (sameSite != null) {
                builder.sameSite(sameSite);
            }
            
            return builder.build();
        }
    }
}