package com.xulunh.orderservice.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEvents {
    private final KafkaTemplate<String, Object> kafka;
    public OrderEvents(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

    public void publishCancelled(UUID orderId) {
        kafka.send("order.events", orderId.toString(), new OrderEvent("OrderCancelled", orderId));
    }

    public record OrderEvent(String type, UUID orderId) {}
}
