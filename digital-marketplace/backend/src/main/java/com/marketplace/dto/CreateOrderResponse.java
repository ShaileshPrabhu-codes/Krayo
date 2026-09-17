package com.marketplace.dto;

// Everything the frontend checkout widget (Razorpay Checkout.js / Stripe Elements) needs.
// Note: no secret keys are ever included here - only the gateway's PUBLIC identifiers.
public record CreateOrderResponse(
        String orderNumber,
        String gateway,             // RAZORPAY | STRIPE
        String frontendToken,       // Razorpay order_id OR Stripe PaymentIntent client_secret - pass straight to the checkout SDK
        String publicKey,           // Razorpay key_id OR Stripe publishable key
        int amount,
        String currency
) {}
