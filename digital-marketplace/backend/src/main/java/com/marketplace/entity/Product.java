package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String title;

    private String description;

    private String theme;

    @Column(nullable = false)
    private String version = "v1.0";

    @Column(name = "price_inr_paise", nullable = false)
    private Integer priceInrPaise;

    @Column(name = "price_usd_cents", nullable = false)
    private Integer priceUsdCents;

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT | PUBLISHED | RETIRED

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAsset> assets = new ArrayList<>();

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
