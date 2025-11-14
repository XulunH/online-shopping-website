package com.xulunh.paymentservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class OrderGateway {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderGateway(RestTemplate restTemplate, @Value("${order.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public OrderDto get(UUID orderId) {
        return restTemplate.getForObject(baseUrl + "/api/v1/orders/" + orderId, OrderDto.class);
    }

    public static class OrderDto {
        public UUID id;
        public String status;
        public BigDecimal totalAmount;
    }
}
