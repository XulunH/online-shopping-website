package com.xulunh.accountservice.dto;

public record AccountResponse(
        Long id,
        String email,
        String username,
        AddressDto shippingAddress,
        AddressDto billingAddress
) {}
