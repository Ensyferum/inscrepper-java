package com.ensyferum.inscrepper.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerenciador de proxies rotativos para evitar detecção por IP
 */
@Slf4j
@Component
@Data
public class ProxyRotationManager {
    
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    // Lista de proxies públicos (em produção, usar proxies pagos e confiáveis)
    private final List<ProxyConfig> proxies = List.of(
        new ProxyConfig("proxy1.example.com", 8080, "user1", "pass1"),
        new ProxyConfig("proxy2.example.com", 8080, "user2", "pass2"),
        new ProxyConfig("proxy3.example.com", 3128, "user3", "pass3")
        // Adicionar mais proxies conforme necessário
    );
    
    /**
     * Obtém o próximo proxy na rotação
     */
    public ProxyConfig getNextProxy() {
        if (proxies.isEmpty()) {
            return null;
        }
        
        int index = currentIndex.getAndIncrement() % proxies.size();
        ProxyConfig proxy = proxies.get(index);
        
        log.debug("🌐 Selecionado proxy {}: {}:{}", index + 1, proxy.getHost(), proxy.getPort());
        return proxy;
    }
    
    /**
     * Obtém um proxy aleatório (em vez de sequencial)
     */
    public ProxyConfig getRandomProxy() {
        if (proxies.isEmpty()) {
            return null;
        }
        
        int randomIndex = ThreadLocalRandom.current().nextInt(proxies.size());
        ProxyConfig proxy = proxies.get(randomIndex);
        
        log.debug("🎲 Proxy aleatório selecionado: {}:{}", proxy.getHost(), proxy.getPort());
        return proxy;
    }
    
    /**
     * Verifica se há proxies disponíveis
     */
    public boolean hasProxies() {
        return !proxies.isEmpty();
    }
    
    /**
     * Obtém quantidade de proxies disponíveis
     */
    public int getProxyCount() {
        return proxies.size();
    }
    
    /**
     * Configuração de um proxy individual
     */
    @Data
    public static class ProxyConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        
        public ProxyConfig(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }
        
        /**
         * Constrói string de proxy para Chrome
         */
        public String toChromeProxyString() {
            return host + ":" + port;
        }
        
        /**
         * Verifica se o proxy tem autenticação
         */
        public boolean hasAuth() {
            return username != null && !username.trim().isEmpty() && 
                   password != null && !password.trim().isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("%s:%d", host, port);
        }
    }
}