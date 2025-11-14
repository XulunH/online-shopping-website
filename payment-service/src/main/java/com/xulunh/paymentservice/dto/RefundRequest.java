package com.xulunh.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record RefundRequest(
        @DecimalMin("0.01") BigDecimal amount
) {}
