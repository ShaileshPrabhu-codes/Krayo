package com.marketplace.controller;

import com.marketplace.service.OrderService;
import com.marketplace.service.RazorpayPaymentService;
import com.marketplace.service.StripePaymentService;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * These endpoints are called ONLY by Razorpay/Stripe's own servers, never by the browser.
 * The frontend NEVER tells us "payment succeeded" - it only polls order status. This is
 * what makes the flow tamper-proof: an attacker controlling the browser/network cannot
 * forge a success state because they don't hold the webhook signing secret.
 */
@RestController
@RequestMapping("/api/v1/payments/webhook")
public class PaymentWebhookController {

    private final RazorpayPaymentService razorpayPaymentService;
    private final StripePaymentService stripePaymentService;
    private final OrderService orderService;

    public PaymentWebhookController(RazorpayPaymentService razorpayPaymentService,
                                     StripePaymentService stripePaymentService,
                                     OrderService orderService) {
        this.razorpayPaymentService = razorpayPaymentService;
        this.stripePaymentService = stripePaymentService;
        this.orderService = orderService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> razorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        if (!razorpayPaymentService.verifyWebhookSignature(rawPayload, signature)) {
            // Signature mismatch -> reject outright. Never trust an unverified payload.
            return ResponseEntity.status(400).body("invalid signature");
        }

        JSONObject payload = new JSONObject(rawPayload);
        String event = payload.optString("event");
        JSONObject paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String gatewayOrderId = paymentEntity.optString("order_id");
        String gatewayPaymentId = paymentEntity.optString("id");

        if ("payment.captured".equals(event)) {
            orderService.markOrderPaid(gatewayOrderId, gatewayPaymentId, rawPayload);
        } else if ("payment.failed".equals(event)) {
            orderService.markOrderFailed(gatewayOrderId);
        }

        return ResponseEntity.ok("ok");
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> stripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("Stripe-Signature") String signature) {

        if (!stripePaymentService.verifyWebhookSignature(rawPayload, signature)) {
            return ResponseEntity.status(400).body("invalid signature");
        }

        JSONObject payload = new JSONObject(rawPayload);
        String type = payload.optString("type");
        JSONObject intent = payload.getJSONObject("data").getJSONObject("object");
        // We stored PaymentIntent.id (not client_secret) as our gatewayOrderId - see StripePaymentService.
        String gatewayOrderId = intent.optString("id");
        String gatewayPaymentId = intent.optString("id");

        if ("payment_intent.succeeded".equals(type)) {
            orderService.markOrderPaid(gatewayOrderId, gatewayPaymentId, rawPayload);
        } else if ("payment_intent.payment_failed".equals(type)) {
            orderService.markOrderFailed(gatewayOrderId);
        }

        return ResponseEntity.ok("ok");
    }
}
