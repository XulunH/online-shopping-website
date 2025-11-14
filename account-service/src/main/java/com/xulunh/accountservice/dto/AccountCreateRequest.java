package com.xulunh.accountservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountCreateRequest(
        @Email @NotBlank String email,
        @NotBlank String username,
        @NotBlank String password,
        AddressDto shippingAddress,
        AddressDto billingAddress
) {
}
