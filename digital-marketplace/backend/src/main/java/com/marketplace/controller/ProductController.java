package com.marketplace.controller;

import com.marketplace.dto.ProductResponse;
import com.marketplace.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // countryCode drives whether INR or USD price is shown; frontend detects this via
    // browser locale or IP geolocation and passes it as a query param.
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(defaultValue = "IN") String countryCode) {
        return ResponseEntity.ok(productService.listPublished(countryCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable Long id,
                                                @RequestParam(defaultValue = "IN") String countryCode) {
        return ResponseEntity.ok(productService.getOne(id, countryCode));
    }
}
