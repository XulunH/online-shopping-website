package com.xulunh.accountservice.dto;

public record AddressDto(
    String line1,
    String line2,
    String city,
    String state,
    String zip,
    String country

){}
