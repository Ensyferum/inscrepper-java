package com.ensyferum.inscrepper.web;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.ContentType;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostsController {

    private final ContentRepository contentRepository;
    private final ProfileRepository profileRepository;

    /**
     * Lista todos os posts com filtros e paginação
     */
    @GetMapping
    public String listPosts(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "12") int size,
                           @RequestParam(required = false) String profile,
                           @RequestParam(required = false) String type,
                           @RequestParam(required = false) String sort) {
        
        log.info("Listando posts - page: {}, size: {}, profile: {}, type: {}, sort: {}", 
                 page, size, profile, type, sort);
        
        // Definir ordenação
        Sort sorting = getSorting(sort);
        Pageable pageable = PageRequest.of(page, size, sorting);
        
        // Buscar posts com filtros
        Page<Content> contentPage;
        if (profile != null && !profile.isEmpty()) {
            Optional<Profile> profileObj = profileRepository.findByUsername(profile);
            if (profileObj.isPresent()) {
                if (type != null && !type.isEmpty()) {
                    ContentType contentType = ContentType.valueOf(type.toUpperCase());
                    contentPage = contentRepository.findByProfileAndType(profileObj.get(), contentType, pageable);
                } else {
                    contentPage = contentRepository.findByProfile(profileObj.get(), pageable);
                }
            } else {
                contentPage = Page.empty(pageable);
            }
        } else if (type != null && !type.isEmpty()) {
            ContentType contentType = ContentType.valueOf(type.toUpperCase());
            contentPage = contentRepository.findByType(contentType, pageable);
        } else {
            contentPage = contentRepository.findAll(pageable);
        }
        
        // Calcular estatísticas
        Map<String, Object> stats = calculateStats(contentPage.getContent());
        
        // Buscar todos os perfis para o filtro
        List<Profile> allProfiles = profileRepository.findAll(Sort.by("username"));
        
        model.addAttribute("contentPage", contentPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contentPage.getTotalPages());
        model.addAttribute("totalElements", contentPage.getTotalElements());
        model.addAttribute("profiles", allProfiles);
        model.addAttribute("stats", stats);
        
        // Manter filtros selecionados
        model.addAttribute("selectedProfile", profile);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedSort", sort);
        
        return "posts/list";
    }

    /**
     * Visualizar detalhes de um post específico
     */
    @GetMapping("/{id}")
    public String viewPost(@PathVariable UUID id, Model model) {
        log.info("Visualizando post ID: {}", id);
        
        Optional<Content> content = contentRepository.findById(id);
        if (content.isEmpty()) {
            log.warn("Post não encontrado: {}", id);
            return "redirect:/posts";
        }
        
        Content post = content.get();
        
        // Buscar posts relacionados do mesmo perfil
        List<Content> relatedPosts = contentRepository.findByProfileOrderByCollectedAtDesc(post.getProfile())
                .stream()
                .filter(c -> !c.getId().equals(id))
                .limit(6)
                .collect(Collectors.toList());
        
        // Calcular taxa de engajamento
        double engagementRate = calculateEngagementRate(post);
        
        model.addAttribute("post", post);
        model.addAttribute("relatedPosts", relatedPosts);
        model.addAttribute("engagementRate", engagementRate);
        
        return "posts/view";
    }

    /**
     * Comparar múltiplos posts
     */
    @GetMapping("/compare")
    public String comparePosts(@RequestParam List<UUID> ids, Model model) {
        log.info("Comparando posts: {}", ids);
        
        if (ids == null || ids.isEmpty()) {
            return "redirect:/posts";
        }
        
        List<Content> posts = contentRepository.findAllById(ids);
        
        if (posts.isEmpty()) {
            return "redirect:/posts";
        }
        
        // Calcular estatísticas comparativas
        Map<String, Object> comparisonStats = calculateComparisonStats(posts);
        
        model.addAttribute("posts", posts);
        model.addAttribute("comparisonStats", comparisonStats);
        
        return "posts/compare";
    }

    /**
     * Deletar post
     */
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable UUID id) {
        log.info("Deletando post ID: {}", id);
        
        Optional<Content> content = contentRepository.findById(id);
        if (content.isPresent()) {
            contentRepository.delete(content.get());
            log.info("Post deletado com sucesso: {}", id);
        }
        
        return "redirect:/posts";
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Sort getSorting(String sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.by("collectedAt").descending();
        }
        
        return switch (sort) {
            case "likes" -> Sort.by("likesCount").descending();
            case "comments" -> Sort.by("commentsCount").descending();
            case "views" -> Sort.by("viewsCount").descending();
            case "oldest" -> Sort.by("collectedAt").ascending();
            default -> Sort.by("collectedAt").descending();
        };
    }

    private Map<String, Object> calculateStats(List<Content> contents) {
        Map<String, Object> stats = new HashMap<>();
        
        if (contents.isEmpty()) {
            stats.put("totalPosts", 0);
            stats.put("totalLikes", 0L);
            stats.put("totalComments", 0L);
            stats.put("totalViews", 0L);
            stats.put("avgLikes", 0.0);
            stats.put("avgComments", 0.0);
            stats.put("avgViews", 0.0);
            return stats;
        }
        
        long totalLikes = contents.stream()
                .mapToLong(c -> c.getLikesCount() != null ? c.getLikesCount() : 0)
                .sum();
        
        long totalComments = contents.stream()
                .mapToLong(c -> c.getCommentsCount() != null ? c.getCommentsCount() : 0)
                .sum();
        
        long totalViews = contents.stream()
                .mapToLong(c -> c.getViewsCount() != null ? c.getViewsCount() : 0)
                .sum();
        
        stats.put("totalPosts", contents.size());
        stats.put("totalLikes", totalLikes);
        stats.put("totalComments", totalComments);
        stats.put("totalViews", totalViews);
        stats.put("avgLikes", totalLikes / (double) contents.size());
        stats.put("avgComments", totalComments / (double) contents.size());
        stats.put("avgViews", totalViews / (double) contents.size());
        
        return stats;
    }

    private double calculateEngagementRate(Content content) {
        if (content.getViewsCount() == null || content.getViewsCount() == 0) {
            return 0.0;
        }
        
        long likes = content.getLikesCount() != null ? content.getLikesCount() : 0;
        return (likes * 100.0) / content.getViewsCount();
    }

    private Map<String, Object> calculateComparisonStats(List<Content> posts) {
        Map<String, Object> stats = new HashMap<>();
        
        // Post com mais likes
        Content mostLiked = posts.stream()
                .max(Comparator.comparing(c -> c.getLikesCount() != null ? c.getLikesCount() : 0))
                .orElse(null);
        
        // Post com mais comentários
        Content mostCommented = posts.stream()
                .max(Comparator.comparing(c -> c.getCommentsCount() != null ? c.getCommentsCount() : 0))
                .orElse(null);
        
        // Post com mais views
        Content mostViewed = posts.stream()
                .max(Comparator.comparing(c -> c.getViewsCount() != null ? c.getViewsCount() : 0))
                .orElse(null);
        
        // Post com melhor engajamento
        Content bestEngagement = posts.stream()
                .max(Comparator.comparing(this::calculateEngagementRate))
                .orElse(null);
        
        stats.put("mostLiked", mostLiked);
        stats.put("mostCommented", mostCommented);
        stats.put("mostViewed", mostViewed);
        stats.put("bestEngagement", bestEngagement);
        
        // Totais
        long totalLikes = posts.stream()
                .mapToLong(c -> c.getLikesCount() != null ? c.getLikesCount() : 0)
                .sum();
        
        long totalComments = posts.stream()
                .mapToLong(c -> c.getCommentsCount() != null ? c.getCommentsCount() : 0)
                .sum();
        
        long totalViews = posts.stream()
                .mapToLong(c -> c.getViewsCount() != null ? c.getViewsCount() : 0)
                .sum();
        
        stats.put("totalLikes", totalLikes);
        stats.put("totalComments", totalComments);
        stats.put("totalViews", totalViews);
        
        return stats;
    }
}
