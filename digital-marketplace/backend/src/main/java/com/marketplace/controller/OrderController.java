package com.marketplace.controller;

import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.dto.CreateOrderResponse;
import com.marketplace.dto.OrderStatusResponse;
import com.marketplace.security.JwtAuthFilter.AuthenticatedUser;
import com.marketplace.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Creates (or safely reuses) an order + gateway session. Requires login.
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orderService.createOrder(user.id(), req));
    }

    // Frontend polls this after checkout instead of trusting the gateway's client-side
    // redirect/callback - the real status only ever changes via a verified webhook.
    @GetMapping("/{orderNumber}/status")
    public ResponseEntity<OrderStatusResponse> getStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getStatus(user.id(), orderNumber));
    }
}
