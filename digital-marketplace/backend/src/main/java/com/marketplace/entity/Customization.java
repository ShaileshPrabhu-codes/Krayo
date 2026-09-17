package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "customizations")
@Data
public class Customization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "custom_message", nullable = false, columnDefinition = "TEXT")
    private String customMessage;

    // Only populated once payment succeeds - this is the HD deliverable
    @Column(name = "rendered_asset_url")
    private String renderedAssetUrl;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
