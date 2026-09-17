package com.marketplace.service;

import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.dto.CreateOrderResponse;
import com.marketplace.dto.OrderStatusResponse;
import com.marketplace.entity.*;
import com.marketplace.exception.ApiException;
import com.marketplace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final List<String> UNRESOLVED_STATUSES = List.of("CREATED", "PENDING");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomizationRepository customizationRepository;
    private final PaymentRepository paymentRepository;
    private final DownloadRepository downloadRepository;
    private final Map<String, PaymentGatewayService> gatewaysByName;
    private final CardRenderService cardRenderService;

    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         UserRepository userRepository,
                         CustomizationRepository customizationRepository,
                         PaymentRepository paymentRepository,
                         DownloadRepository downloadRepository,
                         RazorpayPaymentService razorpayPaymentService,
                         StripePaymentService stripePaymentService,
                         CardRenderService cardRenderService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.customizationRepository = customizationRepository;
        this.paymentRepository = paymentRepository;
        this.downloadRepository = downloadRepository;
        this.gatewaysByName = Map.of(
                "RAZORPAY", razorpayPaymentService,
                "STRIPE", stripePaymentService
        );
        this.cardRenderService = cardRenderService;
    }

    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "User not found"));
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ApiException(404, "Product not found"));

        if (!"PUBLISHED".equals(product.getStatus())) {
            throw new ApiException(400, "This product is not currently available");
        }

        // ---- Anti-duplicate-order guard ----
        // If the user already has an unresolved (CREATED/PENDING) order for this exact
        // product, reuse it instead of spinning up a second payment for the same purchase.
        Order order = orderRepository
                .findByUserIdAndProductIdAndStatusIn(userId, product.getId(), UNRESOLVED_STATUSES)
                .orElseGet(() -> createNewOrder(user, product));

        // Save/refresh the customization text tied to this order
        Customization customization = customizationRepository.findByOrderId(order.getId())
                .orElseGet(Customization::new);
        customization.setOrder(order);
        customization.setRecipientName(req.recipientName());
        customization.setCustomMessage(req.customMessage());
        customizationRepository.save(customization);

        // Only call the gateway to (re)issue a payable session if we don't already have one
        PaymentGatewayService gateway = gatewaysByName.get(order.getGateway());
        if (order.getGatewayOrderId() == null) {
            PaymentGatewayService.GatewayOrder gatewayOrder = gateway.createGatewayOrder(order);
            order.setGatewayOrderId(gatewayOrder.gatewayOrderId());
            order.setStatus("PENDING");
            orderRepository.save(order);
            return toResponse(order, gatewayOrder.publicKey(), gatewayOrder.frontendToken());
        }

        // Order already has a live gateway session (e.g. user refreshed checkout page) -
        // reuse the same session rather than creating a new one/charging twice.
        String frontendToken = gateway.getFrontendToken(order);
        return toResponse(order, publicKeyFor(gateway), frontendToken);
    }

    private Order createNewOrder(User user, Product product) {
        boolean isIndia = "IN".equalsIgnoreCase(user.getCountryCode());

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setIdempotencyKey(user.getId() + "-" + product.getId() + "-" + Instant.now().toEpochMilli() + "-" + RANDOM.nextInt(999999));
        order.setUser(user);
        order.setProduct(product);
        order.setCurrency(isIndia ? "INR" : "USD");
        order.setAmount(isIndia ? product.getPriceInrPaise() : product.getPriceUsdCents());
        order.setGateway(isIndia ? "RAZORPAY" : "STRIPE");
        order.setStatus("CREATED");
        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now());
        return "ORD-" + date + "-" + (100000 + RANDOM.nextInt(900000));
    }

    private String publicKeyFor(PaymentGatewayService gateway) {
        // For simplicity we re-issue via createGatewayOrder's returned key on first call;
        // in production, cache/store the public key in config lookup by gateway name.
        return gateway.getGatewayName().equals("RAZORPAY") ? System.getenv("RAZORPAY_KEY_ID")
                : System.getenv("STRIPE_PUBLISHABLE_KEY");
    }

    private CreateOrderResponse toResponse(Order order, String publicKey, String frontendToken) {
        return new CreateOrderResponse(
                order.getOrderNumber(),
                order.getGateway(),
                frontendToken,
                publicKey,
                order.getAmount(),
                order.getCurrency()
        );
    }

    /**
     * Called exclusively from the webhook handler AFTER signature verification.
     * This is the only path that can mark an order PAID - never trust a client-side callback.
     */
    @Transactional
    public void markOrderPaid(String gatewayOrderId, String gatewayPaymentId, String rawPayload) {
        // Idempotency: if we've already recorded this exact gateway payment id, do nothing.
        if (paymentRepository.existsByGatewayPaymentId(gatewayPaymentId)) {
            return;
        }

        Order order = orderRepository.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new ApiException(404, "Order not found for gateway id " + gatewayOrderId));

        if ("PAID".equals(order.getStatus())) {
            return; // already processed - webhook retries are expected and must be no-ops
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setStatus("SUCCESS");
        payment.setSignatureVerified(true);
        payment.setRawPayload(rawPayload);
        paymentRepository.save(payment);

        order.setStatus("PAID");
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        // Now (and only now) render the HD deliverable and register it for download.
        Customization customization = customizationRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ApiException(500, "Customization missing for paid order"));
        String renderedUrl = cardRenderService.renderHdCard(order.getProduct(), customization);
        customization.setRenderedAssetUrl(renderedUrl);
        customizationRepository.save(customization);

        Download download = new Download();
        download.setUser(order.getUser());
        download.setOrder(order);
        download.setFormat("pdf");
        download.setDownloadUrl(renderedUrl);
        downloadRepository.save(download);
    }

    public OrderStatusResponse getStatus(Long userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ApiException(404, "Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new ApiException(403, "Not your order");
        }

        String message = switch (order.getStatus()) {
            case "PAID" -> "Payment successful";
            case "FAILED" -> "Payment failed. Please try again.";
            case "PENDING" -> "Waiting for payment confirmation...";
            default -> "Order created";
        };
        return new OrderStatusResponse(order.getOrderNumber(), order.getStatus(), message);
    }

    @Transactional
    public void markOrderFailed(String gatewayOrderId) {
        orderRepository.findByGatewayOrderId(gatewayOrderId).ifPresent(order -> {
            if (!"PAID".equals(order.getStatus())) {
                order.setStatus("FAILED");
                orderRepository.save(order);
            }
        });
    }
}
