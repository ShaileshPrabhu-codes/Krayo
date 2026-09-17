package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "product_assets")
@Data
public class ProductAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_format", nullable = false)
    private String fileFormat;

    @Column(name = "is_template")
    private boolean isTemplate = true;

    // Stored as JSONB; a lightweight map is enough - {x,y,fontSize,color,fontFamily}
    @Column(name = "name_zone", columnDefinition = "jsonb")
    private String nameZone;

    @Column(name = "message_zone", columnDefinition = "jsonb")
    private String messageZone;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
