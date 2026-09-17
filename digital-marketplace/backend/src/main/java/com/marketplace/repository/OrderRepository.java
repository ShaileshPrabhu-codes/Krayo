package com.marketplace.repository;

import com.marketplace.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Optional<Order> findByGatewayOrderId(String gatewayOrderId);

    // Used to find & reuse an existing unpaid order instead of creating a duplicate
    Optional<Order> findByUserIdAndProductIdAndStatusIn(Long userId, Long productId, List<String> statuses);

    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}
