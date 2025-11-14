package com.xulunh.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String accountEmail,
        String status,
        BigDecimal totalAmount,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record Item(String itemId, String upc, String name, BigDecimal unitPrice, int quantity) {}
}
