package com.xulunh.paymentservice.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentEvents {
    private final KafkaTemplate<String, Object> kafka;
    public PaymentEvents(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

    public void publishSucceeded(UUID orderId, UUID paymentId, BigDecimal amount) {
        kafka.send("payment.events", orderId.toString(),
                new PaymentEvent("PaymentSucceeded", orderId, paymentId, amount, Instant.now()));
    }

    public record PaymentEvent(String type, UUID orderId, UUID paymentId, BigDecimal amount, Instant occurredAt) {}
}
