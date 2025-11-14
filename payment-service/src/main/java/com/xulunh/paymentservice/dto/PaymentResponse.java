package com.xulunh.paymentservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String status,
        BigDecimal amount,
        String accountEmail,
        Instant createdAt,
        Instant updatedAt
) {}
