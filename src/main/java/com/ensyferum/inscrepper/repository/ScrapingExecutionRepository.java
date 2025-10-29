package com.ensyferum.inscrepper.repository;

import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.model.ScrapingExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScrapingExecutionRepository extends JpaRepository<ScrapingExecution, UUID> {
    
    /**
     * Busca a última execução de um perfil
     */
    Optional<ScrapingExecution> findFirstByProfileOrderByStartedAtDesc(Profile profile);
    
    /**
     * Busca execuções de um perfil ordenadas por data
     */
    List<ScrapingExecution> findByProfileOrderByStartedAtDesc(Profile profile);
    
    /**
     * Busca execuções bem-sucedidas de um perfil
     */
    @Query("SELECT se FROM ScrapingExecution se WHERE se.profile = :profile " +
           "AND se.status IN ('SUCCESS', 'PARTIAL_SUCCESS') ORDER BY se.startedAt DESC")
    List<ScrapingExecution> findSuccessfulExecutionsByProfile(@Param("profile") Profile profile);
    
    /**
     * Busca execuções recentes (últimas 24 horas)
     */
    @Query("SELECT se FROM ScrapingExecution se WHERE se.startedAt >= :since ORDER BY se.startedAt DESC")
    List<ScrapingExecution> findRecentExecutions(@Param("since") Instant since);
    
    /**
     * Estatísticas de execução por perfil
     */
    @Query("SELECT " +
           "COUNT(se) as totalExecutions, " +
           "SUM(CASE WHEN se.status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount, " +
           "SUM(CASE WHEN se.status = 'FAILED' THEN 1 ELSE 0 END) as failureCount, " +
           "AVG(se.executionTimeMs) as avgExecutionTime, " +
           "SUM(se.postsProcessed) as totalPostsProcessed " +
           "FROM ScrapingExecution se WHERE se.profile = :profile")
    Object[] getExecutionStatsByProfile(@Param("profile") Profile profile);
    
    /**
     * Verifica se existe execução bem-sucedida recente
     */
    @Query("SELECT COUNT(se) > 0 FROM ScrapingExecution se WHERE se.profile = :profile " +
           "AND se.status IN ('SUCCESS', 'PARTIAL_SUCCESS') " +
           "AND se.startedAt >= :since")
    boolean hasRecentSuccessfulExecution(@Param("profile") Profile profile, @Param("since") Instant since);
    
    /**
     * Busca execuções que falharam
     */
    List<ScrapingExecution> findByStatusOrderByStartedAtDesc(ScrapingExecution.ExecutionStatus status);
}