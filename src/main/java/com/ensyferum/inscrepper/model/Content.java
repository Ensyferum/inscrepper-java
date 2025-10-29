package com.ensyferum.inscrepper.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contents", indexes = {
        @Index(name = "idx_contents_profile", columnList = "profile_id"),
        @Index(name = "idx_contents_externalId", columnList = "externalId")
})
public class Content {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, foreignKey = @ForeignKey(name = "fk_contents_profile"))
    private Profile profile;

    @Column(nullable = false, unique = true, length = 100)
    private String externalId; // e.g., Instagram shortcode

    @Column(length = 500)
    private String url;

    @Column(length = 500)
    private String mediaUrl;

    @Column(columnDefinition = "text")
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(nullable = false)
    private Instant collectedAt;

    private Instant publishedAt;

    @Column(length = 500)
    private String thumbnailPath;

    @Column(length = 500)
    private String mediaPath;

    @Lob
    @Column(name = "image_blob", columnDefinition = "LONGBLOB")
    private byte[] imageBlob;

    @Column(length = 100)
    private String imageMimeType;

    // Métricas de engajamento
    private Long likesCount;
    
    private Long commentsCount;
    
    private Long viewsCount;

    @PrePersist
    public void prePersist() {
        if (collectedAt == null) {
            collectedAt = Instant.now();
        }
        if (type == null) {
            type = ContentType.UNKNOWN;
        }
    }
}
