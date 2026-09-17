package com.marketplace.service;

import com.marketplace.entity.Order;

/**
 * Every payment gateway (Razorpay for India/UPI, Stripe for international) implements this.
 * IMPORTANT: implementations must never expose merchant secrets (UPI VPA, API secret keys,
 * webhook secrets) to the frontend. Only the values returned in GatewayOrder (a public
 * order/session id + public key) are safe to send to the browser.
 */
public interface PaymentGatewayService {

    String getGatewayName(); // "RAZORPAY" | "STRIPE"

    /**
     * Creates an order/payment-intent on the gateway's side using server-held secret credentials.
     * The merchant's UPI ID / bank details live only inside the gateway's own dashboard - this
     * code never reads, stores, or transmits them.
     */
    GatewayOrder createGatewayOrder(Order order);

    /**
     * Verifies a webhook/callback payload using HMAC signature verification against the
     * gateway's webhook secret. This is what prevents a forged "payment successful" request
     * from an attacker who doesn't know the secret.
     */
    boolean verifyWebhookSignature(String rawPayload, String signatureHeader);

    /**
     * Re-fetches the token the frontend checkout widget needs, given an order that already
     * has a gatewayOrderId (e.g. the user reopened the checkout page). For Razorpay this is
     * just the order id; for Stripe it requires re-retrieving the PaymentIntent's client_secret.
     */
    String getFrontendToken(Order order);

    /**
     * gatewayOrderId: the stable id we store in our own `orders` table and use to match
     *                 incoming webhooks back to the right order (Razorpay order id /
     *                 Stripe PaymentIntent id).
     * publicKey:      safe to expose to the browser (Razorpay key_id / Stripe publishable key).
     * frontendToken:  what the checkout widget actually needs to collect payment
     *                 (same as gatewayOrderId for Razorpay; Stripe's client_secret for Stripe -
     *                 Stripe webhooks don't reliably echo client_secret back, so we must not
     *                 rely on it for server-side matching).
     */
    record GatewayOrder(String gatewayOrderId, String publicKey, String frontendToken) {}
}
