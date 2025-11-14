package com.xulunh.orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderUpdateRequest(
        @NotEmpty List<OrderItemRequest> items
) {}
