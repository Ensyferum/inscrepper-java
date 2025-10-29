package com.ensyferum.inscrepper.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade que registra informações sobre cada execução de scraping
 */
@Entity
@Table(name = "scraping_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapingExecution {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "finished_at")
    private Instant finishedAt;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    
    @Column(name = "posts_found")
    private Integer postsFound;
    
    @Column(name = "posts_processed")
    private Integer postsProcessed;
    
    @Column(name = "posts_new")
    private Integer postsNew;
    
    @Column(name = "posts_updated")
    private Integer postsUpdated;
    
    @Column(name = "posts_skipped")
    private Integer postsSkipped;
    
    @Column(name = "captions_extracted")
    private Integer captionsExtracted;
    
    @Column(name = "images_saved")
    private Integer imagesSaved;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "viewport", length = 50)
    private String viewport;
    
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
    
    @Column(name = "force_update")
    private Boolean forceUpdate;
    
    @Column(name = "attempt_number")
    private Integer attemptNumber;
    
    public enum ExecutionStatus {
        STARTED,
        SUCCESS, 
        PARTIAL_SUCCESS,
        FAILED,
        CANCELLED
    }
    
    /**
     * Calcula o tempo de execução
     */
    public Long getExecutionTimeMs() {
        if (startedAt != null && finishedAt != null) {
            return finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
        return executionTimeMs;
    }
    
    /**
     * Calcula taxa de sucesso
     */
    public double getSuccessRate() {
        if (postsFound == null || postsFound == 0) return 0.0;
        int successful = (postsProcessed != null ? postsProcessed : 0);
        return (double) successful / postsFound * 100.0;
    }
    
    /**
     * Verifica se a execução foi bem-sucedida
     */
    public boolean isSuccessful() {
        return status == ExecutionStatus.SUCCESS || status == ExecutionStatus.PARTIAL_SUCCESS;
    }
    
    /**
     * Gera resumo da execução
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Execução %s: ", status.name()));
        
        if (postsFound != null) {
            summary.append(String.format("%d posts encontrados, ", postsFound));
        }
        
        if (postsProcessed != null) {
            summary.append(String.format("%d processados", postsProcessed));
        }
        
        if (postsNew != null && postsNew > 0) {
            summary.append(String.format(" (%d novos)", postsNew));
        }
        
        if (postsUpdated != null && postsUpdated > 0) {
            summary.append(String.format(" (%d atualizados)", postsUpdated));
        }
        
        if (postsSkipped != null && postsSkipped > 0) {
            summary.append(String.format(" (%d pulados)", postsSkipped));
        }
        
        if (captionsExtracted != null && captionsExtracted > 0) {
            summary.append(String.format(", %d captions extraídos", captionsExtracted));
        }
        
        if (executionTimeMs != null) {
            summary.append(String.format(" em %dms", executionTimeMs));
        }
        
        return summary.toString();
    }
}