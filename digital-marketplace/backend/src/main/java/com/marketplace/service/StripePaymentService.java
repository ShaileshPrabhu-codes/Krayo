package com.marketplace.service;

import com.marketplace.entity.Order;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles card payments in USD for international customers via Stripe.
 * International customers can't pay via UPI (India-only rail), so Stripe's
 * hosted card element / PaymentIntent flow is used instead - the customer's
 * card details never touch our backend (Stripe.js tokenizes them client-side).
 */
@Service
public class StripePaymentService implements PaymentGatewayService {

    private final String secretKey;
    private final String publishableKey;
    private final String webhookSecret;

    public StripePaymentService(
            @Value("${stripe.api-key}") String secretKey,
            @Value("${stripe.publishable-key}") String publishableKey,
            @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.secretKey = secretKey;
        this.publishableKey = publishableKey;
        this.webhookSecret = webhookSecret;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    public String getGatewayName() {
        return "STRIPE";
    }

    @Override
    public GatewayOrder createGatewayOrder(Order order) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("internal_order_number", order.getOrderNumber());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(Long.valueOf(order.getAmount())) // cents
                    .setCurrency(order.getCurrency().toLowerCase()) // "usd"
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            // We store intent.getId() as our internal gatewayOrderId (used to match webhooks).
            // clientSecret is what we hand to the browser - it only authorizes completing
            // THIS SPECIFIC payment intent with Stripe's own SDK, nothing else.
            return new GatewayOrder(intent.getId(), publishableKey, intent.getClientSecret());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        try {
            Webhook.constructEvent(rawPayload, signatureHeader, webhookSecret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFrontendToken(Order order) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(order.getGatewayOrderId());
            return intent.getClientSecret();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }
}
