package com.marketplace.controller;

import com.marketplace.entity.Product;
import com.marketplace.security.JwtAuthFilter.AuthenticatedUser;
import com.marketplace.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    // Protected by SecurityConfig: only ROLE_ADMIN can reach /api/v1/admin/**
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Product> upload(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String theme,
            @RequestParam int priceInrPaise,
            @RequestParam int priceUsdCents,
            @RequestParam MultipartFile file) {

        Product product = productService.uploadNewProduct(
                title, description, theme, priceInrPaise, priceUsdCents, admin.id(), file);
        return ResponseEntity.ok(product);
    }
}
