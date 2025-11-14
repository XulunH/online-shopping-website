package com.xulunh.accountservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountUpdateRequest(
        @NotBlank String username,
        AddressDto shippingAddress,
        AddressDto billingAddress
) {
}
