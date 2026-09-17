package com.marketplace.service;

import com.marketplace.entity.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Handles UPI (and card/netbanking) payments for Indian customers via Razorpay.
 *
 * SECURITY NOTE: Your UPI ID is configured once inside the Razorpay merchant dashboard
 * (Settings -> Payment Methods -> UPI) and is NEVER read, stored, or referenced by this
 * application. Razorpay hosts the actual UPI collect/QR screen on its own domain. This
 * service only ever talks to Razorpay's API using a server-side secret key that is never
 * sent to the browser - the frontend only ever receives the public key_id + order_id below,
 * which cannot be used to redirect funds anywhere; they only open Razorpay's own checkout.
 */
@Service
public class RazorpayPaymentService implements PaymentGatewayService {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayPaymentService(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpay.webhook-secret}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String getGatewayName() {
        return "RAZORPAY";
    }

    @Override
    public GatewayOrder createGatewayOrder(Order order) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", order.getAmount());       // paise
            orderRequest.put("currency", order.getCurrency());   // "INR"
            orderRequest.put("receipt", order.getOrderNumber());
            // Ties this gateway order permanently to our own order row - used to reconcile
            // the webhook back to the correct order without trusting any client-supplied id.
            JSONObject notes = new JSONObject();
            notes.put("internal_order_number", order.getOrderNumber());
            orderRequest.put("notes", notes);

            com.razorpay.Order rpOrder = client.orders.create(orderRequest);
            String rpOrderId = rpOrder.get("id");
            return new GatewayOrder(rpOrderId, keyId, rpOrderId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        try {
            return Utils.verifyWebhookSignature(rawPayload, signatureHeader, webhookSecret);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFrontendToken(Order order) {
        // Razorpay's checkout widget just needs the order id itself again.
        return order.getGatewayOrderId();
    }
}
