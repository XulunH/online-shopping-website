package com.xulunh.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderItemRequest(
        @NotBlank String upc, @Min(1) int quantity
) {
}
