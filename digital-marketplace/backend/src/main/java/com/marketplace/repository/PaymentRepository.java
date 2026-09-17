package com.marketplace.repository;

import com.marketplace.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    boolean existsByGatewayPaymentId(String gatewayPaymentId);
}
