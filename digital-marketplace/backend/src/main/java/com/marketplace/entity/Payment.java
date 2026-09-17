package com.marketplace.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Gateway's own payment/charge/session id - unique constraint gives us
    // free idempotency: replaying the same webhook event twice is a no-op.
    @Column(name = "gateway_payment_id", nullable = false, unique = true)
    private String gatewayPaymentId;

    @Column(nullable = false)
    private String status; // SUCCESS | FAILED

    @Column(name = "signature_verified", nullable = false)
    private boolean signatureVerified = false;

    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
