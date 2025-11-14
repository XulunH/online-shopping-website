package com.xulunh.paymentservice.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
        String type, // PaymentSucceeded, PaymentFailed, RefundSucceeded
        UUID orderId,
        UUID paymentId,
        BigDecimal amount,
        String status,
        Instant occurredAt
) {}
