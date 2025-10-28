package com.ensyferum.inscrepper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
public class InscrepperApplication {
    public static void main(String[] args) {
        SpringApplication.run(InscrepperApplication.class, args);
    }

    @RestController
    static class HealthController {
        @GetMapping("/health")
        public Map<String, String> health() {
            return Map.of("status", "ok");
        }
    }
}
