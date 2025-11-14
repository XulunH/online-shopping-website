package com.xulunh.orderservice.events;

import com.xulunh.orderservice.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentEventsListener {
    private final OrderService orders;

    public PaymentEventsListener(OrderService orders) { this.orders = orders; }

    @KafkaListener(topics = "payment.events", groupId = "order-service")
    public void onPaymentEvent(ConsumerRecord<String, PaymentEvent> record) {
        var evt = record.value();
        if (evt == null || !"PaymentSucceeded".equals(evt.type())) return;
        try {
            orders.complete(evt.orderId());
        } catch (Exception ignored) {
            // idempotent or invalid state → skip
        }
    }

    public record PaymentEvent(String type, UUID orderId, UUID paymentId) {}
}
