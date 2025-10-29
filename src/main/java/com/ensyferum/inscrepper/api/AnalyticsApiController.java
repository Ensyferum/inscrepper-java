package com.ensyferum.inscrepper.api;

import com.ensyferum.inscrepper.model.Content;
import com.ensyferum.inscrepper.model.Profile;
import com.ensyferum.inscrepper.repository.ContentRepository;
import com.ensyferum.inscrepper.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsApiController {

    private final ProfileRepository profileRepository;
    private final ContentRepository contentRepository;

    @GetMapping("/profile/{username}/stats")
    public ResponseEntity<?> getProfileStats(@PathVariable String username) {
        Optional<Profile> profileOpt = profileRepository.findByUsername(username);
        
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Profile profile = profileOpt.get();
        List<Content> contents = contentRepository.findByProfileOrderByCollectedAtDesc(profile);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("username", username);
        stats.put("totalPosts", contents.size());
        
        // Métricas totais
        long totalLikes = contents.stream()
                .mapToLong(c -> c.getLikesCount() != null ? c.getLikesCount() : 0)
                .sum();
        
        long totalComments = contents.stream()
                .mapToLong(c -> c.getCommentsCount() != null ? c.getCommentsCount() : 0)
                .sum();
        
        long totalViews = contents.stream()
                .mapToLong(c -> c.getViewsCount() != null ? c.getViewsCount() : 0)
                .sum();
        
        stats.put("totalLikes", totalLikes);
        stats.put("totalComments", totalComments);
        stats.put("totalViews", totalViews);
        
        // Médias
        stats.put("avgLikes", contents.isEmpty() ? 0 : (double) totalLikes / contents.size());
        stats.put("avgComments", contents.isEmpty() ? 0 : (double) totalComments / contents.size());
        stats.put("avgViews", contents.isEmpty() ? 0 : (double) totalViews / contents.size());
        
        // Top posts
        List<Map<String, Object>> topPosts = contents.stream()
                .sorted((a, b) -> {
                    long likesA = a.getLikesCount() != null ? a.getLikesCount() : 0;
                    long likesB = b.getLikesCount() != null ? b.getLikesCount() : 0;
                    return Long.compare(likesB, likesA);
                })
                .limit(5)
                .map(this::contentToMap)
                .collect(Collectors.toList());
        
        stats.put("topPosts", topPosts);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/profile/{username}/posts")
    public ResponseEntity<?> getProfilePosts(@PathVariable String username) {
        Optional<Profile> profileOpt = profileRepository.findByUsername(username);
        
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Profile profile = profileOpt.get();
        List<Content> contents = contentRepository.findByProfileOrderByCollectedAtDesc(profile);
        
        List<Map<String, Object>> posts = contents.stream()
                .map(this::contentToMap)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/profile/{username}/chart-data")
    public ResponseEntity<?> getChartData(@PathVariable String username) {
        Optional<Profile> profileOpt = profileRepository.findByUsername(username);
        
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Profile profile = profileOpt.get();
        List<Content> contents = contentRepository.findByProfileOrderByCollectedAtDesc(profile);
        
        Map<String, Object> chartData = new HashMap<>();
        
        // Dados para gráfico de barras (por post)
        List<String> labels = new ArrayList<>();
        List<Long> likesData = new ArrayList<>();
        List<Long> commentsData = new ArrayList<>();
        List<Long> viewsData = new ArrayList<>();
        
        // Reverter para ordem cronológica nos gráficos
        List<Content> reversedContents = new ArrayList<>(contents);
        Collections.reverse(reversedContents);
        
        for (Content content : reversedContents) {
            labels.add(content.getExternalId());
            likesData.add(content.getLikesCount() != null ? content.getLikesCount() : 0);
            commentsData.add(content.getCommentsCount() != null ? content.getCommentsCount() : 0);
            viewsData.add(content.getViewsCount() != null ? content.getViewsCount() : 0);
        }
        
        chartData.put("labels", labels);
        chartData.put("likes", likesData);
        chartData.put("comments", commentsData);
        chartData.put("views", viewsData);
        
        return ResponseEntity.ok(chartData);
    }

    private Map<String, Object> contentToMap(Content content) {
        Map<String, Object> map = new HashMap<>();
        map.put("externalId", content.getExternalId());
        map.put("type", content.getType());
        map.put("url", content.getUrl());
        map.put("caption", content.getCaption());
        map.put("likesCount", content.getLikesCount() != null ? content.getLikesCount() : 0);
        map.put("commentsCount", content.getCommentsCount() != null ? content.getCommentsCount() : 0);
        map.put("viewsCount", content.getViewsCount() != null ? content.getViewsCount() : 0);
        map.put("collectedAt", content.getCollectedAt());
        
        // Calcular taxa de engajamento
        long likes = content.getLikesCount() != null ? content.getLikesCount() : 0;
        long views = content.getViewsCount() != null ? content.getViewsCount() : 0;
        double engagementRate = views > 0 ? (likes * 100.0 / views) : 0;
        map.put("engagementRate", engagementRate);
        
        return map;
    }
}
