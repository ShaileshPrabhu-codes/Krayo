package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String currency; // INR | USD

    @Column(nullable = false)
    private Integer amount; // minor units

    @Column(nullable = false)
    private String gateway; // RAZORPAY | STRIPE

    @Column(name = "gateway_order_id")
    private String gatewayOrderId;

    @Column(nullable = false)
    private String status = "CREATED"; // CREATED|PENDING|PAID|FAILED|CANCELLED

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
