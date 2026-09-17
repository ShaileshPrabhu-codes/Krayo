package com.marketplace.dto;

public record ProductResponse(
        Long id,
        String sku,
        String title,
        String description,
        String theme,
        String version,
        int price,           // in the currency appropriate to the requesting user
        String currency,     // INR | USD
        String previewImageUrl
) {}
